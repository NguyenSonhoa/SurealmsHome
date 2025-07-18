package me.sanenuyan.homeGUI.data;

import me.sanenuyan.homeGUI.HomeGUI;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HomeManager {
    private final HomeGUI plugin;
    private final File homesFile;
    private final Map<UUID, List<Home>> playerHomes;

    public HomeManager(HomeGUI plugin) {
        this.plugin = plugin;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
        this.playerHomes = new ConcurrentHashMap<>();
        ConfigurationSerialization.registerClass(Home.class, "Home");
        loadHomes();
    }

    @SuppressWarnings("unchecked")
    private void loadHomes() {
        if (!homesFile.exists()) {
            plugin.getLogger().info("homes.yml not found. Creating new one.");
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml: " + e.getMessage());
            }
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(homesFile);
        playerHomes.clear();

        if (config.isConfigurationSection("homes")) {
            for (String playerUuidString : config.getConfigurationSection("homes").getKeys(false)) {
                try {
                    UUID playerUuid = UUID.fromString(playerUuidString);
                    List<Map<?, ?>> homesMapList = (List<Map<?, ?>>) config.getList("homes." + playerUuidString);
                    if (homesMapList != null) {
                        List<Home> homesList = homesMapList.stream()
                                .map(map -> new Home((Map<String, Object>) map))
                                .collect(Collectors.toList());
                        playerHomes.put(playerUuid, homesList);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID found in homes.yml: " + playerUuidString + " - " + e.getMessage());
                } catch (ClassCastException e) {
                    plugin.getLogger().warning("Error casting home data for " + playerUuidString + ": " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("Loaded " + playerHomes.size() + " players' homes.");
    }

    public void saveHomes() {
        YamlConfiguration config = new YamlConfiguration();
        playerHomes.forEach((uuid, homesList) -> {
            config.set("homes." + uuid.toString(), homesList.stream()
                    .map(Home::serialize)
                    .collect(Collectors.toList()));
        });

        try {
            config.save(homesFile);
            plugin.getLogger().info("Homes saved to homes.yml.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes to homes.yml: " + e.getMessage());
        }
    }

    public List<Home> getPlayerHomes(UUID playerUuid) {
        return playerHomes.getOrDefault(playerUuid, Collections.emptyList());
    }

    public Home getHome(UUID playerUuid, String homeName) {
        return getPlayerHomes(playerUuid).stream()
                .filter(home -> home.getName().equalsIgnoreCase(homeName))
                .findFirst()
                .orElse(null);
    }

    public boolean addHome(Home home) {
        List<Home> homes = playerHomes.computeIfAbsent(home.getPlayerUuid(), k -> new ArrayList<>());
        if (homes.stream().anyMatch(h -> h.getName().equalsIgnoreCase(home.getName()))) {
            return false;
        }
        homes.add(home);
        saveHomes();
        return true;
    }

    public boolean removeHome(UUID playerUuid, String homeName) {
        List<Home> homes = playerHomes.get(playerUuid);
        if (homes == null) {
            return false;
        }
        boolean removed = homes.removeIf(home -> home.getName().equalsIgnoreCase(homeName));
        if (removed) {
            if (homes.isEmpty()) {
                playerHomes.remove(playerUuid);
            }
            saveHomes();
        }
        return removed;
    }
}