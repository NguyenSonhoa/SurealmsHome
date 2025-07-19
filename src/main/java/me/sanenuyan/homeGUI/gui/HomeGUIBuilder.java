package me.sanenuyan.homeGUI.gui;

import me.sanenuyan.homeGUI.HomeGUI;
import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.bukkit.inventory.ItemFlag;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

public class HomeGUIBuilder {

    private final HomeGUI plugin;
    private final HomeManager homeManager;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final TimeZone hanoiTimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh"); // GMT+7

    public HomeGUIBuilder(@NotNull HomeGUI plugin, @NotNull HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        dateFormat.setTimeZone(hanoiTimeZone);
    }

    public Inventory buildHomeGUI(@NotNull Player player, int page) {
        int guiSize = plugin.getConfig().getInt("gui.size", 27);

        Component title = MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("gui.title", "<dark_gray>Homes</dark_gray>")
        );

        Inventory gui = Bukkit.createInventory(player, guiSize, title);

        List<Home> playerHomes = homeManager.getPlayerHomes(player.getUniqueId());
        List<Integer> homeSlots = plugin.getConfig().getIntegerList("gui.home-item-slots");
        int homesPerPage = homeSlots.isEmpty() ? 18 : homeSlots.size();

        int maxHomesForPlayer = plugin.getPlayerMaxHomes(player);
        int currentHomesCount = playerHomes.size();

        int startIndex = page * homesPerPage;
        int endIndex = Math.min(startIndex + homesPerPage, playerHomes.size());

        for (int i = startIndex; i < endIndex; i++) {
            if (i - startIndex >= homeSlots.size()) break;
            Home home = playerHomes.get(i);
            ItemStack homeItem = getPlayerHead(home);
            gui.setItem(homeSlots.get(i - startIndex), homeItem);
        }

        for (int i = 0; i < homeSlots.size(); i++) {
            int slot = homeSlots.get(i);
            if (gui.getItem(slot) == null || Objects.requireNonNull(gui.getItem(slot)).getType().isAir()) {
                int homeSlotIndexInTotal = startIndex + i;

                if (homeSlotIndexInTotal < maxHomesForPlayer) {
                    ConfigurationSection unlockedSection = plugin.getConfig().getConfigurationSection("gui.unlocked-slot-item");
                    if (unlockedSection != null) {
                        ItemStack unlockedItem = getGuiItem(unlockedSection, null);
                        ItemMeta meta = unlockedItem.getItemMeta();
                        List<Component> updatedLore = meta.lore().stream()
                                .map(c -> MiniMessage.miniMessage().deserialize(
                                        MiniMessage.miniMessage().serialize(c)
                                                .replace("<current_homes>", String.valueOf(currentHomesCount))
                                                .replace("<max_homes>", maxHomesForPlayer == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(maxHomesForPlayer))
                                ))
                                .collect(Collectors.toList());
                        meta.lore(updatedLore);
                        unlockedItem.setItemMeta(meta);
                        gui.setItem(slot, unlockedItem);
                    }
                } else {
                    ConfigurationSection lockedSection = plugin.getConfig().getConfigurationSection("gui.locked-slot-item");
                    if (lockedSection != null) {
                        ItemStack lockedItem = getGuiItem(lockedSection, null);
                        ItemMeta meta = lockedItem.getItemMeta();
                        List<Component> updatedLore = meta.lore().stream()
                                .map(c -> MiniMessage.miniMessage().deserialize(
                                        MiniMessage.miniMessage().serialize(c)
                                                .replace("<current_homes>", String.valueOf(currentHomesCount))
                                                .replace("<max_homes>", maxHomesForPlayer == Integer.MAX_VALUE ? "Unlimited" : String.valueOf(maxHomesForPlayer))
                                ))
                                .collect(Collectors.toList());
                        meta.lore(updatedLore);
                        lockedItem.setItemMeta(meta);
                        gui.setItem(slot, lockedItem);
                    }
                }
            }
        }


        ConfigurationSection fillerSection = plugin.getConfig().getConfigurationSection("gui.filler-item");
        if (fillerSection != null) {
            ItemStack fillerItem = getGuiItem(fillerSection, null);
            ItemMeta fillerMeta = fillerItem.getItemMeta();
            fillerMeta.setHideTooltip(true);
            fillerItem.setItemMeta(fillerMeta);

            for (int i = 0; i < guiSize; i++) {
                if (gui.getItem(i) == null || Objects.requireNonNull(gui.getItem(i)).getType().isAir()) {
                    gui.setItem(i, fillerItem);
                }
            }
        }
        ConfigurationSection navButtonsSection = plugin.getConfig().getConfigurationSection("gui.navigation-buttons");
        if (navButtonsSection != null) {
            int totalPages = (int) Math.ceil((double) playerHomes.size() / homesPerPage);
            if (totalPages == 0) totalPages = 1;

            if (page > 0) {
                ConfigurationSection prevPageSection = navButtonsSection.getConfigurationSection("previous-page");
                if (prevPageSection != null) {
                    String prevButtonId = prevPageSection.getString("id");
                    gui.setItem(prevPageSection.getInt("slot"), getGuiItem(prevPageSection, prevButtonId));
                }
            }
            if (page < totalPages - 1) {
                ConfigurationSection nextPageSection = navButtonsSection.getConfigurationSection("next-page");
                if (nextPageSection != null) {
                    String nextButtonId = nextPageSection.getString("id");
                    gui.setItem(nextPageSection.getInt("slot"), getGuiItem(nextPageSection, nextButtonId));
                }
            }
        }

        ConfigurationSection customButtonsSection = plugin.getConfig().getConfigurationSection("gui.custom-buttons");
        if (customButtonsSection != null) {
            for (String key : customButtonsSection.getKeys(false)) {
                ConfigurationSection buttonSection = customButtonsSection.getConfigurationSection(key);
                if (buttonSection != null) {
                    String buttonId = buttonSection.getString("id");
                    gui.setItem(buttonSection.getInt("slot"), getGuiItem(buttonSection, buttonId));
                }
            }
        }

        return gui;
    }

    private ItemStack getPlayerHead(@NotNull Home home) {
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) playerHead.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(home.getPlayerUUID());
        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
            meta.setOwningPlayer(offlinePlayer);
        }

        String worldName = home.getWorldName();
        double x = home.getX();
        double y = home.getY();
        double z = home.getZ();

        String formattedCreationDate = dateFormat.format(new Date(home.getCreationDate()));

        Component displayName = MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("messages.home_display_name", "<green><!i>Home: <home_name></green>")
                        .replace("<home_name>", home.getName())
        );
        meta.displayName(displayName);

        List<Component> lore = new ArrayList<>();
        lore.add(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_lore_line1", "<gray><!i></gray>")));
        lore.add(MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("messages.home_lore_line2", "<gray><!i>World: <world_name></gray>")
                        .replace("<world_name>", worldName)
        ));
        lore.add(MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("messages.home_lore_line3", "<gray><!i>Coords: <x>, <y>, <z></gray>")
                        .replace("<x>", String.valueOf((int)x))
                        .replace("<y>", String.valueOf((int)y))
                        .replace("<z>", String.valueOf((int)z))
        ));
        lore.add(MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("messages.home_lore_creation_date", "<gray><!i>Created: <date></gray>")
                        .replace("<date>", formattedCreationDate)
        ));
        lore.add(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_lore_line4", "<green><!i>Left-Click to teleport</green>")));
        lore.add(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_lore_line5", "<red><!i>Right-Click to delete</red>")));

        meta.lore(lore);

        int homeItemCustomModelData = plugin.getConfig().getInt("gui.home-item-settings.custom_model_data", 0);
        if (homeItemCustomModelData != 0) {
            meta.setCustomModelData(homeItemCustomModelData);
        }

        meta.getPersistentDataContainer().set(plugin.HOME_NAME_KEY, PersistentDataType.STRING, home.getName());

        playerHead.setItemMeta(meta);
        return playerHead;
    }

    private ItemStack getGuiItem(@NotNull ConfigurationSection section, String buttonId) {
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String displayNameStr = section.getString("display_name");
        if (displayNameStr != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(displayNameStr));
        }

        List<String> loreStrList = section.getStringList("lore");
        if (!loreStrList.isEmpty()) {
            List<Component> loreComponents = loreStrList.stream()
                    .map(MiniMessage.miniMessage()::deserialize)
                    .collect(Collectors.toList());
            meta.lore(loreComponents);
        }

        int customModelData = section.getInt("custom_model_data", 0);
        if (customModelData != 0) {
            meta.setCustomModelData(customModelData);
        }

        if (buttonId != null && !buttonId.isEmpty()) {
            meta.getPersistentDataContainer().set(HomeGUI.CUSTOM_BUTTON_ID_KEY, PersistentDataType.STRING, buttonId);
        }

        item.setItemMeta(meta);
        return item;
    }
}