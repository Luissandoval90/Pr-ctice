package com.practice.model;

/**
 * Estado en el que se encuentra una partida o ronda.
 */
public enum GameState {

    /** Esperando jugadores (parties o cola). */
    WAITING("Esperando"),
    /** Cuenta atras previa al combate. */
    COUNTDOWN("Iniciando"),
    /** Combate activo. */
    FIGHTING("En combate"),
    /** Ronda terminada, mostrando resultado. */
    ROUND_END("Ronda finalizada"),
    /** Partida terminada, resumen final. */
    ENDING("Finalizada"),
    /** Partida cancelada (jugador salio, error, etc). */
    CANCELLED("Cancelada");

    private final String display;

    GameState(String display) {
        this.display = display;
    }

    public String display() {
        return this.display;
    }
}
