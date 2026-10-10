package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.model.MatchType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gestiona las arenas de duelo y FFA.
 * <p>
 * Incluye balanceo de carga: al elegir arena se prioriza la que lleve mas
 * tiempo sin usarse, evitando reutilizar siempre la misma.
 */
public final class ArenaManager {

    private final PracticePlugin plugin;
    private final Map<String, Arena> arenas = new LinkedHashMap<>(64);
    private final Map<String, Long> lastUsed = new LinkedHashMap<>(64);
    private final File file;

    public ArenaManager(PracticePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void load() {
        this.arenas.clear();
        if (!this.file.exists()) {
            this.save();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Arena arena = new Arena(key);
            arena.type(section.getString("tipo", "SOLO"));
            arena.worldName(section.getString("mundo"));
            arena.spawn1(section.getSerializable("spawn1", Location.class));
            arena.spawn2(section.getSerializable("spawn2", Location.class));
            arena.spectator(section.getSerializable("espectador", Location.class));
            arena.corner1(section.getSerializable("pos1", Location.class));
            arena.corner2(section.getSerializable("pos2", Location.class));
            arena.enabled(section.getBoolean("activa", true));
            arena.buildAllowed(section.getBoolean("construccion", false));
            this.arenas.put(arena.name(), arena);
        }
        this.plugin.getLogger().info("Cargadas " + this.arenas.size() + " arenas.");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Arena arena : this.arenas.values()) {
            String path = "arenas." + arena.name() + ".";
            config.set(path + "tipo", arena.type());
            config.set(path + "mundo", arena.worldName());
            config.set(path + "spawn1", arena.spawn1());
            config.set(path + "spawn2", arena.spawn2());
            config.set(path + "espectador", arena.spectator());
            config.set(path + "pos1", arena.corner1());
            config.set(path + "pos2", arena.corner2());
            config.set(path + "activa", arena.enabled());
            config.set(path + "construccion", arena.buildAllowed());
        }
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.plugin.getLogger().warning("No se pudo guardar arenas.yml: " + exception.getMessage());
        }
    }

    public Arena get(String name) {
        return name == null ? null : this.arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return get(name) != null;
    }

    public Arena create(String name) {
        Arena arena = new Arena(name);
        this.arenas.put(arena.name(), arena);
        return arena;
    }

    public void delete(String name) {
        this.arenas.remove(name.toLowerCase(Locale.ROOT));
        this.lastUsed.remove(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Arena> all() {
        return this.arenas.values();
    }

    public int count() {
        return this.arenas.size();
    }

    /** Arenas listas y libres para un tipo de partida. */
    public List<Arena> available(MatchType type) {
        List<Arena> list = new ArrayList<>();
        for (Arena arena : this.arenas.values()) {
            if (!arena.inUse() && arena.enabled() && arena.ready() && arena.supports(type)) {
                list.add(arena);
            }
        }
        return list;
    }

    /** Elige la arena menos usada recientemente (balanceo de carga). */
    public Arena pick(MatchType type) {
        List<Arena> candidates = available(type);
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort(Comparator.comparingLong(arena -> this.lastUsed.getOrDefault(arena.name(), 0L)));
        // entre las 3 menos usadas, elegir una al azar para variar
        int window = Math.min(3, candidates.size());
        Arena chosen = candidates.get(window == 1 ? 0 : ThreadLocalRandom.current().nextInt(window));
        this.lastUsed.put(chosen.name(), System.currentTimeMillis());
        return chosen;
    }

    public void release(Arena arena) {
        if (arena != null) {
            arena.inUse(false);
            this.lastUsed.put(arena.name(), System.currentTimeMillis());
        }
    }

    /** Crea (si no existe) una arena jugable en el mundo indicado. */
    public Arena createFallback(World world) {
        Arena existing = get("baltia_default");
        if (existing != null && existing.ready()) {
            return existing;
        }
        Location spawn = world.getSpawnLocation();
        int baseY = Math.max(world.getHighestBlockYAt(spawn) + 30, 80) + (existing == null ? 0 : 1);
        int centerX = spawn.getBlockX() + 40;
        int centerZ = spawn.getBlockZ() + 40;
        Location center = new Location(world, centerX + 0.5D, baseY + 1.0D, centerZ + 0.5D);

        // Plataforma pastel 41x41 con muro de cristal
        int radius = 20;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Block floor = world.getBlockAt(centerX + x, baseY, centerZ + z);
                boolean border = Math.abs(x) == radius || Math.abs(z) == radius;
                floor.setType(border ? Material.LIGHT_BLUE_STAINED_GLASS : Material.SMOOTH_QUARTZ, false);
            }
        }
        for (int y = 1; y <= 3; y++) {
            for (int x = -radius; x <= radius; x++) {
                world.getBlockAt(centerX + x, baseY + y, centerZ + radius).setType(Material.PINK_STAINED_GLASS, false);
                world.getBlockAt(centerX + x, baseY + y, centerZ - radius).setType(Material.PINK_STAINED_GLASS, false);
                world.getBlockAt(centerX + radius, baseY + y, centerZ + x).setType(Material.PINK_STAINED_GLASS, false);
                world.getBlockAt(centerX - radius, baseY + y, centerZ + x).setType(Material.PINK_STAINED_GLASS, false);
            }
        }

        Arena arena = existing == null ? create("baltia_default") : existing;
        arena.type("SOLO");
        arena.spawn1(new Location(world, centerX - 12 + 0.5D, baseY + 1.0D, centerZ + 0.5D, 90.0F, 0.0F));
        arena.spawn2(new Location(world, centerX + 12 + 0.5D, baseY + 1.0D, centerZ + 0.5D, -90.0F, 0.0F));
        arena.spectator(new Location(world, centerX + 0.5D, baseY + 12.0D, centerZ + 0.5D, 0.0F, 45.0F));
        arena.corner1(new Location(world, centerX - radius, baseY - 1, centerZ - radius));
        arena.corner2(new Location(world, centerX + radius, baseY + 4, centerZ + radius));
        arena.enabled(true);
        arena.buildAllowed(false);
        save();
        return arena;
    }

    /** Crea una arena FFA por defecto si no hay ninguna. */
    public Arena createFfaFallback(World world) {
        Arena existing = get("baltia_ffa");
        if (existing != null && existing.ready()) {
            return existing;
        }
        Location spawn = world.getSpawnLocation();
        int baseY = Math.max(world.getHighestBlockYAt(spawn) + 40, 90) + (existing == null ? 0 : 1);
        int centerX = spawn.getBlockX() - 60;
        int centerZ = spawn.getBlockZ() - 60;
        int radius = 35;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt((double) (x * x + z * z));
                if (distance > radius) {
                    continue;
                }
                boolean ring = distance > radius - 1.5D;
                Block block = world.getBlockAt(centerX + x, baseY, centerZ + z);
                if (ring) {
                    block.setType(Material.LIGHT_BLUE_STAINED_GLASS, false);
                } else if (distance < 6.0D) {
                    block.setType(Material.QUARTZ_BLOCK, false);
                } else {
                    block.setType(Material.SMOOTH_QUARTZ, false);
                }
            }
        }
        Arena arena = existing == null ? create("baltia_ffa") : existing;
        arena.type("FFA");
        arena.spawn1(new Location(world, centerX + 0.5D, baseY + 1.0D, centerZ + 0.5D, 0.0F, 0.0F));
        arena.spawn2(new Location(world, centerX + 8.5D, baseY + 1.0D, centerZ + 8.5D, 0.0F, 0.0F));
        arena.spectator(new Location(world, centerX + 0.5D, baseY + 15.0D, centerZ + 0.5D, 0.0F, 60.0F));
        arena.corner1(new Location(world, centerX - radius, baseY - 2, centerZ - radius));
        arena.corner2(new Location(world, centerX + radius, baseY + 6, centerZ + radius));
        arena.enabled(true);
        save();
        return arena;
    }

    public Location lobbySpawn() {
        String world = this.plugin.settings().lobbyWorld;
        World target = world == null || world.isEmpty() ? Bukkit.getWorlds().get(0) : Bukkit.getWorld(world);
        if (target == null) {
            target = Bukkit.getWorlds().get(0);
        }
        return target.getSpawnLocation();
    }
}
