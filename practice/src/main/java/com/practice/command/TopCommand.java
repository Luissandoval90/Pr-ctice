package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.Leaderboard;
import com.practice.util.Cc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Clasificaciones: /top
 */
public final class TopCommand extends BaseCommand {

    public TopCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                this.plugin.menus().openTop(player, Leaderboard.Type.ELO);
            } else {
                show(sender, Leaderboard.Type.ELO);
            }
            return true;
        }
        Leaderboard.Type type = Leaderboard.Type.fromId(args[0]);
        if (type == null) {
            send(sender, "top.categoria-invalida");
            return true;
        }
        show(sender, type);
        return true;
    }

    private void show(CommandSender sender, Leaderboard.Type type) {
        List<Leaderboard.Entry> entries = this.plugin.profiles().top(type, 10);
        if (entries.isEmpty()) {
            send(sender, "top.vacio");
            return;
        }
        sendRaw(sender, "top.header", "categoria", type.display());
        for (Leaderboard.Entry entry : entries) {
            String value = type == Leaderboard.Type.ELO
                    ? String.valueOf((int) entry.value())
                    : Cc.number((long) entry.value());
            sendRaw(sender, "top.linea", "posicion", String.valueOf(entry.position()), "jugador", entry.name(), "valor", value);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("kills", "elo", "wins", "streak", "ffakills"), args[0]);
        }
        return new ArrayList<>();
    }
}
