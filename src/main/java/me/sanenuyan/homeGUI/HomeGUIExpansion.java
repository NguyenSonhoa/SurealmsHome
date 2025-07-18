package me.sanenuyan.homeGUI.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.sanenuyan.homeGUI.HomeGUI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HomeGUIExpansion extends PlaceholderExpansion {

    private final HomeGUI plugin;

    public HomeGUIExpansion(HomeGUI plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "homegui";
    }

    @Override
    public @NotNull String getAuthor() {
        return "YourName";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("homes_count")) {
            return String.valueOf(plugin.getHomeManager().getPlayerHomes(player.getUniqueId()).size());
        }

        if (identifier.equals("max_homes")) {
            return String.valueOf(plugin.getPlayerMaxHomes(player));
        }

        return null;
    }
}