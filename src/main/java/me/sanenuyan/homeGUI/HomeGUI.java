package me.sanenuyan.homeGUI;

import me.sanenuyan.homeGUI.commands.HomeCommands;
import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import me.sanenuyan.homeGUI.gui.HomeGUIBuilder;
import me.sanenuyan.homeGUI.listeners.HomeGUIListener;
import me.sanenuyan.homeGUI.placeholder.HomeGUIExpansion;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeGUI extends JavaPlugin {

   private HomeManager homeManager;
   private HomeGUIBuilder homeGUIBuilder;
   private BukkitAudiences adventure;
   private final Map<UUID, Integer> playerCurrentPage = new HashMap<>();
   public static NamespacedKey CUSTOM_BUTTON_ID_KEY;
   public static NamespacedKey HOME_NAME_KEY;

   private final Map<UUID, Component> playerOpenHomeGUITitles = new HashMap<>();

   @Override
   public void onEnable() {
      this.adventure = BukkitAudiences.create(this);
      saveDefaultConfig();
      homeManager = new HomeManager(this);
      homeGUIBuilder = new HomeGUIBuilder(this, homeManager);

      HomeCommands homeCommands = new HomeCommands(this, homeManager);
      getCommand("home").setExecutor(homeCommands);
      getCommand("sethome").setExecutor(homeCommands);
      getCommand("delhome").setExecutor(homeCommands);
      getCommand("homerefresh").setExecutor(homeCommands);

      getCommand("home").setTabCompleter(homeCommands);
      getCommand("sethome").setTabCompleter(homeCommands);
      getCommand("delhome").setTabCompleter(homeCommands);
      getCommand("homerefresh").setTabCompleter(homeCommands);


      Bukkit.getPluginManager().registerEvents(new HomeGUIListener(this, homeManager, playerCurrentPage, playerOpenHomeGUITitles), this);

      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
         new HomeGUIExpansion(this, homeCommands).register();
         getLogger().info("PlaceholderAPI Expansion registered!");
      } else {
         getLogger().warning("PlaceholderAPI not found, HomeGUI placeholders will not work.");
      }

      CUSTOM_BUTTON_ID_KEY = new NamespacedKey(this, "button_id");
      HOME_NAME_KEY = new NamespacedKey(this, "home_name");
      getLogger().info("HomeGUI has been enabled!");
   }

   @Override
   public void onDisable() {
      if (this.adventure != null) {
         this.adventure.close();
         this.adventure = null;
      }
      homeManager.saveHomes();
      getLogger().info("HomeGUI has been disabled!");
   }

   @NotNull
   public BukkitAudiences adventure() {
      if (this.adventure == null) {
         throw new IllegalStateException("Tried to access Adventure API while plugin was disabled!");
      }
      return this.adventure;
   }

   // Remove this method
   // private boolean setupEconomy() {
   //     if (getServer().getPluginManager().getPlugin("Vault") == null) {
   //         return false;
   //     }
   //     RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
   //     if (rsp == null) {
   //         return false;
   //     }
   //     econ = rsp.getProvider();
   //     return econ != null;
   // }

   // Remove this method
   // public static Economy getEconomy() {
   //     return econ;
   // }

   public void openHomeGUI(@NotNull Player player) {
      openHomeGUI(player, 0);
   }

   public void openHomeGUI(@NotNull Player player, int page) {
      playerCurrentPage.put(player.getUniqueId(), page);
      Component guiTitle = MiniMessage.miniMessage().deserialize(
              getConfig().getString("gui.title", "<dark_gray>Homes</dark_gray>")
      );
      playerOpenHomeGUITitles.put(player.getUniqueId(), guiTitle);

      player.openInventory(homeGUIBuilder.buildHomeGUI(player, page));
      player.updateInventory();
   }

   public void teleportToHome(@NotNull Player player, @NotNull Home home) {
      Location location = home.toLocation();
      if (location != null && location.getWorld() != null && location.getWorld().getSeed() == home.getWorldSeed()) {
         player.teleport(location);
         Audience audience = adventure().player(player);
         Component successMessage = MiniMessage.miniMessage().deserialize(
                 getConfig().getString("messages.teleport_success")
                         .replace("<home_name>", home.getName())
         );
         audience.sendMessage(successMessage);
      } else {
         Audience audience = adventure().player(player);
         Component errorMessage = MiniMessage.miniMessage().deserialize(
                 getConfig().getString("messages.teleport_failed")
                         .replace("<home_name>", home.getName())
         );
         audience.sendMessage(errorMessage);
      }
   }

   public HomeManager getHomeManager() {
      return homeManager;
   }

   public HomeGUIBuilder getHomeGUIBuilder() {
      return homeGUIBuilder;
   }

   public Map<UUID, Integer> getPlayerCurrentPage() {
      return playerCurrentPage;
   }

   public Map<UUID, Component> getPlayerOpenHomeGUITitles() {
      return playerOpenHomeGUITitles;
   }

   public void reloadPlugin() {
      reloadConfig();
      homeManager.loadHomes();
      // Remove setupEconomy() call after reload
      // if (!setupEconomy()) {
      //     getLogger().warning("Vault not found or no economy plugin hooked after reload. Slot purchasing might be disabled!");
      // }
      getLogger().info("HomeGUI config reloaded!");
      Bukkit.getOnlinePlayers().forEach(p -> {
         if (playerOpenHomeGUITitles.containsKey(p.getUniqueId())) {
            p.closeInventory();
            Component message = MiniMessage.miniMessage().deserialize(getConfig().getString("messages.gui_reloaded_prompt", "<yellow>Your GUI has been refreshed. Please reopen /home.</yellow>"));
            adventure().player(p).sendMessage(message);
         }
      });
   }

   public int getPlayerMaxHomes(@NotNull Player player) {
      int maxHomes = getConfig().getInt("settings.default_max_homes", 3);

      ConfigurationSection tiersSection = getConfig().getConfigurationSection("settings.max_homes_tiers");
      if (tiersSection != null) {
         for (String key : tiersSection.getKeys(false)) {
            String permission = tiersSection.getString(key + ".permission");
            int limit = tiersSection.getInt(key + ".limit", -1);

            if (permission != null && player.hasPermission(permission)) {
               if (limit == 0) { //Unlimit
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
}