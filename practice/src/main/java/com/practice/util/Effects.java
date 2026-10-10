package com.practice.util;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogo de efectos de pocion disponibles en los kits.
 * <p>
 * Se usa un mapa estatico en lugar del registro del servidor para
 * conseguir busquedas O(1) y evitar deprecaciones entre versiones.
 */
public final class Effects {

    private static final Map<String, PotionEffectType> TYPES = new LinkedHashMap<>(48);

    static {
        register("speed", PotionEffectType.SPEED);
        register("slowness", PotionEffectType.SLOWNESS);
        register("haste", PotionEffectType.HASTE);
        register("mining_fatigue", PotionEffectType.MINING_FATIGUE);
        register("strength", PotionEffectType.STRENGTH);
        register("instant_health", PotionEffectType.INSTANT_HEALTH);
        register("instant_damage", PotionEffectType.INSTANT_DAMAGE);
        register("jump_boost", PotionEffectType.JUMP_BOOST);
        register("nausea", PotionEffectType.NAUSEA);
        register("regeneration", PotionEffectType.REGENERATION);
        register("resistance", PotionEffectType.RESISTANCE);
        register("fire_resistance", PotionEffectType.FIRE_RESISTANCE);
        register("water_breathing", PotionEffectType.WATER_BREATHING);
        register("invisibility", PotionEffectType.INVISIBILITY);
        register("blindness", PotionEffectType.BLINDNESS);
        register("night_vision", PotionEffectType.NIGHT_VISION);
        register("hunger", PotionEffectType.HUNGER);
        register("weakness", PotionEffectType.WEAKNESS);
        register("poison", PotionEffectType.POISON);
        register("wither", PotionEffectType.WITHER);
        register("health_boost", PotionEffectType.HEALTH_BOOST);
        register("absorption", PotionEffectType.ABSORPTION);
        register("saturation", PotionEffectType.SATURATION);
        register("glowing", PotionEffectType.GLOWING);
        register("levitation", PotionEffectType.LEVITATION);
        register("slow_falling", PotionEffectType.SLOW_FALLING);
        register("conduit_power", PotionEffectType.CONDUIT_POWER);
        register("dolphins_grace", PotionEffectType.DOLPHINS_GRACE);
        register("bad_omen", PotionEffectType.BAD_OMEN);
        register("hero_of_the_village", PotionEffectType.HERO_OF_THE_VILLAGE);
        register("darkness", PotionEffectType.DARKNESS);
    }

    private Effects() {
    }

    private static void register(String name, PotionEffectType type) {
        TYPES.put(name, type);
    }

    /** Devuelve el tipo de efecto por nombre, o null si no existe. */
    public static PotionEffectType type(String name) {
        if (name == null) {
            return null;
        }
        return TYPES.get(name.toLowerCase(java.util.Locale.ROOT).trim());
    }

    /** Convierte "SPEED:2" en un PotionEffect infinito. */
    public static PotionEffect parse(String spec) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }
        String[] parts = spec.split(":");
        PotionEffectType type = type(parts[0]);
        if (type == null) {
            return null;
        }
        int amplifier = 0;
        if (parts.length > 1) {
            try {
                amplifier = Math.max(0, Integer.parseInt(parts[1].trim()) - 1);
            } catch (NumberFormatException ignored) {
                amplifier = 0;
            }
        }
        return new PotionEffect(type, PotionEffect.INFINITE_DURATION, amplifier, false, false, true);
    }

    /** Aplica un efecto por nombre. */
    public static boolean apply(org.bukkit.entity.LivingEntity entity, String name, int level, int seconds) {
        PotionEffectType type = type(name);
        if (type == null) {
            return false;
        }
        int duration = seconds >= 0 ? seconds * 20 : PotionEffect.INFINITE_DURATION;
        entity.addPotionEffect(new PotionEffect(type, duration, Math.max(0, level - 1), false, false, true));
        return true;
    }

    public static Map<String, PotionEffectType> all() {
        return TYPES;
    }
}
