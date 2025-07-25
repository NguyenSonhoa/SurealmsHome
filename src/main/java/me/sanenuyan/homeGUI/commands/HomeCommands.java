package me.sanenuyan.homeGUI.commands;

import me.sanenuyan.homeGUI.HomeGUI;
import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
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
            sender.sendMessage("Bạn phải là người chơi để sử dụng lệnh này.");
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
                        plugin.getConfig().getString("messages.home_not_found_quick_command", "<red>Home '<home_name>' not found.</red>")
                                .replace("<home_name>", homeName)
                );
                audience.sendMessage(notFoundMessage);
            }
        } else {
            Component usageMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.home_usage_quick_command", "<yellow>Sử dụng: /home [tên_home]</yellow>"));
            audience.sendMessage(usageMessage);
        }
        return true;
    }

    private boolean handleHomeRefreshCommand(@NotNull CommandSender sender, @NotNull Audience audience) {
        if (!sender.hasPermission("homegui.reload")) {
            Component noPermissionMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.no_permission", "<red>Bạn không có quyền để thực hiện lệnh này.</red>"));
            audience.sendMessage(noPermissionMessage);
            return true;
        }

        plugin.reloadPlugin();
        Component reloadSuccessMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.plugin_reloaded", "<green>Plugin HomeGUI đã được tải lại cấu hình!</green>"));
        audience.sendMessage(reloadSuccessMessage);
        return true;
    }

    private boolean handleSetHomeCommand(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Audience audience) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bạn phải là người chơi để sử dụng lệnh này.");
            return true;
        }
        Player player = (Player) sender;

        List<String> blockedWorlds = plugin.getConfig().getStringList("settings.blocked_sethome_worlds");
        if (player.getWorld() != null && blockedWorlds.contains(player.getWorld().getName())) {
            Component blockedMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.sethome_blocked_world", "<red>Bạn không thể đặt home ở thế giới này: <world_name></red>")
                            .replace("<world_name>", player.getWorld().getName())
            );
            audience.sendMessage(blockedMessage);
            return true;
        }

        if (args.length == 0) {
            Component usageMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.sethome_usage", "<yellow>Sử dụng: /sethome <tên_home></yellow>"));
            audience.sendMessage(usageMessage);
            return true;
        }
        String homeName = args[0];

        if (homeName.length() > 32 || !homeName.matches("[a-zA-Z0-9_]+")) {
            Component invalidNameMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.sethome_invalid_name", "<red>Tên home chỉ được chứa chữ cái, số và dấu gạch dưới, và tối đa 32 ký tự.</red>"));
            audience.sendMessage(invalidNameMessage);
            return true;
        }

        int maxHomes = getPlayerMaxHomes(player);
        if (maxHomes != Integer.MAX_VALUE && homeManager.getPlayerHomes(player.getUniqueId()).size() >= maxHomes) {
            Component limitMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.max_homes_reached", "<red>Bạn đã đạt đến giới hạn <max_homes> home.</red>")
                            .replace("<max_homes>", String.valueOf(maxHomes))
            );
            audience.sendMessage(limitMessage);
            return true;
        }

        if (homeManager.homeExists(player.getUniqueId(), homeName)) {
            Component existMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.sethome_exists", "<red>Home <home_name> đã tồn tại. Bạn có thể sử dụng /delhome <home_name> để xóa nó trước.</red>")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(existMessage);
            return true;
        }

        Home newHome = new Home(player.getUniqueId(), homeName, player.getLocation());
        if (homeManager.addHome(newHome)) {
            Component successMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.sethome_success", "<green>Home <home_name> đã được đặt thành công!</green>")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(successMessage);
            if (plugin.getPlayerOpenHomeGUITitles().containsKey(player.getUniqueId())) {
                plugin.openHomeGUI(player);
            }
        } else {
            Component failMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.sethome_fail", "<red>Có lỗi xảy ra khi đặt home <home_name>.</red>")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(failMessage);
        }
        return true;
    }

    private boolean handleDelHomeCommand(@NotNull CommandSender sender, @NotNull String[] args, @NotNull Audience audience) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bạn phải là người chơi để sử dụng lệnh này.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            Component usageMessage = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.delhome_usage", "<yellow>Sử dụng: /delhome <tên_home></yellow>"));
            audience.sendMessage(usageMessage);
            return true;
        }
        String homeName = args[0];

        if (homeManager.deleteHome(player.getUniqueId(), homeName)) {
            Component successMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.delhome_success", "<green>Home <home_name> đã bị xóa!</green>")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(successMessage);
            if (plugin.getPlayerOpenHomeGUITitles().containsKey(player.getUniqueId())) {
                plugin.openHomeGUI(player);
            }
        } else {
            Component failMessage = MiniMessage.miniMessage().deserialize(
                    plugin.getConfig().getString("messages.delhome_not_found", "<red>Home <home_name> không tồn tại hoặc bạn không sở hữu nó.</red>")
                            .replace("<home_name>", homeName)
            );
            audience.sendMessage(failMessage);
        }
        return true;
    }

    public int getPlayerMaxHomes(@NotNull Player player) {
        return plugin.getPlayerMaxHomes(player);
    }@Override public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();}Player player = (Player) sender;if (command.getName().equalsIgnoreCase("home") || command.getName().equalsIgnoreCase("delhome")) {if (args.length == 1) {List<String> homeNames = homeManager.getPlayerHomes(player.getUniqueId()).stream()
                .map(Home::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());return homeNames;}} else if (command.getName().equalsIgnoreCase("sethome")) {
            if (args.length == 1) {
                return Collections.emptyList();}
        }
        return Collections.emptyList();
    }
}