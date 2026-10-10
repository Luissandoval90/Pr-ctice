package com.practice.util;

/**
 * Calculo de ELO real (puntuacion esperada) en lugar de un +/- fijo.
 */
public final class Elo {

    private Elo() {
    }

    /**
     * Delta firmado. Las victorias siempre suman al menos 1 y las derrotas restan al menos 1,
     * para que ninguna partida ranked sea invisible.
     */
    public static int change(int self, int opponent, boolean won, int kFactor, double multiplier) {
        int k = Math.max(1, kFactor);
        double expected = 1.0D / (1.0D + Math.pow(10.0D, (opponent - self) / 400.0D));
        double score = won ? 1.0D : 0.0D;
        double scale = multiplier <= 0.0D ? 1.0D : multiplier;
        int delta = (int) Math.round(k * (score - expected) * scale);
        if (won && delta < 1) {
            return 1;
        }
        if (!won && delta > -1) {
            return -1;
        }
        return delta;
    }
}
