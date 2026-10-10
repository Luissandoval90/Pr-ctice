package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.model.FfaSession;
import com.practice.model.Kit;
import com.practice.model.MatchType;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import com.practice.util.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gestiona las arenas FFA (combate libre con reaparicion).
 * <p>
 * Optimizacion: las sesiones se guardan en un mapa concurrente y las
 * reapariciones se agendan con un unico delayed task por muerte.
 */
public final class FfaManager {

    private final PracticePlugin plugin;
    private final Map<UUID, FfaSession> sessions = new ConcurrentHashMap<>(128);
    private final Map<String, Integer> population = new ConcurrentHashMap<>(16);

    public FfaManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public FfaSession session(UUID uuid) {
        return this.sessions.get(uuid);
    }

    public boolean inFfa(UUID uuid) {
        return this.sessions.containsKey(uuid);
    }

    public int population(String arena) {
        return this.population.getOrDefault(arena.toLowerCase(java.util.Locale.ROOT), 0);
    }

    public Collection<FfaSession> sessions() {
        return this.sessions.values();
    }

    public int total() {
        return this.sessions.size();
    }

    /** Une a un jugador a la arena FFA indicada. */
    public boolean join(Player player, String arenaName) {
        if (inFfa(player.getUniqueId())) {
            this.plugin.messages().send(player, "ffa.ya-en-ffa");
            return false;
        }
        if (this.plugin.duels().inMatch(player.getUniqueId())) {
            this.plugin.messages().send(player, "generales.en-partida");
            return false;
        }
        Arena arena = this.plugin.arenas().get(arenaName);
        if (arena == null || !arena.type().equals("FFA")) {
            this.plugin.messages().send(player, "ffa.no-existe");
            return false;
        }
        if (!arena.ready()) {
            this.plugin.messages().send(player, "arenas.incompleta", "falta", String.valueOf(arena.missing()));
            return false;
        }
        FfaSession session = new FfaSession(player.getUniqueId(), arena.name());
        this.sessions.put(player.getUniqueId(), session);
        this.population.merge(arena.name(), 1, Integer::sum);

        PlayerProfile profile = this.plugin.profiles().profile(player);
        profile.ffaArena(arena.name());

        Kit kit = this.plugin.kits().get("ffa");
        if (kit == null && !this.plugin.kits().byType(MatchType.FFA).isEmpty()) {
            kit = this.plugin.kits().byType(MatchType.FFA).get(0);
        }
        respawn(player, arena, kit, true);

        this.plugin.messages().send(player, "ffa.unido", "arena", arena.name());
        if (this.plugin.settings().sounds) {
            PlayerState.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6F, 1.4F);
        }
        return true;
    }

    /** Saca a un jugador de la FFA. */
    public void leave(Player player) {
        FfaSession session = this.sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        this.population.merge(session.arena(), -1, (current, change) -> Math.max(0, current + change));
        PlayerProfile profile = this.plugin.profiles().profile(player);
        profile.ffaArena(null);
        this.plugin.messages().send(player, "ffa.salido");
        this.plugin.lobby().sendToLobby(player);
    }

    /** Reaparece al jugador en la arena con el kit FFA. */
    public void respawn(Player player, Arena arena, Kit kit, boolean teleport) {
        PlayerState.reset(player);
        if (teleport) {
            Location location = randomSpawn(arena);
            if (location != null) {
                PlayerState.teleport(player, location);
            }
        }
        if (kit != null) {
            PlayerState.giveKit(player, kit);
        } else {
            PlayerState.clearInventory(player);
        }
        player.setHealth(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0D);
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
    }

    /** Punto de aparicion aleatorio cerca del centro de la arena. */
    public Location randomSpawn(Arena arena) {
        Location center = arena.center();
        if (center == null) {
            return arena.spawn1();
        }
        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0D;
        double distance = ThreadLocalRandom.current().nextDouble() * 6.0D;
        Location location = center.clone().add(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
        location.setY(arena.spawn1() == null ? center.getY() : arena.spawn1().getY());
        location.setYaw((float) (ThreadLocalRandom.current().nextDouble() * 360.0D - 180.0D));
        location.setPitch(0.0F);
        return location;
    }

    /** Registra una muerte dentro de la FFA y agenda la reaparicion. */
    public void handleDeath(Player victim, Player killer) {
        FfaSession session = this.sessions.get(victim.getUniqueId());
        if (session == null) {
            return;
        }
        int lostStreak = session.streak();
        session.registerDeath();

        PlayerProfile victimProfile = this.plugin.profiles().profile(victim);
        victimProfile.addFfaDeath();
        victimProfile.addDeath();

        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            FfaSession killerSession = this.sessions.get(killer.getUniqueId());
            PlayerProfile killerProfile = this.plugin.profiles().profile(killer);
            killerProfile.addKill();
            killerProfile.addFfaKill();
            if (killerSession != null) {
                killerSession.registerKill();
                killerProfile.ffaStreak(killerSession.streak());
                if (killerSession.streak() > killerProfile.ffaBestStreak()) {
                    killerProfile.ffaBestStreak(killerSession.streak());
                }
                this.plugin.messages().send(killer, "ffa.kill", "victima", victim.getName(),
                        "racha", killerSession.streak());
                this.plugin.challenges().onFfaKill(killer);
                killerProfile.addCoins(2);
                if (killerSession.streak() > 0 && killerSession.streak() % 5 == 0) {
                    Bukkit.broadcast(Cc.mm(this.plugin.messages().raw("ffa.kill-streak-anuncio"),
                            "jugador", killer.getName(), "racha", killerSession.streak()));
                }
            }
        }
        if (lostStreak >= 3) {
            this.plugin.messages().send(victim, "ffa.racha-perdida", "racha", lostStreak);
        }
        this.plugin.profiles().markDirty();

        // Reaparicion programada
        int delay = this.plugin.settings().ffaRespawnTicks;
        if (this.plugin.settings().ffaRespawn) {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                if (victim.isOnline()) {
                    victim.spigot().respawn();
                    victim.setGameMode(org.bukkit.GameMode.SPECTATOR);
                }
            });
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (!victim.isOnline()) {
                    this.sessions.remove(victim.getUniqueId());
                    return;
                }
                FfaSession current = this.sessions.get(victim.getUniqueId());
                if (current == null) {
                    return;
                }
                Arena arena = this.plugin.arenas().get(current.arena());
                if (arena == null) {
                    return;
                }
                this.plugin.messages().send(victim, "ffa.reaparecer");
                respawn(victim, arena, resolveKit(), true);
            }, delay);
        }
    }

    public Kit resolveKit() {
        Kit kit = this.plugin.kits().get("ffa");
        if (kit == null) {
            List<Kit> kits = this.plugin.kits().byType(MatchType.FFA);
            kit = kits.isEmpty() ? null : kits.get(0);
        }
        return kit;
    }

    /** Arenas FFA disponibles. */
    public List<Arena> arenas() {
        List<Arena> list = new ArrayList<>();
        for (Arena arena : this.plugin.arenas().all()) {
            if (arena.type().equals("FFA") && arena.ready()) {
                list.add(arena);
            }
        }
        return list;
    }

    public void clear() {
        this.sessions.clear();
        this.population.clear();
    }
}
