package com.practice.model;

import java.util.UUID;

/**
 * Sesion de un jugador dentro de una arena FFA.
 */
public final class FfaSession {

    private final UUID uuid;
    private final String arena;
    private final long joinedAt;
    private int kills;
    private int deaths;
    private int streak;
    private int bestStreak;
    private long lastKillAt;
    private long lastDeathAt;

    public FfaSession(UUID uuid, String arena) {
        this.uuid = uuid;
        this.arena = arena;
        this.joinedAt = System.currentTimeMillis();
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String arena() {
        return this.arena;
    }

    public long joinedAt() {
        return this.joinedAt;
    }

    public int kills() {
        return this.kills;
    }

    public void kills(int value) {
        this.kills = value;
    }

    public int deaths() {
        return this.deaths;
    }

    public void deaths(int value) {
        this.deaths = value;
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

    public long lastKillAt() {
        return this.lastKillAt;
    }

    public void lastKillAt(long value) {
        this.lastKillAt = value;
    }

    public long lastDeathAt() {
        return this.lastDeathAt;
    }

    public void lastDeathAt(long value) {
        this.lastDeathAt = value;
    }

    public void registerKill() {
        this.kills++;
        this.streak++;
        this.lastKillAt = System.currentTimeMillis();
        if (this.streak > this.bestStreak) {
            this.bestStreak = this.streak;
        }
    }

    public void registerDeath() {
        this.deaths++;
        this.streak = 0;
        this.lastDeathAt = System.currentTimeMillis();
    }

    public double kd() {
        return this.deaths == 0 ? this.kills : (double) this.kills / (double) this.deaths;
    }

    public long duration() {
        return System.currentTimeMillis() - this.joinedAt;
    }
}
