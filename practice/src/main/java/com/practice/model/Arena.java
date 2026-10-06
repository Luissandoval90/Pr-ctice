package com.practice.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Arena de combate con dos spawns y una region protegida.
 * <p>
 * Las regiones se comprueban mediante coordenadas (sin dependencias externas
 * como WorldGuard) para mantener el plugin ligero.
 */
public final class Arena {

    private final String name;
    private String type = "SOLO";
    private String worldName;
    private Location spawn1;
    private Location spawn2;
    private Location spectator;
    private Location corner1;
    private Location corner2;
    private boolean enabled = true;
    private boolean inUse = false;
    private boolean buildAllowed = false;
    private String lastWinner;

    public Arena(String name) {
        this.name = name.toLowerCase(java.util.Locale.ROOT);
    }

    public String name() {
        return this.name;
    }

    public String type() {
        return this.type;
    }

    public void type(String value) {
        this.type = value == null ? "SOLO" : value.toUpperCase(java.util.Locale.ROOT);
    }

    public String worldName() {
        return this.worldName;
    }

    public void worldName(String value) {
        this.worldName = value;
    }

    public World world() {
        return this.worldName == null ? null : Bukkit.getWorld(this.worldName);
    }

    public Location spawn1() {
        return this.spawn1;
    }

    public void spawn1(Location value) {
        this.spawn1 = value;
        if (value != null && this.worldName == null && value.getWorld() != null) {
            this.worldName = value.getWorld().getName();
        }
    }

    public Location spawn2() {
        return this.spawn2;
    }

    public void spawn2(Location value) {
        this.spawn2 = value;
    }

    public Location spectator() {
        return this.spectator;
    }

    public void spectator(Location value) {
        this.spectator = value;
    }

    public Location corner1() {
        return this.corner1;
    }

    public void corner1(Location value) {
        this.corner1 = value;
    }

    public Location corner2() {
        return this.corner2;
    }

    public void corner2(Location value) {
        this.corner2 = value;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public void enabled(boolean value) {
        this.enabled = value;
    }

    public boolean inUse() {
        return this.inUse;
    }

    public void inUse(boolean value) {
        this.inUse = value;
    }

    public boolean buildAllowed() {
        return this.buildAllowed;
    }

    public void buildAllowed(boolean value) {
        this.buildAllowed = value;
    }

    public String lastWinner() {
        return this.lastWinner;
    }

    public void lastWinner(String value) {
        this.lastWinner = value;
    }

    /** Una arena esta lista si tiene los dos spawns y un mundo valido. */
    public boolean ready() {
        return this.worldName != null && this.spawn1 != null && this.spawn2 != null;
    }

    /** Devuelve que le falta a la arena (para mensajes de error). */
    public String missing() {
        if (this.worldName == null) {
            return "mundo";
        }
        if (this.spawn1 == null) {
            return "spawn 1";
        }
        if (this.spawn2 == null) {
            return "spawn 2";
        }
        return null;
    }

    public Location center() {
        if (this.spawn1 == null) {
            return null;
        }
        if (this.spawn2 == null) {
            return this.spawn1.clone();
        }
        return new Location(this.spawn1.getWorld(),
                (this.spawn1.getX() + this.spawn2.getX()) / 2.0D,
                (this.spawn1.getY() + this.spawn2.getY()) / 2.0D,
                (this.spawn1.getZ() + this.spawn2.getZ()) / 2.0D);
    }

    /** Comprueba si una ubicacion esta dentro de la region definida. */
    public boolean contains(Location location) {
        if (location == null || this.corner1 == null || this.corner2 == null) {
            return false;
        }
        if (location.getWorld() == null || !location.getWorld().getName().equals(this.corner1.getWorld() == null ? "" : this.corner1.getWorld().getName())) {
            return false;
        }
        double minX = Math.min(this.corner1.getX(), this.corner2.getX());
        double maxX = Math.max(this.corner1.getX(), this.corner2.getX());
        double minY = Math.min(this.corner1.getY(), this.corner2.getY());
        double maxY = Math.max(this.corner1.getY(), this.corner2.getY());
        double minZ = Math.min(this.corner1.getZ(), this.corner2.getZ());
        double maxZ = Math.max(this.corner1.getZ(), this.corner2.getZ());
        return location.getX() >= minX && location.getX() <= maxX
                && location.getY() >= minY && location.getY() <= maxY
                && location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    /** Comprueba el tipo de partida soportado por esta arena. */
    public boolean supports(MatchType matchType) {
        if (matchType == MatchType.FFA) {
            return this.type.equals("FFA");
        }
        return !this.type.equals("FFA");
    }

    /** Volumen (para informes y estadisticas). */
    public long volume() {
        if (this.corner1 == null || this.corner2 == null) {
            return 0L;
        }
        long dx = (long) Math.abs(this.corner1.getX() - this.corner2.getX()) + 1;
        long dy = (long) Math.abs(this.corner1.getY() - this.corner2.getY()) + 1;
        long dz = (long) Math.abs(this.corner1.getZ() - this.corner2.getZ()) + 1;
        return dx * dy * dz;
    }
}
