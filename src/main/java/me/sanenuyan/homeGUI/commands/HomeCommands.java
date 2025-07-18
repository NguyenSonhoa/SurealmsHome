package me.sanenuyan.homeGUI.commands;

import me.sanenuyan.homeGUI.HomeGUI;
import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class HomeCommands implements CommandExecutor, TabCompleter {

    private final HomeGUI plugin;
    private final HomeManager homeManager;

    public HomeCommands(HomeGUI plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Audience audience = plugin.adventure().sender(sender);

        if (command.getName().equalsIgnoreCase("home")) {
            return handleHomeCommand(sender, args, audience);
        } else if (command.getName().equalsIgnoreCase("homerefresh")) {
            return handleHomeRefreshCommand(sender, audience);
        } else if (command.getName().equalsIgnoreCase("sethome")) {
            return handleSetHomeCommand(sender, args, audience);
        } else if (command.getName().equalsIgnoreCase("delhome")) {
            return handleDelHomeCommand(sender, args, audience);
        }
        return false;
    }

    private boolean handleHomeCommand(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Audience audience) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command must be player to use.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.openHomeGUI(player);
        } else if (args.length == 1) {
            String homeName = args[0];
            Home homeToTeleport = homeManager.getHome(player.getUniqueId(), homeName);
            if (homeToTeleport != null) {
                plugin.teleportToHome(player, homeToTeleport);
            } else {
                Component notFoundMessage = MiniMessage.miniMessage().deserialize(
                        plugin.getConfig().getString("messages.home_not_found_quick_command")
                                .replace("<home_name>", homeName)
                );
                audience.sendMessage(notFoundMessage);
            }
        } else {
            Component usageMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_usage_quick_command"));
            audience.sendMessage(usageMessage);
        }
        return true;
    }

    private boolean handleHomeRefreshCommand(@NotNull CommandSender sender, @NotNull Audience audience) {
        if (!sender.hasPermission("homegui.reload")) {
            Component noPermissionMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.no_permission"));
            audience.sendMessage(noPermissionMessage);
            return true;
        }

        plugin.reloadConfig();
        homeManager.saveHomes(); // Ensure homes are saved/reloaded if needed
        Component reloadSuccessMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.config_reloaded"));
        audience.sendMessage(reloadSuccessMessage);
        return true;
    }

    private boolean handleSetHomeCommand(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Audience audience) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command must be player to use.");
            return true;
        }
        Player player = (Player) sender;

        List<String> blockedWorlds = plugin.getConfig().getStringList("settings.blocked_sethome_worlds");
        if (blockedWorlds.contains(player.getWorld().getName())) {
            Component blockedMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.sethome_blocked_world")
                            .replace("<world_name>", player.getWorld().getName())
            );
            audience.sendMessage(blockedMessage);
            return true;
        }

        if (args.length == 0) {
            Component usageMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.sethome_usage"));
            audience.sendMessage(usageMessage);
            return true;
        }
        String homeName = args[0];

        int maxHomes = getPlayerMaxHomes(player);
        if (maxHomes != Integer.MAX_VALUE && homeManager.getPlayerHomes(player.getUniqueId()).size() >= maxHomes) {
            Component limitMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.max_homes_reached")
                            .replace("<max_homes>", String.valueOf(maxHomes))
            );
            audience.sendMessage(limitMessage);
            return true;
        }

        Home newHome = new Home(player.getUniqueId(), homeName, player.getLocation());
        if (homeManager.addHome(newHome)) {
            Component successMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.sethome_success")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(successMessage);
            // Reopen GUI to refresh if player is in GUI
            Component guiTitle = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui.title", "<dark_gray>Homes</dark_gray>"));
            if (player.getOpenInventory().title().equals(guiTitle)) {
                plugin.openHomeGUI(player);
            }
        } else {
            Component failMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.sethome_exists")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(failMessage);
        }
        return true;
    }

    private boolean handleDelHomeCommand(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Audience audience) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command must be player to use.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            Component usageMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.delhome_usage"));
            audience.sendMessage(usageMessage);
            return true;
        }
        String homeName = args[0];

        if (homeManager.removeHome(player.getUniqueId(), homeName)) {
            Component successMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.delhome_success")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(successMessage);
            // Reopen GUI to refresh if player is in GUI
            Component guiTitle = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("gui.title", "<dark_gray>Homes</dark_gray>"));
            if (player.getOpenInventory().title().equals(guiTitle)) {
                plugin.openHomeGUI(player);
            }
        } else {
            Component failMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.delhome_not_found")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(failMessage);
        }
        return true;
    }
    public int getPlayerMaxHomes(@NotNull Player player) {
        int maxHomes = plugin.getConfig().getInt("settings.default_max_homes", 1);

        ConfigurationSection tiersSection = plugin.getConfig().getConfigurationSection("settings.max_homes_tiers");
        if (tiersSection != null) {
            for (String key : tiersSection.getKeys(false)) {
                String permission = tiersSection.getString(key + ".permission");
                int limit = tiersSection.getInt(key + ".limit", -1);

                if (permission != null && player.hasPermission(permission)) {
                    if (limit == 0) {
                        return Integer.MAX_VALUE;
                    }
                    if (limit > maxHomes) {
                        maxHomes = limit;
                    }
                }
            }
        }
        return maxHomes;
    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("home")) {
            if (args.length == 1) {
                List<String> homeNames = homeManager.getPlayerHomes(player.getUniqueId()).stream()
                        .map(Home::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
                return homeNames;
            }
        }
        return Collections.emptyList();
    }
}
