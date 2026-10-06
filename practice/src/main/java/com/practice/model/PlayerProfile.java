package com.practice.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Perfil de un jugador con todas sus estadisticas.
 * <p>
 * Se mantiene en memoria durante la sesion y se persiste de forma
 * asincrona para no bloquear el hilo principal (optimizacion).
 */
public final class PlayerProfile {

    private final UUID uuid;
    private String name;

    // Estadisticas globales de duelos
    private int elo;
    private int kills;
    private int deaths;
    private int wins;
    private int losses;
    private int streak;
    private int bestStreak;
    private int eloPeak;

    // Estadisticas FFA
    private int ffaKills;
    private int ffaDeaths;
    private int ffaStreak;
    private int ffaBestStreak;

    // Tiempo jugado
    private long playtimeMillis;

    // Estado en memoria (no persistente)
    private transient String matchId;
    private transient String ffaArena;
    private transient String spectatingId;
    private transient UUID partyId;
    private transient long lastBoardUpdate;
    private transient long boardCooldownUntil;
    private transient final Map<Leaderboard.Type, Integer> cachedRanks = new EnumMap<>(Leaderboard.Type.class);

    // Estadisticas por kit (wins, losses, kills)
    private final Map<String, int[]> kitRecords = new java.util.HashMap<>();

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String name() {
        return this.name;
    }

    public void name(String name) {
        this.name = name;
    }

    public int elo() {
        return this.elo;
    }

    public void elo(int elo) {
        this.elo = elo;
        if (elo > this.eloPeak) {
            this.eloPeak = elo;
        }
    }

    public int eloPeak() {
        return this.eloPeak;
    }

    public void eloPeak(int value) {
        this.eloPeak = value;
    }

    public int kills() {
        return this.kills;
    }

    public void kills(int value) {
        this.kills = value;
    }

    public void addKill() {
        this.kills++;
    }

    public int deaths() {
        return this.deaths;
    }

    public void deaths(int value) {
        this.deaths = value;
    }

    public void addDeath() {
        this.deaths++;
    }

    public int wins() {
        return this.wins;
    }

    public void wins(int value) {
        this.wins = value;
    }

    public void addWin() {
        this.wins++;
    }

    public int losses() {
        return this.losses;
    }

    public void losses(int value) {
        this.losses = value;
    }

    public void addLoss() {
        this.losses++;
    }

    public int streak() {
        return this.streak;
    }

    public void streak(int value) {
        this.streak = value;
    }

    public int bestStreak() {
        return this.bestStreak;
    }

    public void bestStreak(int value) {
        this.bestStreak = value;
    }

    public int ffaKills() {
        return this.ffaKills;
    }

    public void ffaKills(int value) {
        this.ffaKills = value;
    }

    public void addFfaKill() {
        this.ffaKills++;
    }

    public int ffaDeaths() {
        return this.ffaDeaths;
    }

    public void ffaDeaths(int value) {
        this.ffaDeaths = value;
    }

    public void addFfaDeath() {
        this.ffaDeaths++;
    }

    public int ffaStreak() {
        return this.ffaStreak;
    }

    public void ffaStreak(int value) {
        this.ffaStreak = value;
    }

    public int ffaBestStreak() {
        return this.ffaBestStreak;
    }

    public void ffaBestStreak(int value) {
        this.ffaBestStreak = value;
    }

    public long playtimeMillis() {
        return this.playtimeMillis;
    }

    public void playtimeMillis(long value) {
        this.playtimeMillis = value;
    }

    public void addPlaytime(long millis) {
        this.playtimeMillis += millis;
    }

    public String matchId() {
        return this.matchId;
    }

    public void matchId(String id) {
        this.matchId = id;
    }

    public boolean inMatch() {
        return this.matchId != null;
    }

    public String ffaArena() {
        return this.ffaArena;
    }

    public void ffaArena(String arena) {
        this.ffaArena = arena;
    }

    public boolean inFfa() {
        return this.ffaArena != null;
    }

    public boolean busy() {
        return inMatch() || inFfa();
    }

    public String spectatingId() {
        return this.spectatingId;
    }

    public void spectatingId(String id) {
        this.spectatingId = id;
    }

    public boolean spectating() {
        return this.spectatingId != null;
    }

    public UUID partyId() {
        return this.partyId;
    }

    public void partyId(UUID id) {
        this.partyId = id;
    }

    public long lastBoardUpdate() {
        return this.lastBoardUpdate;
    }

    public void lastBoardUpdate(long value) {
        this.lastBoardUpdate = value;
    }

    public long boardCooldownUntil() {
        return this.boardCooldownUntil;
    }

    public void boardCooldownUntil(long value) {
        this.boardCooldownUntil = value;
    }

    public Map<Leaderboard.Type, Integer> cachedRanks() {
        return this.cachedRanks;
    }

    /** Registro por kit: [victorias, derrotas, kills]. */
    public int[] kitRecord(String kit) {
        return this.kitRecords.computeIfAbsent(kit, key -> new int[3]);
    }

    public Map<String, int[]> kitRecords() {
        return this.kitRecords;
    }

    // -------- Derivados --------
    public double kd() {
        return this.deaths == 0 ? this.kills : (double) this.kills / (double) this.deaths;
    }

    public double winRate() {
        int total = this.wins + this.losses;
        return total == 0 ? 0.0D : (double) this.wins * 100.0D / (double) total;
    }

    public double ffaKd() {
        return this.ffaDeaths == 0 ? this.ffaKills : (double) this.ffaKills / (double) this.ffaDeaths;
    }

    public String formattedPlaytime() {
        long totalMinutes = this.playtimeMillis / 60000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        return hours + "h " + minutes + "m";
    }
}
