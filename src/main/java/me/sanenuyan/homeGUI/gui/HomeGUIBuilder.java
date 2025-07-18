package me.sanenuyan.homeGUI.gui;

import me.sanenuyan.homeGUI.HomeGUI;
import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HomeGUIBuilder {
    private final HomeGUI plugin;
    private final HomeManager homeManager;

    public HomeGUIBuilder(HomeGUI plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    public Inventory buildHomeGUI(@NotNull Player player, int page, int totalPages, List<Home> homesForPage) {
        int guiSize = plugin.getConfig().getInt("gui.size", 27);
        Component title = MiniMessage.miniMessage().deserialize(
                plugin.getConfig().getString("gui.title", "<dark_gray>Homes</dark_gray>")
                        .replace("<page>", String.valueOf(page + 1))
                        .replace("<total_pages>", String.valueOf(totalPages))
        );

        // --- Cập nhật dòng này để sử dụng HomeGUIHolder ---
        // Đầu tiên, tạo Inventory với một holder null, sau đó bọc nó lại bằng HomeGUIHolder
        Inventory gui = Bukkit.createInventory(null, guiSize, title);
        HomeGUIHolder holder = new HomeGUIHolder(gui); // Tạo holder mới
        // Thiết lập holder cho Inventory (điều này được thực hiện nội bộ bởi Bukkit.createInventory khi bạn truyền holder)
        // Tuy nhiên, vì chúng ta muốn truy cập InventoryHolder từ sự kiện, chúng ta cần đảm bảo nó được liên kết.
        // Cách tốt nhất là truyền holder trực tiếp khi tạo inventory.

        // Fix: Tạo Inventory trực tiếp với holder
        gui = Bukkit.createInventory(holder, guiSize, title);


        List<Integer> homeSlots = plugin.getConfig().getIntegerList("gui.home-item-slots");
        if (homeSlots.isEmpty()) {
            for (int i = 0; i < guiSize; i++) {
                if (i < guiSize - 9) {
                    homeSlots.add(i);
                }
            }
        }

        // Place Home Items
        for (int i = 0; i < homesForPage.size(); i++) {
            if (i < homeSlots.size()) {
                gui.setItem(homeSlots.get(i), getPlayerHead(player, homesForPage.get(i)));
            }
        }

        // Place Filler Items
        ItemStack fillerItem = getGuiItem("gui.filler-item");
        for (int i = 0; i < guiSize; i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType().isAir()) {
                if (!homeSlots.contains(i)) {
                    gui.setItem(i, fillerItem);
                }
            }
        }

        // Place Navigation Buttons
        ConfigurationSection navButtonsSection = plugin.getConfig().getConfigurationSection("gui.navigation-buttons");
        if (navButtonsSection != null) {
            // Previous Page Button
            if (page > 0) {
                ItemStack prevButton = getGuiItem("gui.navigation-buttons.previous-page");
                if (prevButton != null) {
                    int slot = navButtonsSection.getInt("previous-page.slot", -1);
                    if (slot != -1 && slot < guiSize) gui.setItem(slot, prevButton);
                }
            }
            // Next Page Button
            if (page < totalPages - 1) {
                ItemStack nextButton = getGuiItem("gui.navigation-buttons.next-page");
                if (nextButton != null) {
                    int slot = navButtonsSection.getInt("next-page.slot", -1);
                    if (slot != -1 && slot < guiSize) gui.setItem(slot, nextButton);
                }
            }
        }

        // Place Custom Buttons
        ConfigurationSection customButtonsSection = plugin.getConfig().getConfigurationSection("gui.custom-buttons");
        if (customButtonsSection != null) {
            for (String key : customButtonsSection.getKeys(false)) {
                ItemStack customButton = getGuiItem("gui.custom-buttons." + key);
                if (customButton != null) {
                    int slot = customButtonsSection.getInt(key + ".slot", -1);
                    if (slot != -1 && slot < guiSize) gui.setItem(slot, customButton);
                }
            }
        }
        return gui;
    }

    private @Nullable ItemStack getGuiItem(@NotNull String path) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) return null;

        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            plugin.getLogger().warning("Invalid material for path " + path + ": " + section.getString("material"));
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

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
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        item.setItemMeta(meta);
        return item;
    }


    private @NotNull ItemStack getPlayerHead(@NotNull Player player, @NotNull Home home) {
        Location homeLocation = home.toLocation();
        if (homeLocation != null && homeLocation.getWorld() != null) {
            String worldName = homeLocation.getWorld().getName();
            int x = homeLocation.getBlockX();
            int y = homeLocation.getBlockY();
            int z = homeLocation.getBlockZ();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
                meta.setOwningPlayer(offlinePlayer);

                Component displayName = MiniMessage.miniMessage().deserialize(
                        plugin.getConfig().getString("messages.home_display_name")
                                .replace("<home_name>", home.getName())
                );
                meta.displayName(displayName);

                meta.setCustomModelData(101); // Common custom model data for home heads

                List<Component> lore = Arrays.asList(
                        MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_lore_line1")),
                        MiniMessage.miniMessage().deserialize(
                                plugin.getConfig().getString("messages.home_lore_line2")
                                        .replace("<world_name>", worldName)
                        ),
                        MiniMessage.miniMessage().deserialize(
                                plugin.getConfig().getString("messages.home_lore_line3")
                                        .replace("<x>", String.valueOf(x))
                                        .replace("<y>", String.valueOf(y))
                                        .replace("<z>", String.valueOf(z))
                        ),
                        MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_lore_line4")),
                        MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_lore_line5"))
                );
                meta.lore(lore);
                head.setItemMeta(meta);
            }
            return head;
        } else {
            // Fallback item if home location is invalid
            return getGuiItem("gui.filler-item");
        }
    }
}