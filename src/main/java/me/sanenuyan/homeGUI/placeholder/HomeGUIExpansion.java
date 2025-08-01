package me.sanenuyan.homeGUI.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.sanenuyan.homeGUI.HomeGUI;
import me.sanenuyan.homeGUI.commands.HomeCommands; // Import HomeCommands
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HomeGUIExpansion extends PlaceholderExpansion {

    private final HomeGUI plugin;
    private final HomeCommands homeCommands;

    public HomeGUIExpansion(HomeGUI plugin, HomeCommands homeCommands) {
        this.plugin = plugin;
        this.homeCommands = homeCommands;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "homegui";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SaneNuyan";
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
            return String.valueOf(homeCommands.getPlayerMaxHomes(player));
        }

        return null;
    }
}