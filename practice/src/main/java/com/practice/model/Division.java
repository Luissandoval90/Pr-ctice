package com.practice.model;

/**
 * Divisiones pastel del ranked. Se calculan a partir del ELO, no se guardan.
 */
public enum Division {

    HIERRO(0, 799, "Hierro", "#C9C9C9", "✖"),
    BRONCE(800, 999, "Bronce", "#E6C7A8", "◆"),
    PLATA(1000, 1199, "Plata", "#E8E8F0", "✦"),
    ORO(1200, 1399, "Oro", "#FFF5BA", "★"),
    PLATINO(1400, 1599, "Platino", "#B0E0E6", "❖"),
    DIAMANTE(1600, 1799, "Diamante", "#C1E1C1", "✺"),
    ESMERALDA(1800, 2099, "Esmeralda", "#A8E6CF", "❋"),
    AMATISTA(2100, 2499, "Amatista", "#E6D5F7", "✿"),
    BALTIA(2500, Integer.MAX_VALUE, "Baltia", "#FFD1DC", "♛");

    private final int min;
    private final int max;
    private final String display;
    private final String color;
    private final String symbol;

    Division(int min, int max, String display, String color, String symbol) {
        this.min = min;
        this.max = max;
        this.display = display;
        this.color = color;
        this.symbol = symbol;
    }

    public int min() {
        return this.min;
    }

    public int max() {
        return this.max;
    }

    public String display() {
        return this.display;
    }

    public String color() {
        return this.color;
    }

    public String symbol() {
        return this.symbol;
    }

    /** Nombre coloreado en MiniMessage. */
    public String colored() {
        return "<" + this.color + ">" + this.display;
    }

    /** Insignia corta para la TAB y el nametag. */
    public String badge() {
        return "<" + this.color + ">" + this.symbol;
    }

    public Division next() {
        int index = this.ordinal() + 1;
        return index >= values().length ? this : values()[index];
    }

    public boolean top() {
        return this == BALTIA;
    }

    /** Progreso 0..1 hacia la siguiente division. */
    public double progress(int elo) {
        if (this.top()) {
            return 1.0D;
        }
        int span = Math.max(1, this.max - this.min + 1);
        return Math.max(0.0D, Math.min(1.0D, (elo - this.min) / (double) span));
    }

    public static Division of(int elo) {
        for (Division division : values()) {
            if (elo >= division.min && elo <= division.max) {
                return division;
            }
        }
        return elo < 0 ? HIERRO : BALTIA;
    }
}
