package me.sanenuyan.homeGUI.data;

import org.bukkit.Bukkit;
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
    private final Map<UUID, List<Home>> playerHomes;

    public HomeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
        this.playerHomes = new ConcurrentHashMap<>();
        loadHomes();
    }

    public void loadHomes() {
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

                    // Thử đọc world UUID trước
                    UUID worldUUID = null;
                    String worldUUIDStr = homesConfig.getString(path + ".world_uuid");
                    if (worldUUIDStr != null) {
                        try {
                            worldUUID = UUID.fromString(worldUUIDStr);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid World UUID for home '" + homeName + "' of player " + playerUUIDStr + ": " + worldUUIDStr);
                        }
                    }

                    String worldName = homesConfig.getString(path + ".world");
                    double x = homesConfig.getDouble(path + ".x");
                    double y = homesConfig.getDouble(path + ".y");
                    double z = homesConfig.getDouble(path + ".z");
                    float yaw = (float) homesConfig.getDouble(path + ".yaw");
                    float pitch = (float) homesConfig.getDouble(path + ".pitch");
                    long worldSeed = homesConfig.getLong(path + ".world_seed", 0L);
                    long creationDate = homesConfig.getLong(path + ".creationDate", System.currentTimeMillis()); // Đọc creationDate, mặc định là thời gian hiện tại nếu không tồn tại

                    // Kiểm tra và sử dụng constructor phù hợp
                    if (worldUUID != null && worldName != null) {
                        homes.add(new Home(playerUUID, homeName, worldUUID, worldName, worldSeed, x, y, z, yaw, pitch, creationDate));
                    } else if (worldName != null) { // Fallback cho homes cũ không có UUID
                        homes.add(new Home(playerUUID, homeName, worldName, x, y, z, yaw, pitch, worldSeed, creationDate));
                    } else {
                        plugin.getLogger().warning("Home '" + homeName + "' for player " + playerUUIDStr + " has missing world data. Skipping.");
                    }
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
                if (home.getWorldUUID() != null) { // Lưu world UUID
                    homesConfig.set(path + ".world_uuid", home.getWorldUUID().toString());
                }
                homesConfig.set(path + ".world", home.getWorldName());
                homesConfig.set(path + ".x", home.getX());
                homesConfig.set(path + ".y", home.getY());
                homesConfig.set(path + ".z", home.getZ());
                homesConfig.set(path + ".yaw", home.getYaw());
                homesConfig.set(path + ".pitch", home.getPitch());
                homesConfig.set(path + ".world_seed", home.getWorldSeed());
                homesConfig.set(path + ".creationDate", home.getCreationDate()); // Lưu creationDate
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
        List<Home> homes = playerHomes.getOrDefault(home.getPlayerUUID(), new ArrayList<>());
        homes.removeIf(h -> h.getName().equalsIgnoreCase(home.getName()));
        homes.add(home);
        playerHomes.put(home.getPlayerUUID(), homes);
        saveHomes();
        return true;
    }

    public boolean deleteHome(UUID playerUUID, String homeName) {
        List<Home> homes = playerHomes.getOrDefault(playerUUID, new ArrayList<>());
        boolean removed = homes.removeIf(home -> home.getName().equalsIgnoreCase(homeName));
        if (removed) {
            playerHomes.put(playerUUID, homes);
            saveHomes();
        }
        return removed;
    }

    public boolean homeExists(UUID playerUUID, String homeName) {
        return getPlayerHomes(playerUUID).stream()
                .anyMatch(home -> home.getName().equalsIgnoreCase(homeName));
    }
}