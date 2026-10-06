package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.model.Kit;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Base de los comandos con utilidades de autocompletado.
 */
public abstract class BaseCommand implements TabExecutor {

    protected final PracticePlugin plugin;

    protected BaseCommand(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    protected void send(CommandSender sender, String key, Object... placeholders) {
        this.plugin.messages().send(sender, key, placeholders);
    }

    protected void sendRaw(CommandSender sender, String key, Object... placeholders) {
        this.plugin.messages().sendRaw(sender, key, placeholders);
    }

    protected void usage(CommandSender sender, String usage) {
        this.plugin.messages().send(sender, "generales.uso-comando", "uso", usage);
    }

    protected boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        this.plugin.messages().send(sender, "generales.sin-permiso");
        return false;
    }

    /** Filtra una lista de opciones por el prefijo escrito. */
    protected List<String> filter(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return options;
        }
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>(options.size());
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
        return out;
    }

    protected List<String> onlineNames() {
        List<String> names = new ArrayList<>(Bukkit.getOnlinePlayers().size());
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    protected List<String> kitNames() {
        return new ArrayList<>(this.plugin.kits().names());
    }

    protected List<String> arenaNames() {
        List<String> names = new ArrayList<>(this.plugin.arenas().count());
        for (Arena arena : this.plugin.arenas().all()) {
            names.add(arena.name());
        }
        return names;
    }

    protected Kit kit(String name) {
        return this.plugin.kits().get(name);
    }

    protected List<String> kitListNames() {
        List<String> names = new ArrayList<>();
        for (Kit kit : this.plugin.kits().all()) {
            names.add(kit.name());
        }
        return names;
    }
}
