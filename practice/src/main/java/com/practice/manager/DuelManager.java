package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.model.DuelMatch;
import com.practice.model.DuelRequest;
import com.practice.model.GameState;
import com.practice.model.Kit;
import com.practice.model.MatchType;
import com.practice.model.Party;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import com.practice.util.PlayerState;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nucleo del sistema de duelos.
 * <p>
 * Se encarga de las peticiones, el ciclo de vida de las partidas (cuenta
 * atras, rondas, muerte subita y resumen) y de la aplicacion de ELO.
 * <p>
 * Optimizacion: el estado de las partidas se avanza desde un unico task
 * periodico en lugar de crear tareas por ronda.
 */
public final class DuelManager {

    private final PracticePlugin plugin;
    private final Map<String, DuelMatch> matches = new LinkedHashMap<>(64);
    private final Map<UUID, String> byPlayer = new LinkedHashMap<>(128);
    private final Map<UUID, DuelRequest> pending = new LinkedHashMap<>(32);
    private final Map<String, Long> roundEndsAt = new LinkedHashMap<>(32);
    private final Map<String, Long> finishAt = new LinkedHashMap<>(32);

    public DuelManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Peticiones de duelo
    // ------------------------------------------------------------------
    public DuelRequest pending(UUID target) {
        return this.pending.get(target);
    }

    public DuelRequest sent(UUID challenger) {
        for (DuelRequest request : this.pending.values()) {
            if (request.challenger().equals(challenger)) {
                return request;
            }
        }
        return null;
    }

    /** Envia una peticion de duelo. Devuelve el motivo de error o null si fue bien. */
    public String request(Player challenger, Player target, Kit kit, int rounds) {
        if (challenger.getUniqueId().equals(target.getUniqueId())) {
            return "duelos.no-propio-duelo";
        }
        if (inMatch(challenger.getUniqueId()) || inMatch(target.getUniqueId())) {
            return "duelos.ya-en-partida";
        }
        if (this.plugin.ffa().inFfa(challenger.getUniqueId()) || this.plugin.ffa().inFfa(target.getUniqueId())) {
            return "duelos.ya-en-partida";
        }
        Party challengerParty = this.plugin.parties().of(challenger.getUniqueId());
        Party targetParty = this.plugin.parties().of(target.getUniqueId());
        if (!this.plugin.settings().allowPartyInternal && challengerParty != null && challengerParty == targetParty) {
            return "duelos.no-mismo-equipo";
        }
        if (kit == null) {
            return "kits.no-existe";
        }
        if (this.plugin.settings().eloRange > 0 && this.plugin.settings().eloEnabled) {
            PlayerProfile first = this.plugin.profiles().profile(challenger);
            PlayerProfile second = this.plugin.profiles().profile(target);
            int difference = Math.abs(first.elo() - second.elo());
            if (difference > this.plugin.settings().eloRange) {
                this.plugin.messages().send(challenger, "duelos.rango-elo", "diferencia", difference);
                return null;
            }
        }
        DuelRequest request = new DuelRequest(challenger.getUniqueId(), target.getUniqueId(),
                kit.name(), rounds, this.plugin.settings().requestExpire);
        this.pending.put(target.getUniqueId(), request);
        this.plugin.messages().send(challenger, "duelos.peticion-enviada", "objetivo", target.getName(), "kit", kit.name());
        this.plugin.messages().sendList(target, "duelos.peticion-recibida",
                "retador", challenger.getName(), "kit", kit.name());
        if (this.plugin.settings().sounds) {
            PlayerState.sound(target, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.6F);
        }
        return null;
    }

    /** Acepta la peticion pendiente del jugador. */
    public void accept(Player target) {
        DuelRequest request = this.pending.remove(target.getUniqueId());
        if (request == null || request.expired()) {
            this.plugin.messages().send(target, "duelos.sin-peticion");
            return;
        }
        Player challenger = Bukkit.getPlayer(request.challenger());
        if (challenger == null) {
            this.plugin.messages().send(target, "generales.jugador-desconocido");
            return;
        }
        if (inMatch(challenger.getUniqueId()) || inMatch(target.getUniqueId())) {
            this.plugin.messages().send(target, "duelos.ya-en-partida");
            return;
        }
        Kit kit = this.plugin.kits().get(request.kit());
        if (kit == null) {
            this.plugin.messages().send(target, "kits.no-existe", "kit", request.kit());
            return;
        }
        this.plugin.messages().send(target, "duelos.aceptado", "retador", challenger.getName());
        this.plugin.messages().send(challenger, "duelos.aceptado-retador", "objetivo", target.getName());

        List<List<UUID>> teams = new ArrayList<>(2);
        List<UUID> first = List.of(challenger.getUniqueId());
        List<UUID> second = List.of(target.getUniqueId());
        teams.add(first);
        teams.add(second);
        startMatch(MatchType.SOLO, kit, teams, request.rounds());
    }

    /** Rechaza la peticion pendiente. */
    public void deny(Player target) {
        DuelRequest request = this.pending.remove(target.getUniqueId());
        if (request == null) {
            this.plugin.messages().send(target, "duelos.sin-peticion");
            return;
        }
        this.plugin.messages().send(target, "duelos.rechazado");
        Player challenger = Bukkit.getPlayer(request.challenger());
        if (challenger != null) {
            this.plugin.messages().send(challenger, "duelos.rechazado-retador", "objetivo", target.getName());
        }
    }

    // ------------------------------------------------------------------
    //  Ciclo de vida de las partidas
    // ------------------------------------------------------------------
    public boolean startMatch(MatchType type, Kit kit, List<List<UUID>> teams, int rounds) {
        if (teams.size() < 2) {
            return false;
        }
        Arena arena = this.plugin.arenas().pick(type);
        if (arena == null) {
            Component message = this.plugin.messages().get("duelos.no-arena-tipo", "tipo", type.display());
            for (List<UUID> team : teams) {
                for (UUID uuid : team) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(message);
                    }
                }
            }
            return false;
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        int totalRounds = rounds <= 0 ? this.plugin.settings().defaultRounds : rounds;
        DuelMatch match = new DuelMatch(id, type, kit.name(), arena.name(), totalRounds);
        for (List<UUID> team : teams) {
            match.addTeam(team);
        }
        arena.inUse(true);
        this.matches.put(id, match);
        for (UUID uuid : match.allPlayers()) {
            this.byPlayer.put(uuid, id);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                PlayerProfile profile = this.plugin.profiles().profile(player);
                profile.matchId(id);
            }
        }
        startRound(match);
        return true;
    }

    /** Comienza la ronda actual (teleport + kit + cuenta atras). */
    private void startRound(DuelMatch match) {
        Arena arena = this.plugin.arenas().get(match.arenaName());
        Kit kit = this.plugin.kits().get(match.kitName());
        if (arena == null || kit == null) {
            endMatch(match, -2, "error");
            return;
        }
        match.state(GameState.COUNTDOWN);
        match.countdownEndsAt(System.currentTimeMillis() + Math.max(1, this.plugin.settings().countdown) * 1000L);
        match.alive().clear();
        match.alive().addAll(match.allPlayers());

        int[] spawnIndex = {0, 0};
        for (int teamIndex = 0; teamIndex < match.teams().size(); teamIndex++) {
            List<UUID> team = match.team(teamIndex);
            for (UUID uuid : team) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                Location location = teamIndex == 0
                        ? offset(arena.spawn1(), spawnIndex[0]++)
                        : offset(arena.spawn2(), spawnIndex[1]++);
                PlayerState.lobby(player);
                this.plugin.lobby().sendToMatch(player, kit);
                PlayerState.teleport(player, location);
                player.setInvulnerable(true);
                player.setWalkSpeed(0.0F);
                player.setFlySpeed(0.0F);
                if (this.plugin.settings().countdown > 0) {
                    sendCountdownTitles(player, match, kit);
                }
            }
        }
        this.plugin.profiles().markDirty();
    }

    private Location offset(Location base, int index) {
        if (base == null) {
            return null;
        }
        if (index == 0) {
            return base.clone();
        }
        double angle = index * 0.6D;
        Location location = base.clone().add(Math.cos(angle) * 1.5D * index, 0.0D, Math.sin(angle) * 1.5D * index);
        location.setYaw(base.getYaw());
        return location;
    }

    private void sendCountdownTitles(Player player, DuelMatch match, Kit kit) {
        UUID rival = match.firstOpponent(player.getUniqueId());
        String rivalName = rival == null ? "?" : nameOf(rival);
        PlayerState.title(player,
                Cc.mm(this.plugin.messages().raw("duelos.cuenta-atras"), "tiempo", this.plugin.settings().countdown),
                Cc.mm("<#E8E8F0>" + kit.name() + " <dark_gray>| <#BFC4CC>vs <#FFD1DC>" + rivalName),
                0, 30, 10);
        if (this.plugin.settings().sounds) {
            PlayerState.sound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.8F, 1.4F);
        }
    }

    /** Empieza la fase de combate de la ronda. */
    private void beginFight(DuelMatch match) {
        match.state(GameState.FIGHTING);
        match.roundStartedAt(System.currentTimeMillis());
        for (UUID uuid : match.allPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.setInvulnerable(false);
            player.setWalkSpeed(0.2F);
            player.setFlySpeed(0.1F);
            if (this.plugin.settings().titles) {
                UUID rival = match.firstOpponent(uuid);
                PlayerState.title(player,
                        Cc.mm(this.plugin.messages().raw("duelos.empezado-titulo")),
                        Cc.mm(this.plugin.messages().raw("duelos.empezado-subtitulo"),
                                "kit", match.kitName(), "rival", rival == null ? "?" : nameOf(rival)),
                        5, 25, 10);
            }
            if (this.plugin.settings().sounds) {
                PlayerState.sound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.2F);
            }
        }
    }

    /** Gestiona la muerte de un jugador dentro de un duelo. */
    public void handleDeath(Player victim, Player killer) {
        String id = this.byPlayer.get(victim.getUniqueId());
        if (id == null) {
            return;
        }
        DuelMatch match = this.matches.get(id);
        if (match == null) {
            return;
        }
        PlayerProfile victimProfile = this.plugin.profiles().profile(victim);
        victimProfile.addDeath();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            PlayerProfile killerProfile = this.plugin.profiles().profile(killer);
            killerProfile.addKill();
            int[] record = killerProfile.kitRecord(match.kitName());
            record[2]++;
        }
        match.markDead(victim.getUniqueId());
        if (this.plugin.settings().sounds) {
            PlayerState.sound(victim, Sound.ENTITY_VILLAGER_DEATH, 0.6F, 0.8F);
        }
        // Reaparicion inmediata como espectador
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            if (!victim.isOnline()) {
                return;
            }
            victim.spigot().respawn();
            victim.setGameMode(GameMode.SPECTATOR);
            Arena arena = this.plugin.arenas().get(match.arenaName());
            if (arena != null && arena.spectator() != null) {
                PlayerState.teleport(victim, arena.spectator());
            }
        });
        this.plugin.profiles().markDirty();

        // ¿Se ha decidido la ronda?
        if (match.state() != GameState.FIGHTING) {
            return;
        }
        int roundWinner = match.roundWinner();
        if (roundWinner >= 0 || roundWinner == -2) {
            finishRound(match, roundWinner);
        }
    }

    /** Cierra la ronda y programa la siguiente. */
    private void finishRound(DuelMatch match, int roundWinner) {
        match.state(GameState.ROUND_END);
        if (roundWinner >= 0) {
            match.scores()[roundWinner]++;
        }
        List<UUID> winners = roundWinner >= 0 ? match.team(roundWinner) : new ArrayList<>();
        List<UUID> losers = roundWinner >= 0 ? match.team(roundWinner == 0 ? 1 : 0) : new ArrayList<>();

        Component scoreLine = Cc.mm("<#BFC4CC>Marcador: <#FFD1DC>" + nameOf(match.team(0).isEmpty() ? null : match.team(0).get(0))
                + " <#E8E8F0>" + match.scores()[0] + " <dark_gray>- <#E8E8F0>" + match.scores()[1]
                + " <#FFD1DC>" + nameOf(match.team(1).isEmpty() ? null : match.team(1).get(0)));
        for (UUID uuid : match.allPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.sendMessage(Cc.mm(this.plugin.messages().raw("duelos.cuenta-rondas"),
                    "ronda", Math.min(match.round(), match.totalRounds()), "total", match.totalRounds()));
            player.sendMessage(scoreLine);
            if (winners.contains(uuid)) {
                if (this.plugin.settings().titles) {
                    PlayerState.title(player, Cc.mm("<gradient:#C1E1C1:#B0E0E6><bold>RONDA GANADA</bold></gradient>"),
                            Component.empty(), 0, 25, 10);
                }
                if (this.plugin.settings().sounds) {
                    PlayerState.sound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.5F);
                }
            } else if (losers.contains(uuid)) {
                if (this.plugin.settings().titles) {
                    PlayerState.title(player, Cc.mm("<gradient:#FFB3BA:#FFD1DC><bold>RONDA PERDIDA</bold></gradient>"),
                            Component.empty(), 0, 25, 10);
                }
            }
        }
        this.roundEndsAt.put(match.id(), System.currentTimeMillis() + 3000L);
    }

    /** Avanza a la ronda siguiente o termina la partida. */
    private void nextRound(DuelMatch match) {
        this.roundEndsAt.remove(match.id());
        int winner = match.matchWinner();
        if (winner != -1) {
            endMatch(match, winner, "final");
            return;
        }
        match.round(match.round() + 1);
        startRound(match);
    }

    /** Finaliza la partida: estadisticas, ELO, resumen y limpieza. */
    public void endMatch(DuelMatch match, int winnerTeam, String reason) {
        if (match.state() == GameState.ENDING || match.state() == GameState.CANCELLED) {
            return;
        }
        this.roundEndsAt.remove(match.id());
        match.state(GameState.ENDING);
        match.endedAt(System.currentTimeMillis());
        match.winningTeam(winnerTeam);
        if (winnerTeam >= 0 && !match.team(winnerTeam).isEmpty()) {
            match.winnerPlayer(match.team(winnerTeam).get(0));
        }

        boolean ranked = this.plugin.settings().eloEnabled && match.type() != MatchType.FFA;
        for (int teamIndex = 0; teamIndex < match.teams().size(); teamIndex++) {
            for (UUID uuid : match.team(teamIndex)) {
                Player player = Bukkit.getPlayer(uuid);
                PlayerProfile profile = player != null
                        ? this.plugin.profiles().profile(player)
                        : this.plugin.profiles().profileOrCreate(uuid, nameOf(uuid));
                if (winnerTeam == -2) {
                    if (player != null) {
                        player.sendMessage(Cc.mm(this.plugin.messages().raw("duelos.resumen-titulo")));
                        player.sendMessage(Cc.mm("<#BFC4CC>Empate tecnico."));
                    }
                    continue;
                }
                boolean won = teamIndex == winnerTeam;
                int before = profile.elo();
                int delta = 0;
                if (ranked) {
                    double multiplier = match.totalRounds() > 1 ? this.plugin.settings().eloRoundsMultiplier : 1.0D;
                    if (won) {
                        int bonus = profile.streak() >= 3 ? this.plugin.settings().eloStreakBonus : 0;
                        delta = (int) Math.round((this.plugin.settings().eloWin + bonus) * multiplier);
                        profile.elo(clamp(profile.elo() + delta));
                    } else {
                        delta = (int) Math.round(this.plugin.settings().eloLoss * multiplier);
                        profile.elo(clamp(profile.elo() - delta));
                    }
                }
                if (won) {
                    profile.addWin();
                    profile.streak(profile.streak() + 1);
                    if (profile.streak() > profile.bestStreak()) {
                        profile.bestStreak(profile.streak());
                    }
                    profile.kitRecord(match.kitName())[0]++;
                } else {
                    profile.addLoss();
                    profile.streak(0);
                    profile.kitRecord(match.kitName())[1]++;
                }
                if (player != null) {
                    sendSummary(player, match, profile, won, before, delta, ranked);
                }
            }
        }
        this.plugin.profiles().markDirty();

        // Efectos de victoria
        if (winnerTeam >= 0 && this.plugin.settings().particles) {
            for (UUID uuid : match.team(winnerTeam)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.4D, 0.8D, 0.4D, 0.02D);
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5D, 1.0D, 0.5D, 0.0D);
                }
            }
        }

        int summary = this.plugin.settings().summarySeconds;
        this.finishAt.put(match.id(), System.currentTimeMillis() + summary * 1000L);
    }

    private void sendSummary(Player player, DuelMatch match, PlayerProfile profile, boolean won,
                             int before, int delta, boolean ranked) {
        Arena arena = this.plugin.arenas().get(match.arenaName());
        player.sendMessage(Component.empty());
        player.sendMessage(Cc.mm(this.plugin.messages().raw("duelos.resumen-titulo")));
        player.sendMessage(Cc.mm("<#BFC4CC>Resultado: " + (won ? "<#C1E1C1>VICTORIA" : "<#FFB3BA>DERROTA")));
        player.sendMessage(Cc.mm("<#BFC4CC>Kit: <#FFD1DC>" + match.kitName()
                + " <dark_gray>| <#BFC4CC>Arena: <#E8E8F0>" + (arena == null ? "?" : arena.name())));
        player.sendMessage(Cc.mm("<#BFC4CC>Marcador: <#E8E8F0>" + match.scoreLine()
                + " <dark_gray>| <#BFC4CC>Duracion: <#FFF5BA>" + Cc.time(match.duration())));
        if (ranked) {
            if (won) {
                player.sendMessage(Cc.mm(this.plugin.messages().raw("duelos.elo-ganado"),
                        "antes", before, "despues", profile.elo(), "cambio", delta));
            } else {
                player.sendMessage(Cc.mm(this.plugin.messages().raw("duelos.elo-perdido"),
                        "antes", before, "despues", profile.elo(), "cambio", delta));
            }
        }
        player.sendMessage(Cc.mm("<#BFC4CC>Kills: <#C1E1C1>" + profile.kills()
                + " <dark_gray>| <#BFC4CC>Muertes: <#FFB3BA>" + profile.deaths()
                + " <dark_gray>| <#BFC4CC>Racha: <#FFD1DC>" + profile.streak()));
        player.sendMessage(Component.empty());
        if (this.plugin.settings().titles && this.plugin.settings().effectTitles) {
            if (won) {
                PlayerState.title(player, Cc.mm("<gradient:#C1E1C1:#B0E0E6><bold>¡VICTORIA!</bold></gradient>"),
                        Cc.mm("<#E8E8F0>+" + delta + " ELO"), 5, 40, 10);
            } else {
                PlayerState.title(player, Cc.mm("<gradient:#FFB3BA:#FFD1DC><bold>DERROTA</bold></gradient>"),
                        Cc.mm("<#BFC4CC>Sigue practicando"), 5, 40, 10);
            }
        }
        if (this.plugin.settings().sounds) {
            PlayerState.sound(player, won ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_DEATH, 0.9F, won ? 1.2F : 0.8F);
        }
    }

    private int clamp(int elo) {
        return Math.max(this.plugin.settings().eloMin, Math.min(this.plugin.settings().eloMax, elo));
    }

    /** Un jugador abandona su partida (salida del servidor o /spawn). */
    public void leave(Player player, boolean quit) {
        String id = this.byPlayer.get(player.getUniqueId());
        if (id == null) {
            return;
        }
        DuelMatch match = this.matches.get(id);
        if (match == null) {
            this.byPlayer.remove(player.getUniqueId());
            return;
        }
        match.markDead(player.getUniqueId());
        int team = match.teamOf(player.getUniqueId());
        int other = team == 0 ? 1 : 0;
        if (match.state() == GameState.FIGHTING || match.state() == GameState.COUNTDOWN) {
            for (UUID uuid : match.allPlayers()) {
                if (uuid.equals(player.getUniqueId())) {
                    continue;
                }
                Player other1 = Bukkit.getPlayer(uuid);
                if (other1 != null) {
                    this.plugin.messages().send(other1, "duelos.abandonado", "jugador", player.getName());
                }
            }
            if (match.team(other).isEmpty()) {
                endMatch(match, -2, "abandono");
            } else if (match.state() == GameState.FIGHTING) {
                finishRound(match, other);
            } else {
                endMatch(match, other, "abandono");
            }
        }
        if (quit) {
            finalizePlayer(player.getUniqueId());
        }
        this.plugin.profiles().markDirty();
    }

    /** Espectador de una partida en curso. */
    public void spectate(Player spectator, DuelMatch match) {
        if (match == null) {
            return;
        }
        Arena arena = this.plugin.arenas().get(match.arenaName());
        if (arena == null) {
            return;
        }
        PlayerState.lobby(spectator);
        spectator.setGameMode(GameMode.SPECTATOR);
        PlayerState.teleport(spectator, arena.spectator() == null ? arena.center() : arena.spectator());
        match.addSpectator(spectator.getUniqueId());
        PlayerProfile profile = this.plugin.profiles().profile(spectator);
        profile.spectatingId(match.id());
        this.plugin.messages().send(spectator, "duelos.espectador-unido",
                "jugador", nameOf(match.team(0).isEmpty() ? match.team(1).get(0) : match.team(0).get(0)));
    }

    public void stopSpectating(Player spectator) {
        PlayerProfile profile = this.plugin.profiles().profile(spectator);
        String id = profile.spectatingId();
        if (id == null) {
            return;
        }
        DuelMatch match = this.matches.get(id);
        if (match != null) {
            match.removeSpectator(spectator.getUniqueId());
        }
        profile.spectatingId(null);
        this.plugin.messages().send(spectator, "duelos.espectador-salio");
        this.plugin.lobby().sendToLobby(spectator);
    }

    // ------------------------------------------------------------------
    //  Task periodico
    // ------------------------------------------------------------------
    public void tick() {
        long now = System.currentTimeMillis();
        // peticiones expiradas
        if (!this.pending.isEmpty()) {
            this.pending.entrySet().removeIf(entry -> {
                if (entry.getValue().expired()) {
                    Player target = Bukkit.getPlayer(entry.getKey());
                    if (target != null) {
                        this.plugin.messages().send(target, "duelos.expirado");
                    }
                    Player challenger = Bukkit.getPlayer(entry.getValue().challenger());
                    if (challenger != null) {
                        this.plugin.messages().send(challenger, "duelos.expirado");
                    }
                    return true;
                }
                return false;
            });
        }
        if (this.matches.isEmpty()) {
            return;
        }
        List<DuelMatch> toFinalize = new ArrayList<>(2);
        for (DuelMatch match : this.matches.values()) {
            switch (match.state()) {
                case COUNTDOWN -> {
                    if (now >= match.countdownEndsAt()) {
                        beginFight(match);
                    }
                }
                case FIGHTING -> {
                    int limit = this.plugin.settings().matchLimitMinutes;
                    if (limit > 0 && match.duration() > limit * 60000L) {
                        int winner = match.scores()[0] == match.scores()[1] ? -2 : (match.scores()[0] > match.scores()[1] ? 0 : 1);
                        endMatch(match, winner, "tiempo");
                    }
                }
                case ROUND_END -> {
                    Long until = this.roundEndsAt.get(match.id());
                    if (until != null && now >= until) {
                        nextRound(match);
                    }
                }
                case ENDING -> {
                    Long until = this.finishAt.get(match.id());
                    if (until != null && now >= until) {
                        toFinalize.add(match);
                    }
                }
                default -> {
                }
            }
        }
        for (DuelMatch match : toFinalize) {
            finalizeMatch(match);
        }
    }

    /** Limpia la partida y devuelve a todos al lobby. */
    private void finalizeMatch(DuelMatch match) {
        this.matches.remove(match.id());
        this.finishAt.remove(match.id());
        Arena arena = this.plugin.arenas().get(match.arenaName());
        if (arena != null) {
            arena.lastWinner(nameOf(match.winnerPlayer()));
            this.plugin.arenas().release(arena);
        }
        for (UUID uuid : match.allPlayers()) {
            this.byPlayer.remove(uuid);
            PlayerProfile profile = this.plugin.profiles().profile(uuid);
            if (profile != null) {
                profile.matchId(null);
            }
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                this.plugin.lobby().sendToLobby(player);
            }
        }
        for (UUID uuid : new ArrayList<>(match.spectators())) {
            Player spectator = Bukkit.getPlayer(uuid);
            PlayerProfile profile = this.plugin.profiles().profile(uuid);
            if (profile != null) {
                profile.spectatingId(null);
            }
            if (spectator != null) {
                this.plugin.lobby().sendToLobby(spectator);
            }
        }
        match.spectators().clear();
    }

    private void finalizePlayer(UUID uuid) {
        this.byPlayer.remove(uuid);
        PlayerProfile profile = this.plugin.profiles().profile(uuid);
        if (profile != null) {
            profile.matchId(null);
        }
    }

    // ------------------------------------------------------------------
    //  Consultas
    // ------------------------------------------------------------------
    public DuelMatch match(UUID uuid) {
        String id = this.byPlayer.get(uuid);
        return id == null ? null : this.matches.get(id);
    }

    public DuelMatch matchById(String id) {
        return this.matches.get(id);
    }

    public boolean inMatch(UUID uuid) {
        return this.byPlayer.containsKey(uuid);
    }

    public Collection<DuelMatch> all() {
        return this.matches.values();
    }

    public int activeMatches() {
        return this.matches.size();
    }

    public String nameOf(UUID uuid) {
        if (uuid == null) {
            return "?";
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        PlayerProfile profile = this.plugin.profiles().profile(uuid);
        return profile == null ? "?" : profile.name();
    }

    public void clear() {
        this.matches.clear();
        this.byPlayer.clear();
        this.pending.clear();
        this.roundEndsAt.clear();
        this.finishAt.clear();
    }
}
