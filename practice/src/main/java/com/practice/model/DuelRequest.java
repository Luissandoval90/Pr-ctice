package com.practice.model;

import java.util.UUID;

/**
 * Peticion de duelo entre dos jugadores.
 */
public final class DuelRequest {

    private final UUID challenger;
    private final UUID target;
    private final String kit;
    private final int rounds;
    private final long createdAt;
    private final long expiresAt;

    public DuelRequest(UUID challenger, UUID target, String kit, int rounds, long expireSeconds) {
        this.challenger = challenger;
        this.target = target;
        this.kit = kit;
        this.rounds = rounds;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + expireSeconds * 1000L;
    }

    public UUID challenger() {
        return this.challenger;
    }

    public UUID target() {
        return this.target;
    }

    public String kit() {
        return this.kit;
    }

    public int rounds() {
        return this.rounds;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public boolean expired() {
        return System.currentTimeMillis() > this.expiresAt;
    }

    public long remainingMillis() {
        return Math.max(0L, this.expiresAt - System.currentTimeMillis());
    }

    public Integer[] roundsArray() {
        Integer[] array = new Integer[this.rounds];
        for (int i = 0; i < this.rounds; i++) {
            array[i] = i + 1;
        }
        return array;
    }

    /** Clave unica para mapas de peticiones. */
    public String key() {
        return this.challenger + ":" + this.target;
    }
}
