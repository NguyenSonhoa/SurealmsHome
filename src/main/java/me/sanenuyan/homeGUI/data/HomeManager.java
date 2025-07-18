package me.sanenuyan.homeGUI.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HomeManager {
    private final JavaPlugin plugin;
    private final File homesFile;
    private YamlConfiguration homesConfig;
    private final Map<UUID, List<Home>> playerHomes; // Map to store homes per player

    public HomeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
        this.playerHomes = new ConcurrentHashMap<>();
        loadHomes();
    }

    private void loadHomes() {
        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml: " + e.getMessage());
            }
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);

        playerHomes.clear();
        for (String playerUUIDStr : homesConfig.getKeys(false)) {
            UUID playerUUID = UUID.fromString(playerUUIDStr);
            List<Home> homes = new ArrayList<>();
            if (homesConfig.isConfigurationSection(playerUUIDStr)) {
                for (String homeName : homesConfig.getConfigurationSection(playerUUIDStr).getKeys(false)) {
                    String path = playerUUIDStr + "." + homeName;
                    String worldName = homesConfig.getString(path + ".world");
                    double x = homesConfig.getDouble(path + ".x");
                    double y = homesConfig.getDouble(path + ".y");
                    double z = homesConfig.getDouble(path + ".z");
                    float yaw = (float) homesConfig.getDouble(path + ".yaw");
                    float pitch = (float) homesConfig.getDouble(path + ".pitch");
                    long worldSeed = homesConfig.getLong(path + ".world_seed", 0L); // Default to 0 for old homes

                    homes.add(new Home(playerUUID, homeName, worldName, x, y, z, yaw, pitch, worldSeed));
                }
            }
            playerHomes.put(playerUUID, homes);
        }
        plugin.getLogger().info("Loaded " + playerHomes.values().stream().mapToInt(List::size).sum() + " homes.");
    }

    public void saveHomes() {
        homesConfig = new YamlConfiguration(); // Clear existing config to ensure clean save

        playerHomes.forEach((uuid, homes) -> {
            homes.forEach(home -> {
                String path = uuid.toString() + "." + home.getName();
                homesConfig.set(path + ".world", home.getWorldName());
                homesConfig.set(path + ".x", home.getX());
                homesConfig.set(path + ".y", home.getY());
                homesConfig.set(path + ".z", home.getZ());
                homesConfig.set(path + ".yaw", home.getYaw());
                homesConfig.set(path + ".pitch", home.getPitch());
                homesConfig.set(path + ".world_seed", home.getWorldSeed());
            });
        });

        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }

    public List<Home> getPlayerHomes(UUID playerUUID) {
        return playerHomes.getOrDefault(playerUUID, new ArrayList<>());
    }

    public Home getHome(UUID playerUUID, String homeName) {
        return getPlayerHomes(playerUUID).stream()
                .filter(home -> home.getName().equalsIgnoreCase(homeName))
                .findFirst()
                .orElse(null);
    }

    public boolean addHome(Home home) {
        List<Home> homes = playerHomes.getOrDefault(home.getOwnerUUID(), new ArrayList<>());
        // Check if home with this name already exists (case-insensitive)
        if (homes.stream().anyMatch(h -> h.getName().equalsIgnoreCase(home.getName()))) {
            // Update existing home
            homes = homes.stream()
                    .filter(h -> !h.getName().equalsIgnoreCase(home.getName()))
                    .collect(Collectors.toList());
            homes.add(home);
            playerHomes.put(home.getOwnerUUID(), homes);
            saveHomes();
            return true; // Indicate that it was updated or added
        } else {
            // Add new home
            homes.add(home);
            playerHomes.put(home.getOwnerUUID(), homes);
            saveHomes();
            return true;
        }
    }


    public boolean removeHome(UUID playerUUID, String homeName) {
        List<Home> homes = playerHomes.getOrDefault(playerUUID, new ArrayList<>());
        boolean removed = homes.removeIf(home -> home.getName().equalsIgnoreCase(homeName));
        if (removed) {
            playerHomes.put(playerUUID, homes);
            saveHomes();
        }
        return removed;
    }
}