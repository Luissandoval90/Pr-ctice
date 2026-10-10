package com.practice.listener;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.model.DuelMatch;
import com.practice.model.FfaSession;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import com.practice.util.PlayerState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Ciclo de vida de los jugadores: entrada, salida, muerte y reaparicion.
 */
public final class PlayerListener implements Listener {

    private final PracticePlugin plugin;

    public PlayerListener(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        event.joinMessage(null);
        this.plugin.profiles().profile(player);
        this.plugin.profiles().startSession(player.getUniqueId());
        this.plugin.tab().updateName(player);
        this.plugin.board().scoreboard(player);
        if (this.plugin.settings().nametags) {
            this.plugin.tab().updateNametag(player);
        }
        if (this.plugin.settings().teleportOnJoin) {
            this.plugin.lobby().sendToLobby(player);
        }
        this.plugin.lobby().welcome(player);
        this.plugin.friends().announceJoin(player);
        this.plugin.challenges().ensure(this.plugin.profiles().profile(player));
        this.plugin.board().update(player);
        if (this.plugin.settings().welcomeMessage) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (player.isOnline()) {
                    this.plugin.tab().updateName(player);
                }
            }, 20L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.quitMessage(null);
        this.plugin.duels().leave(player, true);
        this.plugin.queue().leave(player);
        FfaSession session = this.plugin.ffa().session(player.getUniqueId());
        if (session != null) {
            this.plugin.ffa().leave(player);
        }
        PlayerProfile profile = this.plugin.profiles().profile(player);
        if (profile != null) {
            profile.spectatingId(null);
        }
        this.plugin.profiles().endSession(player.getUniqueId());
        this.plugin.profiles().saveAsync();
        this.plugin.tab().remove(player);
        this.plugin.board().remove(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DuelMatch match = this.plugin.duels().match(player.getUniqueId());
        if (match != null) {
            event.deathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepInventory(true);
            Player killer = player.getKiller();
            this.plugin.duels().handleDeath(player, killer);
            return;
        }
        FfaSession session = this.plugin.ffa().session(player.getUniqueId());
        if (session != null) {
            event.deathMessage(null);
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepInventory(true);
            this.plugin.ffa().handleDeath(player, player.getKiller());
            return;
        }
        // Muerte en el lobby (no deberia ocurrir)
        event.deathMessage(null);
        event.getDrops().clear();
        event.setKeepInventory(true);
        Bukkit.getScheduler().runTask(this.plugin, () -> this.plugin.lobby().sendToLobby(player));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        DuelMatch match = this.plugin.duels().match(player.getUniqueId());
        if (match != null) {
            Arena arena = this.plugin.arenas().get(match.arenaName());
            if (arena != null && arena.spectator() != null) {
                event.setRespawnLocation(arena.spectator());
            }
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            });
            return;
        }
        FfaSession session = this.plugin.ffa().session(player.getUniqueId());
        if (session != null) {
            Arena arena = this.plugin.arenas().get(session.arena());
            if (arena != null) {
                event.setRespawnLocation(this.plugin.ffa().randomSpawn(arena));
            }
            return;
        }
        event.setRespawnLocation(this.plugin.lobby().spawn());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        Player player = event.getPlayer();
        this.plugin.duels().leave(player, true);
        this.plugin.queue().leave(player);
        this.plugin.tournaments().eliminate(player.getUniqueId());
    }

    /** Anuncio de entrada con el diseno pastel del servidor. */
    @EventHandler
    public void onJoinAnnounce(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.settings().welcomeMessage) {
            return;
        }
        PlayerProfile profile = this.plugin.profiles().profile(player);
        Component message = Cc.mm("<dark_gray>[<#C1E1C1>+</#C1E1C1>] <#E8E8F0>%jugador% <#BFC4CC>se ha conectado "
                + "<dark_gray>(<#FFF5BA>%elo% ELO<dark_gray>)",
                "jugador", player.getName(), "elo", profile.elo());
        Bukkit.broadcast(message);
        PlayerState.sound(player, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.6F);
    }
}
