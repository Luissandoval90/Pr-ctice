package com.practice.command;

import com.practice.PracticePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Ajustes personales: /opciones
 */
public final class SettingsCommand extends BaseCommand {

    public SettingsCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("division")) {
            this.plugin.menus().openDivision(player);
            return true;
        }
        if (args.length > 0 && (args[0].equalsIgnoreCase("desafios") || args[0].equalsIgnoreCase("retos"))) {
            this.plugin.menus().openChallenges(player);
            return true;
        }
        this.plugin.menus().openSettings(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("ajustes", "division", "desafios"), args[0]);
        }
        return new ArrayList<>();
    }
}
