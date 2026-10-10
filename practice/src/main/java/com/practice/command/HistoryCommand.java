package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.MatchRecord;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Historial de partidas: /historial
 */
public final class HistoryCommand extends BaseCommand {

    public HistoryCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PlayerProfile profile;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                send(sender, "generales.jugador-desconocido");
                return true;
            }
            this.plugin.menus().openHistory(player, player);
            return true;
        }
        profile = this.plugin.profiles().offline(args[0]);
        if (profile == null) {
            send(sender, "stats.sin-datos");
            return true;
        }
        if (sender instanceof Player player && args.length == 1) {
            Player online = org.bukkit.Bukkit.getPlayerExact(args[0]);
            if (online != null) {
                this.plugin.menus().openHistory(player, online);
                return true;
            }
        }
        if (profile.history().isEmpty()) {
            send(sender, "historial.vacio");
            return true;
        }
        sender.sendMessage(Cc.mm("<gradient:#B0E0E6:#C1E1C1><bold>Historial de " + profile.name() + "</bold></gradient>"));
        int shown = 0;
        for (MatchRecord record : profile.history()) {
            if (shown++ >= 8) {
                break;
            }
            sender.sendMessage(Cc.mm((record.won() ? "<#C1E1C1>W" : "<#FFB3BA>L")
                    + " <#E8E8F0>vs <#FFD1DC>" + record.opponent()
                    + " <dark_gray>| <#BFC4CC>" + record.kit()
                    + " <dark_gray>| <#E8E8F0>" + record.score()
                    + " <dark_gray>| " + (record.eloDelta() >= 0 ? "<#C1E1C1>+" : "<#FFB3BA>") + record.eloDelta()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(onlineNames(), args[0]);
        }
        return new ArrayList<>();
    }
}
