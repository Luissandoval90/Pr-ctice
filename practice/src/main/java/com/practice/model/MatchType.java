package com.practice.model;

/**
 * Tipos de partida soportados.
 */
public enum MatchType {

    /** Uno contra uno clasico. */
    SOLO("1v1", "Duelo 1v1", 2, 2),
    /** Dos contra dos. */
    DUO("2v2", "Duelo 2v2", 4, 4),
    /** Duelo por equipos de cualquier tamano. */
    TEAM("Equipos", "Duelo por equipos", 2, 20),
    /** Combate libre, sin rondas ni equipos. */
    FFA("FFA", "Free For All", 2, 100);

    private final String id;
    private final String display;
    private final int minPlayers;
    private final int maxPlayers;

    MatchType(String id, String display, int minPlayers, int maxPlayers) {
        this.id = id;
        this.display = display;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    public String id() {
        return this.id;
    }

    public String display() {
        return this.display;
    }

    public int minPlayers() {
        return this.minPlayers;
    }

    public int maxPlayers() {
        return this.maxPlayers;
    }

    public boolean teamBased() {
        return this != FFA;
    }

    public static MatchType fromId(String id) {
        if (id == null) {
            return SOLO;
        }
        for (MatchType type : values()) {
            if (type.id.equalsIgnoreCase(id) || type.name().equalsIgnoreCase(id)) {
                return type;
            }
        }
        // heuristica por jugadores
        if (id.startsWith("2v2") || id.contains("duo")) {
            return DUO;
        }
        if (id.startsWith("ffa") || id.contains("free")) {
            return FFA;
        }
        if (id.startsWith("4v4") || id.startsWith("3v3") || id.contains("team")) {
            return TEAM;
        }
        return SOLO;
    }
}
