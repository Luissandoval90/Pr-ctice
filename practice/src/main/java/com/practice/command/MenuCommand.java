package com.practice.command;

import com.practice.PracticePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu principal: /menu
 */
public final class MenuCommand extends BaseCommand {

    public MenuCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (args.length == 0) {
            this.plugin.menus().openMain(player);
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "kits", "duelos" -> this.plugin.menus().openKits(player);
            case "stats", "perfil" -> this.plugin.menus().openStats(player, player);
            case "ffa" -> this.plugin.menus().openFfa(player);
            case "top", "ranking" -> this.plugin.menus().openTop(player,
                    com.practice.model.Leaderboard.Type.ELO);
            case "party" -> this.plugin.menus().openParty(player);
            default -> this.plugin.menus().openMain(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("kits", "duelos", "stats", "ffa", "top", "party"), args[0]);
        }
        return new ArrayList<>();
    }
}
