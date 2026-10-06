package com.practice.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Partida de duelo en curso (1v1, 2v2, equipos).
 * <p>
 * Mantiene los equipos, marcadores, rondas y espectadores.
 */
public final class DuelMatch {

    private final String id;
    private final MatchType type;
    private final String kitName;
    private final String arenaName;
    private final List<List<UUID>> teams = new ArrayList<>(2);
    private final int[] scores;
    private final int totalRounds;
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();

    private GameState state = GameState.WAITING;
    private int round = 1;
    private long startedAt;
    private long roundStartedAt;
    private long countdownEndsAt;
    private long endedAt;
    private UUID winnerTeamPlayer;
    private int winningTeam = -1;

    public DuelMatch(String id, MatchType type, String kitName, String arenaName, int totalRounds) {
        this.id = id;
        this.type = type;
        this.kitName = kitName;
        this.arenaName = arenaName;
        this.totalRounds = Math.max(1, totalRounds);
        this.scores = new int[2];
    }

    public String id() {
        return this.id;
    }

    public MatchType type() {
        return this.type;
    }

    public String kitName() {
        return this.kitName;
    }

    public String arenaName() {
        return this.arenaName;
    }

    public List<List<UUID>> teams() {
        return this.teams;
    }

    public int[] scores() {
        return this.scores;
    }

    public int totalRounds() {
        return this.totalRounds;
    }

    public int round() {
        return this.round;
    }

    public void round(int value) {
        this.round = value;
    }

    public Set<UUID> spectators() {
        return this.spectators;
    }

    public Set<UUID> alive() {
        return this.alive;
    }

    public GameState state() {
        return this.state;
    }

    public void state(GameState value) {
        this.state = value;
    }

    public long startedAt() {
        return this.startedAt;
    }

    public void startedAt(long value) {
        this.startedAt = value;
    }

    public long roundStartedAt() {
        return this.roundStartedAt;
    }

    public void roundStartedAt(long value) {
        this.roundStartedAt = value;
    }

    public long countdownEndsAt() {
        return this.countdownEndsAt;
    }

    public void countdownEndsAt(long value) {
        this.countdownEndsAt = value;
    }

    public long endedAt() {
        return this.endedAt;
    }

    public void endedAt(long value) {
        this.endedAt = value;
    }

    public UUID winnerPlayer() {
        return this.winnerTeamPlayer;
    }

    public int winningTeam() {
        return this.winningTeam;
    }

    public void winningTeam(int value) {
        this.winningTeam = value;
    }

    public void winnerPlayer(UUID uuid) {
        this.winnerTeamPlayer = uuid;
    }

    /** Agrega un equipo a la partida. */
    public void addTeam(List<UUID> players) {
        if (this.teams.size() >= 2) {
            return;
        }
        this.teams.add(new ArrayList<>(players));
        this.alive.addAll(players);
    }

    public List<UUID> team(int index) {
        return index >= 0 && index < this.teams.size() ? this.teams.get(index) : Collections.emptyList();
    }

    public List<UUID> allPlayers() {
        List<UUID> all = new ArrayList<>();
        for (List<UUID> team : this.teams) {
            all.addAll(team);
        }
        return all;
    }

    public int teamOf(UUID uuid) {
        for (int i = 0; i < this.teams.size(); i++) {
            if (this.teams.get(i).contains(uuid)) {
                return i;
            }
        }
        return -1;
    }

    public List<UUID> opponentsOf(UUID uuid) {
        int team = teamOf(uuid);
        if (team < 0) {
            return Collections.emptyList();
        }
        return this.team(team == 0 ? 1 : 0);
    }

    public UUID firstOpponent(UUID uuid) {
        List<UUID> opponents = opponentsOf(uuid);
        return opponents.isEmpty() ? null : opponents.get(0);
    }

    public boolean contains(UUID uuid) {
        for (List<UUID> team : this.teams) {
            if (team.contains(uuid)) {
                return true;
            }
        }
        return false;
    }

    public boolean alive(UUID uuid) {
        return this.alive.contains(uuid);
    }

    public void markDead(UUID uuid) {
        this.alive.remove(uuid);
    }

    public int aliveIn(int team) {
        int count = 0;
        for (UUID uuid : this.team(team)) {
            if (this.alive.contains(uuid)) {
                count++;
            }
        }
        return count;
    }

    /** Equipo ganador de la ronda actual, o -1 si sigue en curso. */
    public int roundWinner() {
        if (this.aliveIn(0) > 0 && this.aliveIn(1) == 0) {
            return 0;
        }
        if (this.aliveIn(1) > 0 && this.aliveIn(0) == 0) {
            return 1;
        }
        if (this.aliveIn(0) > 0 && this.aliveIn(1) > 0) {
            return -1;
        }
        return -2; // empate / nadie vivo
    }

    /** Equipo que ha ganado la partida, o -1 si aun no hay. */
    public int matchWinner() {
        int needed = this.totalRounds / 2 + 1;
        if (this.scores[0] >= needed) {
            return 0;
        }
        if (this.scores[1] >= needed) {
            return 1;
        }
        if (this.round > this.totalRounds) {
            return this.scores[0] == this.scores[1] ? -2 : (this.scores[0] > this.scores[1] ? 0 : 1);
        }
        return -1;
    }

    public boolean finished() {
        return matchWinner() != -1;
    }

    public int neededWins() {
        return this.totalRounds / 2 + 1;
    }

    public long duration() {
        return this.roundStartedAt == 0L ? 0L : System.currentTimeMillis() - this.roundStartedAt;
    }

    public String scoreLine() {
        return this.scores[0] + " - " + this.scores[1];
    }

    public void addSpectator(UUID uuid) {
        this.spectators.add(uuid);
    }

    public void removeSpectator(UUID uuid) {
        this.spectators.remove(uuid);
    }
}
