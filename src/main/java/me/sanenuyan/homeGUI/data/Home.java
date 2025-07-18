package me.sanenuyan.homeGUI.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.UUID;

public class Home {
    private final UUID ownerUUID;
    private final String name;
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;
    private final long worldSeed;

    public Home(UUID ownerUUID, String name, Location location) {
        this.ownerUUID = ownerUUID;
        this.name = name;
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.worldSeed = location.getWorld().getSeed();
    }

    // Constructor for loading from data
    public Home(UUID ownerUUID, String name, String worldName, double x, double y, double z, float yaw, float pitch, long worldSeed) {
        this.ownerUUID = ownerUUID;
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.worldSeed = worldSeed;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            // World might not be loaded or exists
            return null;
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}