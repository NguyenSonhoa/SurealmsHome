package me.sanenuyan.homeGUI.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class Home implements ConfigurationSerializable {
    private final UUID playerUuid;
    private final String name;
    private final String worldName;
    private final UUID worldUuid;
    private final long worldSeed;
    private final double x, y, z;
    private final float yaw, pitch;

    public Home(UUID playerUuid, String name, Location location) {
        this.playerUuid = playerUuid;
        this.name = name;
        this.worldName = location.getWorld().getName();
        this.worldUuid = location.getWorld().getUID();
        this.worldSeed = location.getWorld().getSeed();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public Home(Map<String, Object> map) {
        this.playerUuid = UUID.fromString((String) map.get("playerUuid"));
        this.name = (String) map.get("name");
        this.worldName = (String) map.get("worldName");
        this.worldUuid = UUID.fromString((String) map.get("worldUuid"));
        this.worldSeed = (long) map.get("worldSeed");
        this.x = (double) map.get("x");
        this.y = (double) map.get("y");
        this.z = (double) map.get("z");
        this.yaw = ((Double) map.get("yaw")).floatValue();
        this.pitch = ((Double) map.get("pitch")).floatValue();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("playerUuid", playerUuid.toString());
        map.put("name", name);
        map.put("worldName", worldName);
        map.put("worldUuid", worldUuid.toString());
        map.put("worldSeed", worldSeed);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", yaw);
        map.put("pitch", pitch);
        return map;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public UUID getWorldUuid() {
        return worldUuid;
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldUuid);
        if (world == null) {
            world = Bukkit.getWorld(worldName);
        }
        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}