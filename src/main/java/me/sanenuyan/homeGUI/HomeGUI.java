package me.sanenuyan.homeGUI;

import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import me.sanenuyan.homeGUI.placeholder.HomeGUIExpansion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HomeGUI extends JavaPlugin implements Listener, TabCompleter {
   private BukkitAudiences adventure;
   private HomeManager homeManager;

   @Override
   public void onEnable() {
      this.saveDefaultConfig();
      this.adventure = BukkitAudiences.create(this);
      this.homeManager = new HomeManager(this);

      Bukkit.getPluginManager().registerEvents(this, this);

      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
         new HomeGUIExpansion(this).register();
         getLogger().info("PlaceholderAPI expansion registered!");
      } else {
         getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
      }

      this.getCommand("home").setExecutor(new CommandExecutor() {
         @Override
         public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player)) {
               sender.sendMessage("Bạn phải là người chơi để sử dụng lệnh này.");
               return true;
            }
            Player player = (Player) sender;
            Audience audience = adventure.player(player);

            if (args.length == 0) {
               openHomeGUI(player);
            } else if (args.length == 1) {
               String homeName = args[0];
               Home homeToTeleport = homeManager.getHome(player.getUniqueId(), homeName);
               if (homeToTeleport != null) {
                  teleportToHome(player, homeToTeleport);
               } else {
                  Component notFoundMessage = MiniMessage.miniMessage().deserialize(
                          getConfig().getString("messages.home_not_found_quick_command")
                                  .replace("<home_name>", homeName)
                  );
                  audience.sendMessage(notFoundMessage);
               }
            } else {
               Component usageMessage = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.home_usage_quick_command"));
               audience.sendMessage(usageMessage);
            }
            return true;
         }
      });
      this.getCommand("home").setTabCompleter(this);


      this.getCommand("homes").setExecutor(new CommandExecutor() {
         @Override
         public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            Audience audience = adventure.sender(sender);
            Component message = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.homes_command_blocked"));
            audience.sendMessage(message);
            return true;
         }
      });


      this.getCommand("homerefresh").setExecutor(new CommandExecutor() {
         @Override
         public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!sender.hasPermission("homegui.reload")) {
               Audience audience = adventure.sender(sender);
               Component noPermissionMessage = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.no_permission"));
               audience.sendMessage(noPermissionMessage);
               return true;
            }

            reloadConfig();
            Audience audience = adventure.sender(sender);
            Component reloadSuccessMessage = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.config_reloaded"));
            audience.sendMessage(reloadSuccessMessage);
            return true;
         }
      });

      this.getCommand("sethome").setExecutor(new CommandExecutor() {
         @Override
         public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player)) {
               sender.sendMessage("Bạn phải là người chơi để sử dụng lệnh này.");
               return true;
            }
            Player player = (Player) sender;
            Audience audience = adventure.player(player);

            List<String> blockedWorlds = getConfig().getStringList("settings.blocked_sethome_worlds");
            if (blockedWorlds.contains(player.getWorld().getName())) {
               Component blockedMessage = MiniMessage.miniMessage().deserialize(
                       getConfig().getString("messages.sethome_blocked_world")
                               .replace("<world_name>", player.getWorld().getName())
               );
               audience.sendMessage(blockedMessage);
               return true;
            }

            if (args.length == 0) {
               Component usageMessage = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.sethome_usage"));
               audience.sendMessage(usageMessage);
               return true;
            }
            String homeName = args[0];

            int maxHomes = getPlayerMaxHomes(player);
            if (maxHomes != Integer.MAX_VALUE && homeManager.getPlayerHomes(player.getUniqueId()).size() >= maxHomes) {
               Component limitMessage = MiniMessage.miniMessage().deserialize(
                       getConfig().getString("messages.max_homes_reached")
                               .replace("<max_homes>", String.valueOf(maxHomes))
               );
               audience.sendMessage(limitMessage);
               return true;
            }

            Home newHome = new Home(player.getUniqueId(), homeName, player.getLocation());
            if (homeManager.addHome(newHome)) {
               Component successMessage = MiniMessage.miniMessage().deserialize(
                       getConfig().getString("messages.sethome_success")
                               .replace("<home_name>", homeName)
               );
               audience.sendMessage(successMessage);
            } else {
               Component failMessage = MiniMessage.miniMessage().deserialize(
                       getConfig().getString("messages.sethome_exists")
                               .replace("<home_name>", homeName)
               );
               audience.sendMessage(failMessage);
            }
            return true;
         }
      });

      this.getCommand("delhome").setExecutor(new CommandExecutor() {
         @Override
         public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player)) {
               sender.sendMessage("Bạn phải là người chơi để sử dụng lệnh này.");
               return true;
            }
            Player player = (Player) sender;
            Audience audience = adventure.player(player);

            if (args.length == 0) {
               Component usageMessage = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.delhome_usage"));
               audience.sendMessage(usageMessage);
               return true;
            }
            String homeName = args[0];

            if (homeManager.removeHome(player.getUniqueId(), homeName)) {
               Component successMessage = MiniMessage.miniMessage().deserialize(
                       getConfig().getString("messages.delhome_success")
                               .replace("<home_name>", homeName)
               );
               audience.sendMessage(successMessage);
            } else {
               Component failMessage = MiniMessage.miniMessage().deserialize(
                       getConfig().getString("messages.delhome_not_found")
                               .replace("<home_name>", homeName)
               );
               audience.sendMessage(failMessage);
            }
            return true;
         }
      });
   }

   @Override
   public void onDisable() {
      if (this.adventure != null) {
         this.adventure.close();
      }
      if (this.homeManager != null) {
         this.homeManager.saveHomes();
      }
   }

   public void openHomeGUI(@NotNull Player player) {
      Component title = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.home_gui_title"));
      Inventory gui = Bukkit.createInventory((InventoryHolder)null, 27, LegacyComponentSerializer.legacySection().serialize(title));

      List<Home> homes = homeManager.getPlayerHomes(player.getUniqueId());
      int slot = 0;

      for(Home home : homes) {
         gui.setItem(slot, this.getPlayerHead(player, home));
         ++slot;
      }

      while(slot < 27) {
         gui.setItem(slot, this.getBarrierSlot());
         ++slot;
      }
      player.openInventory(gui);
   }

   @NotNull
   private ItemStack getPlayerHead(@NotNull Player player, @NotNull Home home) {
      Location homeLocation = home.toLocation();
      if (homeLocation != null && homeLocation.getWorld() != null) {
         String worldName = homeLocation.getWorld().getName();
         int x = homeLocation.getBlockX();
         int y = homeLocation.getBlockY();
         int z = homeLocation.getBlockZ();

         ItemStack head = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta meta = (SkullMeta)head.getItemMeta();
         meta.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));

         Component displayName = MiniMessage.miniMessage().deserialize(
                 getConfig().getString("messages.home_display_name")
                         .replace("<home_name>", home.getName())
         );
         meta.displayName(displayName);

         meta.setCustomModelData(101);

         List<Component> lore = Arrays.asList(
                 MiniMessage.miniMessage().deserialize(getConfig().getString("messages.home_lore_line1")),
                 MiniMessage.miniMessage().deserialize(
                         getConfig().getString("messages.home_lore_line2")
                                 .replace("<world_name>", worldName)
                 ),
                 MiniMessage.miniMessage().deserialize(
                         getConfig().getString("messages.home_lore_line3")
                                 .replace("<x>", String.valueOf(x))
                                 .replace("<y>", String.valueOf(y))
                                 .replace("<z>", String.valueOf(z))
                 ),
                 MiniMessage.miniMessage().deserialize(getConfig().getString("messages.home_lore_line4")),
                 MiniMessage.miniMessage().deserialize(getConfig().getString("messages.home_lore_line5"))
         );
         meta.lore(lore);

         head.setItemMeta(meta);
         return head;
      } else {
         return this.getBarrierSlot();
      }
   }

   @NotNull
   private ItemStack getBarrierSlot() {
      ItemStack barrier = new ItemStack(Material.BARRIER);
      ItemMeta meta = barrier.getItemMeta();

      Component displayName = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.empty_home_display_name"));
      meta.displayName(displayName);

      meta.setCustomModelData(10);

      List<Component> lore = Arrays.asList(
              MiniMessage.miniMessage().deserialize(getConfig().getString("messages.empty_home_lore_line1")),
              MiniMessage.miniMessage().deserialize(getConfig().getString("messages.empty_home_lore_line2")),
              MiniMessage.miniMessage().deserialize(getConfig().getString("messages.empty_home_lore_line3"))
      );
      meta.lore(lore);

      barrier.setItemMeta(meta);
      return barrier;
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      Component expectedTitle = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.home_gui_title"));
      if (LegacyComponentSerializer.legacySection().deserialize(event.getView().getTitle()).equals(expectedTitle)) {
         event.setCancelled(true);
         Player player = (Player)event.getWhoClicked();
         ItemStack clickedItem = event.getCurrentItem();
         if (clickedItem == null || clickedItem.getType() == Material.BARRIER) {
            return;
         }

         if (clickedItem.getType() == Material.PLAYER_HEAD) {
            String homeName = MiniMessage.miniMessage().serialize(clickedItem.getItemMeta().displayName()).replace("<green>Home: ", "").replace("</green>", "");

            Home homeToTeleport = homeManager.getHome(player.getUniqueId(), homeName);
            if (homeToTeleport == null) {
               Audience audience = adventure.player(player);
               Component errorMessage = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.home_not_found_gui"));
               audience.sendMessage(errorMessage);
               return;
            }

            if (event.getClick().isLeftClick()) {
               this.teleportToHome(player, homeToTeleport);
            } else if (event.getClick().isRightClick()) {
               event.setCancelled(true);
               player.performCommand("delhome " + homeName);
               this.openHomeGUI(player);
            }
         }
      }
   }

   private void teleportToHome(@NotNull Player player, @NotNull Home home) {
      Location homeLocation = home.toLocation();
      Audience audience = adventure.player(player);

      if (homeLocation == null || homeLocation.getWorld() == null) {
         Component message = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.invalid_world"));
         audience.sendMessage(message);
         return;
      }

      World targetWorld = homeLocation.getWorld();
      long targetWorldCurrentSeed = targetWorld.getSeed();

      long storedWorldSeed = home.getWorldSeed();

      if (targetWorldCurrentSeed != storedWorldSeed) {
         Component message = MiniMessage.miniMessage().deserialize(
                 getConfig().getString("messages.world_seed_mismatch_stored")
                         .replace("<home_name>", home.getName())
                         .replace("<home_world>", home.getWorldName())
         );
         audience.sendMessage(message);
         return;
      }

      player.teleport(homeLocation);
      Component message = MiniMessage.miniMessage().deserialize(
              getConfig().getString("messages.teleport_success")
                      .replace("<home_name>", home.getName())
      );
      audience.sendMessage(message);
   }

   public int getPlayerMaxHomes(@NotNull Player player) {
      int maxHomes = getConfig().getInt("settings.default_max_homes", 1);

      ConfigurationSection tiersSection = getConfig().getConfigurationSection("settings.max_homes_tiers");
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

   public HomeManager getHomeManager() {
      return homeManager;
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