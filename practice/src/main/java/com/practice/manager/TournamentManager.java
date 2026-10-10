package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Kit;
import com.practice.model.MatchType;
import com.practice.util.Cc;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Torneo de eliminacion directa. Una sola instancia a la vez.
 * Las partidas se crean con el sistema de duelos y no dan ELO.
 */
public final class TournamentManager {

    public enum Phase { OPEN, RUNNING, DONE }

    private final PracticePlugin plugin;
    private Phase phase = Phase.DONE;
    private UUID host;
    private String kitName = "nodebuff";
    private int size = 8;
    private final List<UUID> registered = new ArrayList<>();
    private final List<UUID> nextRound = new ArrayList<>();
    private final List<UUID[]> waitingPairs = new ArrayList<>();
    private int awaiting;
    private int round = 1;
    private String id;

    public TournamentManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public Phase phase() {
        return this.phase;
    }

    public boolean active() {
        return this.phase == Phase.OPEN || this.phase == Phase.RUNNING;
    }

    public List<UUID> registered() {
        return Collections.unmodifiableList(this.registered);
    }

    public String kitName() {
        return this.kitName;
    }

    public int size() {
        return this.size;
    }

    public int round() {
        return this.round;
    }

    public UUID host() {
        return this.host;
    }

    public String create(Player host, Kit kit, int size) {
        if (!this.plugin.settings().tournamentsEnabled) {
            return "torneos.desactivado";
        }
        if (active()) {
            return "torneos.ya-activo";
        }
        this.phase = Phase.OPEN;
        this.host = host.getUniqueId();
        this.kitName = kit.name();
        this.size = size < 4 ? 4 : Math.min(16, size);
        this.registered.clear();
        this.registered.add(host.getUniqueId());
        this.round = 1;
        this.id = UUID.randomUUID().toString().substring(0, 6);
        Bukkit.broadcast(Cc.mm("<gradient:#FFD1DC:#B0E0E6><bold>TORNEO</bold></gradient> <#E8E8F0>"
                + host.getName() + " <#BFC4CC>abrio un torneo de <#FFD1DC>" + kit.name()
                + " <#BFC4CC>(" + this.size + "). <#C1E1C1>/torneo unirse"));
        return null;
    }

    public String join(Player player) {
        if (this.phase != Phase.OPEN) {
            return "torneos.no-abierto";
        }
        if (this.registered.contains(player.getUniqueId())) {
            return "torneos.ya-unido";
        }
        if (this.registered.size() >= this.size) {
            return "torneos.lleno";
        }
        if (this.plugin.duels().inMatch(player.getUniqueId()) || this.plugin.ffa().inFfa(player.getUniqueId())) {
            return "generales.en-partida";
        }
        this.registered.add(player.getUniqueId());
        broadcast("<#C1E1C1>" + player.getName() + " <#BFC4CC>se unio al torneo <#E8E8F0>("
                + this.registered.size() + "/" + this.size + ")");
        return null;
    }

    public String leave(Player player) {
        if (!this.registered.remove(player.getUniqueId())) {
            return "torneos.no-unido";
        }
        if (this.phase == Phase.OPEN && player.getUniqueId().equals(this.host) && !this.registered.isEmpty()) {
            this.host = this.registered.get(0);
        }
        if (this.registered.isEmpty()) {
            cancel("sin jugadores");
        }
        return null;
    }

    public String start(Player player) {
        if (this.phase != Phase.OPEN) {
            return "torneos.no-abierto";
        }
        if (!player.getUniqueId().equals(this.host) && !player.hasPermission("practice.admin")) {
            return "torneos.solo-host";
        }
        if (this.registered.size() < 2) {
            return "torneos.pocos";
        }
        this.phase = Phase.RUNNING;
        this.round = 1;
        this.nextRound.clear();
        this.nextRound.addAll(this.registered);
        Collections.shuffle(this.nextRound);
        broadcast("<gradient:#FFF5BA:#FFD1DC><bold>EL TORNEO EMPIEZA</bold></gradient> <#BFC4CC>("
                + this.registered.size() + " jugadores)");
        beginRound();
        return null;
    }

    private void beginRound() {
        List<UUID> players = new ArrayList<>(this.nextRound);
        this.nextRound.clear();
        this.waitingPairs.clear();
        this.awaiting = 0;
        if (players.size() <= 1) {
            finish(players.isEmpty() ? null : players.get(0));
            return;
        }
        broadcast("<#B0E0E6>Ronda " + this.round + " <#BFC4CC>· " + players.size() + " jugadores");
        int index = 0;
        while (index + 1 < players.size()) {
            queuePair(players.get(index), players.get(index + 1));
            index += 2;
        }
        if (index < players.size()) {
            UUID bye = players.get(index);
            this.nextRound.add(bye);
            Player lucky = Bukkit.getPlayer(bye);
            if (lucky != null) {
                this.plugin.messages().send(lucky, "torneos.bye");
            }
        }
        flushPairs();
        if (this.awaiting == 0 && this.waitingPairs.isEmpty()) {
            this.round++;
            beginRound();
        }
    }

    private void queuePair(UUID a, UUID b) {
        this.waitingPairs.add(new UUID[]{a, b});
    }

    public void tick() {
        if (this.phase == Phase.RUNNING) {
            flushPairs();
        }
    }

    private void flushPairs() {
        if (this.waitingPairs.isEmpty()) {
            return;
        }
        Kit kit = this.plugin.kits().get(this.kitName);
        if (kit == null) {
            cancel("kit eliminado");
            return;
        }
        List<UUID[]> still = new ArrayList<>();
        for (UUID[] pair : this.waitingPairs) {
            Player first = Bukkit.getPlayer(pair[0]);
            Player second = Bukkit.getPlayer(pair[1]);
            if (first == null || second == null) {
                UUID winner = first != null ? pair[0] : pair[1];
                if (first == null && second == null) {
                    continue;
                }
                this.nextRound.add(winner);
                continue;
            }
            if (this.plugin.duels().inMatch(first.getUniqueId()) || this.plugin.duels().inMatch(second.getUniqueId())) {
                still.add(pair);
                continue;
            }
            List<List<UUID>> teams = new ArrayList<>(2);
            teams.add(List.of(first.getUniqueId()));
            teams.add(List.of(second.getUniqueId()));
            boolean started = this.plugin.duels().startMatch(MatchType.SOLO, kit, teams,
                    kit.maxRounds() > 0 ? kit.maxRounds() : 1, false, this.id);
            if (started) {
                this.awaiting++;
            } else {
                still.add(pair);
            }
        }
        this.waitingPairs.clear();
        this.waitingPairs.addAll(still);
    }

    public void onMatchEnd(com.practice.model.DuelMatch match) {
        if (this.phase != Phase.RUNNING || match.tournamentId() == null || !match.tournamentId().equals(this.id)) {
            return;
        }
        this.awaiting = Math.max(0, this.awaiting - 1);
        int winner = match.winningTeam();
        if (winner >= 0 && !match.team(winner).isEmpty()) {
            UUID uuid = match.team(winner).get(0);
            this.nextRound.add(uuid);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Cc.mm("<#C1E1C1>Avanzas a la siguiente ronda del torneo."));
                this.plugin.profiles().profile(player).addCoins(10);
            }
        }
        if (this.awaiting == 0 && this.waitingPairs.isEmpty()) {
            this.round++;
            beginRound();
        }
    }

    private void finish(UUID champion) {
        this.phase = Phase.DONE;
        if (champion == null) {
            broadcast("<#FFB3BA>El torneo termino sin campeon.");
            clear();
            return;
        }
        String name = this.plugin.duels().nameOf(champion);
        int prize = this.plugin.settings().tournamentPrize;
        Player player = Bukkit.getPlayer(champion);
        if (player != null) {
            this.plugin.profiles().profile(player).addCoins(prize);
            this.plugin.profiles().markDirty();
            com.practice.util.PlayerState.title(player,
                    Cc.mm("<gradient:#FFF5BA:#FFD1DC><bold>CAMPEON</bold></gradient>"),
                    Cc.mm("<#E8E8F0>+" + prize + " monedas"), 5, 50, 10);
        }
        Bukkit.broadcast(Cc.mm("<gradient:#FFD1DC:#FFF5BA><bold>CAMPEON</bold></gradient> <#E8E8F0>"
                + name + " <#BFC4CC>gano el torneo de <#FFD1DC>" + this.kitName
                + " <#BFC4CC>(+" + prize + " monedas)"));
        clear();
    }

    public void cancel(String reason) {
        if (!active() && this.phase != Phase.RUNNING) {
            this.phase = Phase.DONE;
            return;
        }
        broadcast("<#FFB3BA>Torneo cancelado" + (reason == null ? "." : ": " + reason));
        this.phase = Phase.DONE;
        clear();
    }

    private void clear() {
        this.registered.clear();
        this.nextRound.clear();
        this.waitingPairs.clear();
        this.awaiting = 0;
        this.host = null;
    }

    public boolean contains(UUID uuid) {
        return this.registered.contains(uuid) || this.nextRound.contains(uuid);
    }

    /** Saca a un jugador que se desconecta fuera de una partida ya resuelta. */
    public void eliminate(UUID uuid) {
        this.registered.remove(uuid);
        this.nextRound.remove(uuid);
        java.util.Iterator<UUID[]> iterator = this.waitingPairs.iterator();
        while (iterator.hasNext()) {
            UUID[] pair = iterator.next();
            if (pair[0].equals(uuid) || pair[1].equals(uuid)) {
                UUID other = pair[0].equals(uuid) ? pair[1] : pair[0];
                iterator.remove();
                if (Bukkit.getPlayer(other) != null) {
                    this.nextRound.add(other);
                }
            }
        }
        if (this.phase == Phase.OPEN && this.registered.isEmpty()) {
            this.phase = Phase.DONE;
        }
    }

    private void broadcast(String mini) {
        Bukkit.broadcast(Cc.mm("<gradient:#E6D5F7:#B0E0E6><bold>Torneo</bold></gradient> " + mini));
    }
}
