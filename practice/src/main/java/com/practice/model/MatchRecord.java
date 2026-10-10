package com.practice.model;

/**
 * Entrada compacta del historial de partidas (se persiste como una linea).
 */
public final class MatchRecord {

    private final long time;
    private final String opponent;
    private final String kit;
    private final boolean won;
    private final int eloDelta;
    private final boolean ranked;
    private final String score;

    public MatchRecord(long time, String opponent, String kit, boolean won, int eloDelta, boolean ranked, String score) {
        this.time = time;
        this.opponent = opponent == null ? "?" : opponent;
        this.kit = kit == null ? "?" : kit;
        this.won = won;
        this.eloDelta = eloDelta;
        this.ranked = ranked;
        this.score = score == null ? "-" : score;
    }

    public long time() {
        return this.time;
    }

    public String opponent() {
        return this.opponent;
    }

    public String kit() {
        return this.kit;
    }

    public boolean won() {
        return this.won;
    }

    public int eloDelta() {
        return this.eloDelta;
    }

    public boolean ranked() {
        return this.ranked;
    }

    public String score() {
        return this.score;
    }

    public String encode() {
        return this.time + ";" + clean(this.opponent) + ";" + clean(this.kit) + ";"
                + (this.won ? "1" : "0") + ";" + this.eloDelta + ";" + (this.ranked ? "1" : "0") + ";" + clean(this.score);
    }

    public static MatchRecord decode(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split(";", 7);
        if (parts.length < 7) {
            return null;
        }
        try {
            return new MatchRecord(Long.parseLong(parts[0]), parts[1], parts[2],
                    "1".equals(parts[3]), Integer.parseInt(parts[4]), "1".equals(parts[5]), parts[6]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String clean(String value) {
        return value.replace(';', ' ');
    }
}
