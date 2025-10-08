package me.sanenuyan.homeGUI.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Home {
    private final UUID playerUUID;
    private String name;
    private UUID worldUUID;
    private String worldName;
    private long worldSeed;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private final long creationDate;

    // Constructor chính, bao gồm creationDate
    public Home(@NotNull UUID playerUUID, @NotNull String name, @NotNull UUID worldUUID, @NotNull String worldName, long worldSeed, double x, double y, double z, float yaw, float pitch, long creationDate) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.worldUUID = worldUUID;
        this.worldName = worldName;
        this.worldSeed = worldSeed;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.creationDate = creationDate;
    }

    // Constructor dùng để load từ cấu hình cũ hoặc khi không có world UUID
    public Home(@NotNull UUID playerUUID, @NotNull String name, @NotNull String worldName, double x, double y, double z, float yaw, float pitch, long worldSeed, long creationDate) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.worldSeed = worldSeed;
        this.creationDate = creationDate;

        World world = Bukkit.getWorld(worldName);
        this.worldUUID = (world != null) ? world.getUID() : null;
    }

    // Constructor khi tạo Home từ Location (sẽ đặt creationDate là thời gian hiện tại)
    public Home(@NotNull UUID playerUUID, @NotNull String name, @NotNull Location location) {
        this.playerUUID = playerUUID;
        this.name = name;
        this.worldUUID = location.getWorld() != null ? location.getWorld().getUID() : null;
        this.worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown_world";
        this.worldSeed = location.getWorld() != null ? location.getWorld().getSeed() : 0L;
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
        this.creationDate = System.currentTimeMillis();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getWorldUUID() {
        return worldUUID;
    }

    public String getWorldName() {
        return worldName;
    }

    public long getWorldSeed() {
        return worldSeed;
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

    public long getCreationDate() {
        return creationDate;
    }

    @Nullable
    public Location toLocation() {
        World world = Bukkit.getWorld(worldUUID);
        if (world == null) {
            world = Bukkit.getWorld(worldName);
            if (world == null) {
                Bukkit.getLogger().warning("World for home '" + name + "' (UUID: " + worldUUID + ", Name: " + worldName + ") not found. Returning null location.");
                return null;
            }
        }
        return new Location(world, x, y, z, yaw, pitch);
    }
}