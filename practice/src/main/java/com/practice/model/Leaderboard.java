package com.practice.model;

import java.util.List;
import java.util.UUID;

/**
 * Categorias de clasificacion disponibles para /top.
 */
public final class Leaderboard {

    public enum Type {
        KILLS("kills", "Asesinatos"),
        ELO("elo", "ELO"),
        WINS("wins", "Victorias"),
        STREAK("streak", "Rachas"),
        FFA_KILLS("ffakills", "Asesinatos FFA");

        private final String id;
        private final String display;

        Type(String id, String display) {
            this.id = id;
            this.display = display;
        }

        public String id() {
            return this.id;
        }

        public String display() {
            return this.display;
        }

        public static Type fromId(String id) {
            if (id == null) {
                return KILLS;
            }
            String lower = id.toLowerCase(java.util.Locale.ROOT);
            for (Type type : values()) {
                if (type.id.equals(lower) || type.name().equalsIgnoreCase(lower)) {
                    return type;
                }
            }
            return null;
        }
    }

    /** Entrada de la clasificacion. */
    public record Entry(UUID uuid, String name, double value, int position) {
    }

    private Leaderboard() {
    }

    public static List<Entry> top(java.util.Collection<PlayerProfile> profiles, Type type, int limit) {
        java.util.List<PlayerProfile> sorted = new java.util.ArrayList<>(profiles);
        sorted.sort((a, b) -> Double.compare(value(b, type), value(a, type)));
        java.util.List<Entry> out = new java.util.ArrayList<>(Math.min(limit, sorted.size()));
        for (int i = 0; i < sorted.size() && i < limit; i++) {
            PlayerProfile profile = sorted.get(i);
            out.add(new Entry(profile.uuid(), profile.name(), value(profile, type), i + 1));
        }
        return out;
    }

    public static double value(PlayerProfile profile, Type type) {
        return switch (type) {
            case KILLS -> profile.kills();
            case ELO -> profile.elo();
            case WINS -> profile.wins();
            case STREAK -> profile.bestStreak();
            case FFA_KILLS -> profile.ffaKills();
        };
    }
}
