package me.sanenuyan.homeGUI.listeners;

import me.sanenuyan.homeGUI.HomeGUI;
import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class HomeGUIListener implements Listener {

    private final HomeGUI plugin;
    private final HomeManager homeManager;
    private final Map<UUID, Integer> playerCurrentPage;
    private final Map<UUID, Component> playerOpenHomeGUITitles;

    public HomeGUIListener(HomeGUI plugin, HomeManager homeManager, Map<UUID, Integer> playerCurrentPage, Map<UUID, Component> playerOpenHomeGUITitles) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.playerCurrentPage = playerCurrentPage;
        this.playerOpenHomeGUITitles = playerOpenHomeGUITitles;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (playerOpenHomeGUITitles.containsKey(player.getUniqueId())) {
            Component expectedTitle = playerOpenHomeGUITitles.get(player.getUniqueId());
            if (event.getView().title().equals(expectedTitle)) {
                event.setCancelled(true);

                if (player.getItemOnCursor() != null && player.getItemOnCursor().getType() != Material.AIR) {
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                }

                if (clickedItem == null || clickedItem.getType().isAir()) {
                    return;
                }

                ItemMeta meta = clickedItem.getItemMeta();
                if (meta == null) {
                    return;
                }

                String buttonId = meta.getPersistentDataContainer().get(HomeGUI.CUSTOM_BUTTON_ID_KEY, PersistentDataType.STRING);

                if (buttonId != null) {
                    int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);

                    String prevPageButtonId = Objects.requireNonNull(plugin.getConfig().getString("gui.navigation-buttons.previous-page.id"));
                    String nextPageButtonId = Objects.requireNonNull(plugin.getConfig().getString("gui.navigation-buttons.next-page.id"));
                    String closeButtonId = Objects.requireNonNull(plugin.getConfig().getString("gui.custom-buttons.close-button.id"));

                    if (buttonId.equals(prevPageButtonId)) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            plugin.openHomeGUI(player, Math.max(0, currentPage - 1));
                        }, 3L);
                    } else if (buttonId.equals(nextPageButtonId)) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            plugin.openHomeGUI(player, currentPage + 1);
                        }, 3L);
                    } else if (buttonId.equals(closeButtonId)) {
                        player.closeInventory();
                    } else {
                        ConfigurationSection customButtons = plugin.getConfig().getConfigurationSection("gui.custom-buttons");
                        if (customButtons != null) {
                            for (String key : customButtons.getKeys(false)) {
                                ConfigurationSection buttonSection = customButtons.getConfigurationSection(key);
                                if (buttonSection != null && buttonId.equals(buttonSection.getString("id"))) {
                                    List<String> commands = buttonSection.getStringList("commands");
                                    for (String cmd : commands) {
                                        if (!cmd.equals("player:close")) {
                                            player.closeInventory();
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    if (cmd.startsWith("player:")) {
                                                        player.performCommand(cmd.substring("player:".length()).replace("{player}", player.getName()));
                                                    } else if (cmd.startsWith("console:")) {
                                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring("console:".length()).replace("{player}", player.getName()));
                                                    }
                                                }
                                            }.runTaskLater(plugin, 2L);
                                        } else {
                                            player.closeInventory();
                                        }
                                    }
                                    return;
                                }
                            }
                        }
                    }
                } else if (clickedItem.getType() == Material.PLAYER_HEAD) {
                    String homeName = meta.getPersistentDataContainer().get(plugin.HOME_NAME_KEY, PersistentDataType.STRING);

                    if (homeName != null && !homeName.isEmpty()) {
                        Home home = homeManager.getHome(player.getUniqueId(), homeName);
                        if (home != null) {
                            if (event.isLeftClick()) {
                                plugin.teleportToHome(player, home);
                                player.closeInventory();
                            } else if (event.isRightClick()) {
                                if (homeManager.deleteHome(player.getUniqueId(), home.getName())) {
                                    Audience audience = plugin.adventure().player(player);
                                    Component successMessage = MiniMessage.miniMessage().deserialize(
                                            plugin.getConfig().getString("messages.delhome_success")
                                                    .replace("<home_name>", home.getName())
                                    );
                                    audience.sendMessage(successMessage);

                                    player.closeInventory();

                                    int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                        plugin.openHomeGUI(player, currentPage);
                                    }, 3L);
                                } else {
                                    Audience audience = plugin.adventure().player(player);
                                    Component errorMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_not_found_gui", "<red>Home không tồn tại hoặc đã bị xóa. GUI đã được làm mới.</red>"));
                                    audience.sendMessage(errorMessage);

                                    player.closeInventory();

                                    int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                        plugin.openHomeGUI(player, currentPage);
                                    }, 3L);
                                }
                            }
                        } else {
                            Audience audience = plugin.adventure().player(player);
                            Component errorMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_not_found_gui", "<red>Home không tồn tại hoặc đã bị xóa. GUI đã được làm mới.</red>"));
                            audience.sendMessage(errorMessage);

                            player.closeInventory();

                            int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                plugin.openHomeGUI(player, currentPage);
                            }, 3L);
                        }
                    } else {
                        plugin.getLogger().warning("Clicked a player head in HomeGUI without a valid HOME_NAME_KEY. Item: " + clickedItem.getType());

                        player.closeInventory();
                        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            plugin.openHomeGUI(player, currentPage);
                        }, 3L);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();

        if (playerOpenHomeGUITitles.containsKey(player.getUniqueId())) {
            Component expectedTitle = playerOpenHomeGUITitles.get(player.getUniqueId());
            if (event.getView().title().equals(expectedTitle)) {
                playerOpenHomeGUITitles.remove(player.getUniqueId());
                playerCurrentPage.remove(player.getUniqueId());
            }
        }
    }
}