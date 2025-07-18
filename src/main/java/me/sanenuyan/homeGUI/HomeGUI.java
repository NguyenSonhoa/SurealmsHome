package me.sanenuyan.homeGUI;

import me.sanenuyan.homeGUI.commands.HomeCommands;
import me.sanenuyan.homeGUI.data.Home;
import me.sanenuyan.homeGUI.data.HomeManager;
import me.sanenuyan.homeGUI.gui.HomeGUIBuilder;
import me.sanenuyan.homeGUI.listeners.HomeGUIListener;
import me.sanenuyan.homeGUI.placeholder.HomeGUIExpansion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HomeGUI extends JavaPlugin {
   private BukkitAudiences adventure;
   private HomeManager homeManager;
   private HomeGUIBuilder guiBuilder;
   private HomeCommands homeCommands;
   private HomeGUIListener homeGUIListener;
   private final Map<UUID, Integer> playerCurrentPage = new ConcurrentHashMap<>();

   @Override
   public void onEnable() {
      this.saveDefaultConfig();
      this.adventure = BukkitAudiences.create(this);
      this.homeManager = new HomeManager(this);
      this.guiBuilder = new HomeGUIBuilder(this, homeManager);
      this.homeCommands = new HomeCommands(this, homeManager);
      this.homeGUIListener = new HomeGUIListener(this, homeManager, playerCurrentPage);

      Bukkit.getPluginManager().registerEvents(homeGUIListener, this);

      this.getCommand("home").setExecutor(homeCommands);
      this.getCommand("home").setTabCompleter(homeCommands);
      this.getCommand("homerefresh").setExecutor(homeCommands);
      this.getCommand("sethome").setExecutor(homeCommands);
      this.getCommand("delhome").setExecutor(homeCommands);

      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
         new HomeGUIExpansion(this, homeCommands).register();
         getLogger().info("PlaceholderAPI expansion registered!");
      } else {
         getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
      }
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

   public BukkitAudiences adventure() {
      return this.adventure;
   }

   // GUI
   public void openHomeGUI(@NotNull Player player) {
      openHomeGUI(player, playerCurrentPage.getOrDefault(player.getUniqueId(), 0));
   }

   public void openHomeGUI(@NotNull Player player, int page) {
      List<Home> allHomes = homeManager.getPlayerHomes(player.getUniqueId());
      List<Integer> homeSlots = getConfig().getIntegerList("gui.home-item-slots");
      int homesPerPage = homeSlots.isEmpty() ? 18 : homeSlots.size(); // Default if not specified

      int totalPages = (int) Math.ceil((double) allHomes.size() / homesPerPage);
      if (totalPages == 0) totalPages = 1;

      if (page < 0) page = 0;
      if (page >= totalPages) page = totalPages - 1;
      playerCurrentPage.put(player.getUniqueId(), page);

      int startIndex = page * homesPerPage;
      List<Home> homesForPage = allHomes.stream()
              .skip(startIndex)
              .limit(homesPerPage)
              .collect(ArrayList::new, ArrayList::add, ArrayList::addAll); //
      player.openInventory(guiBuilder.buildHomeGUI(player, page, totalPages, homesForPage));
   }
   public void teleportToHome(@NotNull Player player, @NotNull Home home) {
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

   public HomeManager getHomeManager() {
      return homeManager;
   }
}
