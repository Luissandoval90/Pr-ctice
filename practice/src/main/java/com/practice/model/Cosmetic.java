package com.practice.model;

import org.bukkit.Particle;
import org.bukkit.Sound;

/**
 * Cosmeticos de kill y de victoria. El precio esta en monedas.
 */
public enum Cosmetic {

    NINGUNO("ninguno", "Ninguno", 0, Kind.BOTH, null, null),
    CORAZONES("corazones", "Corazones", 40, Kind.KILL, Particle.HEART, Sound.ENTITY_EXPERIENCE_ORB_PICKUP),
    LLAMAS("llamas", "Llamas pastel", 50, Kind.KILL, Particle.FLAME, Sound.BLOCK_NOTE_BLOCK_HAT),
    TOTEM("totem", "Totem", 80, Kind.KILL, Particle.TOTEM_OF_UNDYING, Sound.ENTITY_PLAYER_LEVELUP),
    ALMAS("almas", "Almas", 70, Kind.KILL, Particle.SOUL, Sound.BLOCK_AMETHYST_BLOCK_CHIME),
    FUEGOS("fuegos", "Fuegos", 90, Kind.WIN, Particle.FIREWORK, Sound.ENTITY_PLAYER_LEVELUP),
    ESTRELLAS("estrellas", "Estrellas", 60, Kind.WIN, Particle.END_ROD, Sound.BLOCK_NOTE_BLOCK_PLING),
    BRILLO("brillo", "Brillo", 45, Kind.WIN, Particle.HAPPY_VILLAGER, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);

    public enum Kind { KILL, WIN, BOTH }

    private final String id;
    private final String display;
    private final int price;
    private final Kind kind;
    private final Particle particle;
    private final Sound sound;

    Cosmetic(String id, String display, int price, Kind kind, Particle particle, Sound sound) {
        this.id = id;
        this.display = display;
        this.price = price;
        this.kind = kind;
        this.particle = particle;
        this.sound = sound;
    }

    public String id() {
        return this.id;
    }

    public String display() {
        return this.display;
    }

    public int price() {
        return this.price;
    }

    public Kind kind() {
        return this.kind;
    }

    public boolean killEffect() {
        return this.kind == Kind.KILL || this.kind == Kind.BOTH;
    }

    public boolean winEffect() {
        return this.kind == Kind.WIN || this.kind == Kind.BOTH;
    }

    public Particle particle() {
        return this.particle;
    }

    public Sound sound() {
        return this.sound;
    }

    public static Cosmetic byId(String id) {
        if (id == null) {
            return NINGUNO;
        }
        for (Cosmetic cosmetic : values()) {
            if (cosmetic.id.equalsIgnoreCase(id)) {
                return cosmetic;
            }
        }
        return NINGUNO;
    }
}
