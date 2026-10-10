package com.practice.listener;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.model.DuelMatch;
import com.practice.model.GameState;
import com.practice.model.Kit;
import com.practice.util.PlayerState;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Proteccion del lobby, reglas de construccion y reglas de combate.
 */
public final class ProtectionListener implements Listener {

    private final PracticePlugin plugin;

    public ProtectionListener(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Construccion y bloques
    // ------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("practice.admin")) {
            return;
        }
        DuelMatch match = this.plugin.duels().match(player.getUniqueId());
        if (match != null) {
            Kit kit = this.plugin.kits().get(match.kitName());
            Arena arena = this.plugin.arenas().get(match.arenaName());
            boolean allowed = (kit != null && kit.blockBreaking()) || (arena != null && arena.buildAllowed());
            if (!allowed) {
                event.setCancelled(true);
            } else {
                this.plugin.rollback().record(match.id(), event.getBlock());
            }
            return;
        }
        if (this.plugin.ffa().inFfa(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (this.plugin.settings().lobbyProtection && !player.hasPermission("practice.staff")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("practice.admin")) {
            return;
        }
        DuelMatch match = this.plugin.duels().match(player.getUniqueId());
        if (match != null) {
            Kit kit = this.plugin.kits().get(match.kitName());
            Arena arena = this.plugin.arenas().get(match.arenaName());
            boolean allowed = (kit != null && kit.blockPlacing()) || (arena != null && arena.buildAllowed());
            if (!allowed) {
                event.setCancelled(true);
            } else if (arena != null && !arena.contains(event.getBlock().getLocation())) {
                // Permitir solo dentro de la region de la arena
                event.setCancelled(true);
            } else if (event.getBlockReplacedState() != null) {
                this.plugin.rollback().record(match.id(), event.getBlock(), event.getBlockReplacedState().getBlockData());
            }
            return;
        }
        if (this.plugin.ffa().inFfa(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (this.plugin.settings().lobbyProtection && !player.hasPermission("practice.staff")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("practice.staff") && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (this.plugin.duels().inMatch(player.getUniqueId())) {
            return;
        }
        if (this.plugin.settings().lobbyProtection) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (this.plugin.duels().inMatch(event.getPlayer().getUniqueId())) {
            return;
        }
        if (this.plugin.settings().lobbyProtection) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().clear();
    }

    // ------------------------------------------------------------------
    //  Danio
    // ------------------------------------------------------------------
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            return;
        }
        DuelMatch match = this.plugin.duels().match(player.getUniqueId());
        if (match != null) {
            if (match.state() == GameState.COUNTDOWN || match.state() == GameState.ROUND_END
                    || match.state() == GameState.ENDING) {
                event.setCancelled(true);
                return;
            }
            Kit kit = this.plugin.kits().get(match.kitName());
            if (kit != null) {
                if (!kit.fallDamage() && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (this.plugin.ffa().inFfa(player.getUniqueId())) {
            return;
        }
        if (this.plugin.settings().lobbyProtection && !player.hasPermission("practice.staff")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = null;
        if (event.getDamager() instanceof Player direct) {
            attacker = direct;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        DuelMatch match = this.plugin.duels().match(victim.getUniqueId());
        if (match != null) {
            if (attacker == null) {
                return;
            }
            if (match.teamOf(victim.getUniqueId()) == match.teamOf(attacker.getUniqueId())) {
                // Fuego amigo desactivado
                event.setCancelled(true);
                return;
            }
            if (match.state() != GameState.FIGHTING) {
                event.setCancelled(true);
            }
            return;
        }
        if (this.plugin.ffa().inFfa(victim.getUniqueId())) {
            return;
        }
        if (this.plugin.settings().lobbyProtection && !victim.hasPermission("practice.staff")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (this.plugin.duels().inMatch(player.getUniqueId()) || this.plugin.ffa().inFfa(player.getUniqueId())) {
            return;
        }
        if (this.plugin.settings().lobbyProtection) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.duels().inMatch(player.getUniqueId()) || this.plugin.ffa().inFfa(player.getUniqueId())) {
            return;
        }
        if (this.plugin.settings().lobbyProtection) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------
    //  Interacciones
    // ------------------------------------------------------------------
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.duels().inMatch(player.getUniqueId()) || this.plugin.ffa().inFfa(player.getUniqueId())) {
            return;
        }
        if (this.plugin.profiles().profile(player).spectating()) {
            event.setCancelled(true);
            ItemStack held = event.getItem();
            if (held != null && held.getType() == Material.COMPASS) {
                this.plugin.duels().cycleSpectator(player);
            } else if (held != null && held.getType() == Material.BARRIER) {
                this.plugin.duels().stopSpectating(player);
            }
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (!PlayerState.isLobbyItem(item)) {
                    return;
                }
                event.setCancelled(true);
                if (item.getType() == Material.COMPASS) {
                    this.plugin.menus().openMain(player);
                } else if (item.getType() == Material.BOOK) {
                    this.plugin.menus().openStats(player, player);
                } else if (item.getType() == Material.NETHER_STAR) {
                    this.plugin.menus().openParty(player);
                } else if (item.getType() == Material.DIAMOND_SWORD) {
                    this.plugin.menus().openFfa(player);
                } else if (item.getType() == Material.PAPER) {
                    this.plugin.menus().openKits(player);
                } else if (item.getType() == Material.EMERALD) {
                    this.plugin.menus().openTop(player, com.practice.model.Leaderboard.Type.ELO);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (this.plugin.duels().inMatch(player.getUniqueId()) || this.plugin.ffa().inFfa(player.getUniqueId())) {
            return;
        }
        if (event.getInventory().getHolder() instanceof com.practice.hud.MenuHolder) {
            return;
        }
        if (this.plugin.settings().lobbyProtection && !player.hasPermission("practice.staff")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item.getType() != Material.MUSHROOM_STEW) {
            return;
        }
        DuelMatch match = this.plugin.duels().match(player.getUniqueId());
        if (match == null) {
            return;
        }
        // Las sopas curan 3.5 corazones y se conservan para poder usarlas de nuevo
        event.setCancelled(true);
        double max = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0D;
        player.setHealth(Math.min(max, player.getHealth() + 7.0D));
        ItemStack hand = event.getItem();
        if (hand.getAmount() <= 1) {
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(null);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
        if (this.plugin.settings().sounds) {
            PlayerState.sound(player, org.bukkit.Sound.ENTITY_GENERIC_EAT, 0.7F, 1.2F);
        }
    }

    // ------------------------------------------------------------------
    //  Clima, tiempo y vacio
    // ------------------------------------------------------------------
    @EventHandler
    public void onWeather(WeatherChangeEvent event) {
        if (this.plugin.settings().clearWeather && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTimeSkip(TimeSkipEvent event) {
        if (this.plugin.settings().clearWeather && event.getSkipReason() == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!this.plugin.settings().antiVoid) {
            return;
        }
        Location to = event.getTo();
        if (to == null || to.getY() > this.plugin.settings().voidY) {
            return;
        }
        Player player = event.getPlayer();
        if (this.plugin.duels().inMatch(player.getUniqueId())) {
            DuelMatch match = this.plugin.duels().match(player.getUniqueId());
            Arena arena = match == null ? null : this.plugin.arenas().get(match.arenaName());
            if (arena != null) {
                player.teleport(match.teamOf(player.getUniqueId()) == 0 ? arena.spawn1() : arena.spawn2());
                this.plugin.messages().send(player, "proteccion.void");
            }
            return;
        }
        if (this.plugin.ffa().inFfa(player.getUniqueId())) {
            var session = this.plugin.ffa().session(player.getUniqueId());
            Arena arena = session == null ? null : this.plugin.arenas().get(session.arena());
            if (arena != null) {
                player.teleport(this.plugin.ffa().randomSpawn(arena));
            }
            return;
        }
        if (this.plugin.lobby().isLobbyWorld(player)) {
            player.teleport(this.plugin.lobby().spawn());
            this.plugin.messages().send(player, "proteccion.void");
        }
    }
}
