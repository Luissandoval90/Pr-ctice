package com.practice.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Sistema de rangos con prefijos pastel.
 * <p>
 * Los rangos se resuelven por prioridad (el ultimo rango cuyo permiso tenga
 * el jugador) y se cachean por jugador desde el TabManager.
 */
public final class Ranks {

    /** Definicion de un rango. */
    public record Rank(String key, String permission, String prefix, String color) {

        public boolean isDefault() {
            return this.permission == null || this.permission.isEmpty();
        }
    }

    private final List<Rank> ranks = new ArrayList<>(8);

    public void load(JavaPlugin plugin) {
        this.ranks.clear();
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("rangos");
        if (section == null) {
            this.ranks.add(new Rank("default", "", "<#BFC4CC>Jugador", "#BFC4CC"));
            return;
        }
        // El rango default siempre primero
        if (section.isConfigurationSection("default")) {
            this.ranks.add(read("default", section.getConfigurationSection("default")));
        }
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("default")) {
                continue;
            }
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            if (rankSection != null) {
                this.ranks.add(read(key, rankSection));
            }
        }
    }

    private Rank read(String key, ConfigurationSection section) {
        return new Rank(key,
                section.getString("permiso", "practice." + key),
                section.getString("prefijo", "<#BFC4CC>" + key),
                section.getString("color", "#BFC4CC"));
    }

    /** Devuelve el rango de mayor prioridad que tiene el jugador. */
    public Rank of(Player player) {
        Rank result = this.ranks.isEmpty()
                ? new Rank("default", "", "<#BFC4CC>Jugador", "#BFC4CC")
                : this.ranks.get(0);
        for (Rank rank : this.ranks) {
            if (!rank.isDefault() && player.hasPermission(rank.permission())) {
                result = rank;
            }
        }
        return result;
    }

    public List<Rank> all() {
        return this.ranks;
    }
}
