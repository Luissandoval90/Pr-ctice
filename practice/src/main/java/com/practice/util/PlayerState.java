package com.practice.util;

import com.practice.model.Kit;
import com.practice.model.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.Map;

/**
 * Operaciones comunes sobre el estado de un jugador.
 * <p>
 * Centraliza la limpieza, la teleportacion y la aplicacion de kits para
 * evitar duplicar logica entre duelos, FFA y lobby.
 */
public final class PlayerState {

    private PlayerState() {
    }

    /** Deja al jugador en un estado limpio y seguro. */
    public static void reset(Player player) {
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.clearActivePotionEffects();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0D);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
        player.setAbsorptionAmount(0.0D);
        player.setLevel(0);
        player.setExp(0.0F);
        player.setTotalExperience(0);
        player.setFallDistance(0.0F);
    }

    /** Limpia el inventario por completo. */
    public static void clearInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[4]);
        inventory.setItemInOffHand(null);
        player.updateInventory();
    }

    /** Estado de lobby: aventura, sin vuelo, limpio. */
    public static void lobby(Player player) {
        reset(player);
        clearInventory(player);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setWalkSpeed(0.2F);
        player.setFlySpeed(0.1F);
        player.setFireTicks(0);
        player.setFireTicks(0);
        player.closeInventory();
    }

    /** Aplica un kit completo al jugador. */
    public static void giveKit(Player player, Kit kit) {
        reset(player);
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        ItemStack[] contents = kit.inventory();
        if (contents != null) {
            for (int slot = 0; slot < contents.length && slot < 36; slot++) {
                ItemStack item = contents[slot];
                if (item != null) {
                    inventory.setItem(slot, item.clone());
                }
            }
        }
        ItemStack[] armor = kit.armor();
        if (armor != null) {
            inventory.setBoots(cloneOrNull(armor, 0));
            inventory.setLeggings(cloneOrNull(armor, 1));
            inventory.setChestplate(cloneOrNull(armor, 2));
            inventory.setHelmet(cloneOrNull(armor, 3));
        }
        if (kit.offHand() != null) {
            inventory.setItemInOffHand(kit.offHand().clone());
        }
        for (Map.Entry<String, Integer> entry : kit.effects().entrySet()) {
            PotionEffectType type = Effects.type(entry.getKey());
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION,
                        Math.max(0, entry.getValue() - 1), false, false, true));
            }
        }
        player.updateInventory();
    }

    private static ItemStack cloneOrNull(ItemStack[] armor, int index) {
        return index < armor.length && armor[index] != null ? armor[index].clone() : null;
    }

    /** Da un item en el slot indicado. */
    public static void give(Player player, int slot, ItemStack item) {
        player.getInventory().setItem(slot, item);
    }

    /** Teleporta mirando hacia una direccion concreta. */
    public static void teleport(Player player, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        player.teleport(location);
        player.setFallDistance(0.0F);
    }

    /** Reproduce un sonido solo si los efectos estan activos. */
    public static void sound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    /** Titulo con tiempos por defecto. */
    public static void title(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(title, subtitle,
                Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
    }

    /** Devuelve la vida del jugador en formato de corazones. */
    public static String healthBar(Player player) {
        double health = player.getHealth();
        int filled = (int) Math.ceil(Math.max(0.0D, health) / 2.0D);
        StringBuilder sb = new StringBuilder("<#E8E8F0>");
        sb.append("❤".repeat(Math.max(0, filled)));
        sb.append("<dark_gray>");
        sb.append("❤".repeat(Math.max(0, 10 - filled)));
        return sb.toString();
    }

    /** Comprueba si un item es del menu del lobby. */
    public static boolean isLobbyItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return item.getType() == Material.COMPASS
                || item.getType() == Material.BOOK
                || item.getType() == Material.NETHER_STAR
                || item.getType() == Material.DIAMOND_SWORD
                || item.getType() == Material.PAPER
                || item.getType() == Material.EMERALD;
    }

    /** Suma tiempo jugado al perfil. */
    public static void trackPlaytime(PlayerProfile profile, long millis) {
        profile.addPlaytime(millis);
    }
}
