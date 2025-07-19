package me.sanenuyan.homeGUI.listeners;

import me.sanenuyan.homeGUI.HomeGUI;
import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import me.sanenuyan.homeGUI.gui.HomeGUIHolder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomeGUIListener implements Listener {

    private final HomeGUI plugin;
    private final HomeManager homeManager;
    private final Map<UUID, Integer> playerCurrentPage;

    public HomeGUIListener(HomeGUI plugin, HomeManager homeManager, Map<UUID, Integer> playerCurrentPage) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.playerCurrentPage = playerCurrentPage;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomeGUIHolder)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        // Handle Home Item Click (left/right click)
        if (clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.hasItemMeta() && clickedItem.getItemMeta() instanceof SkullMeta) {
            String homeName = MiniMessage.miniMessage().stripTags(MiniMessage.miniMessage().serialize(clickedItem.getItemMeta().displayName()))
                    .replace("Home: ", "");

            Home homeToTeleport = homeManager.getHome(player.getUniqueId(), homeName);
            if (homeToTeleport == null) {
                Audience audience = plugin.adventure().player(player);
                Component errorMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_not_found_gui"));
                audience.sendMessage(errorMessage);
                plugin.openHomeGUI(player);
                return;
            }

            if (event.getClick().isLeftClick()) {
                plugin.teleportToHome(player, homeToTeleport);
                player.closeInventory();
            } else if (event.getClick().isRightClick()) {
                player.performCommand("delhome " + homeName);
            }
            return;
        }

        // Handle Navigation Buttons
        ConfigurationSection navButtonsSection = plugin.getConfig().getConfigurationSection("gui.navigation-buttons");
        if (navButtonsSection != null) {
            int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
            List<Home> allHomes = homeManager.getPlayerHomes(player.getUniqueId());
            List<Integer> homeSlots = plugin.getConfig().getIntegerList("gui.home-item-slots");
            int homesPerPage = homeSlots.isEmpty() ? 18 : homeSlots.size();
            int totalPages = (int) Math.ceil((double) allHomes.size() / homesPerPage);
            if (totalPages == 0) totalPages = 1;

            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasCustomModelData() &&
                    clickedItem.getItemMeta().getCustomModelData() == navButtonsSection.getInt("previous-page.custom_model_data", 0) &&
                    currentPage > 0) {
                plugin.openHomeGUI(player, currentPage - 1);
                return;
            }
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasCustomModelData() &&
                    clickedItem.getItemMeta().getCustomModelData() == navButtonsSection.getInt("next-page.custom_model_data", 0) &&
                    currentPage < totalPages - 1) {
                plugin.openHomeGUI(player, currentPage + 1);
                return;
            }
        }

        // Handle Custom Buttons
        ConfigurationSection customButtonsSection = plugin.getConfig().getConfigurationSection("gui.custom-buttons");
        if (customButtonsSection != null) {
            for (String key : customButtonsSection.getKeys(false)) {
                if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasCustomModelData() &&
                        clickedItem.getItemMeta().getCustomModelData() == customButtonsSection.getInt(key + ".custom_model_data", 0)) {

                    List<String> commands = customButtonsSection.getStringList(key + ".commands");
                    executeCommands(player, commands);
                    player.closeInventory();
                    return;
                }
            }
        }
    }

    private void executeCommands(@NotNull Player player, @NotNull List<String> commands) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        for (String command : commands) {
            String processedCommand = command.replace("{player}", player.getName());
            if (processedCommand.startsWith("console:")) {
                Bukkit.dispatchCommand(console, processedCommand.substring("console:".length()));
            } else {
                player.performCommand(processedCommand);
            }
        }
    }
}
