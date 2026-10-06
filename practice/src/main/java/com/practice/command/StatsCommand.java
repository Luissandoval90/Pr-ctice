package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Estadisticas de jugadores: /stats
 */
public final class StatsCommand extends BaseCommand {

    public StatsCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 && sender instanceof Player player) {
            this.plugin.menus().openStats(player, player);
            return true;
        }
        String name = args.length > 0 ? args[0] : sender.getName();
        PlayerProfile profile = this.plugin.profiles().offline(name);
        Player target = Bukkit.getPlayerExact(name);
        if (profile == null && target != null) {
            profile = this.plugin.profiles().profile(target);
        }
        if (profile == null) {
            send(sender, "stats.sin-datos");
            return true;
        }
        sendRaw(sender, "stats.header", "jugador", profile.name());
        sendRaw(sender, "stats.linea", "nombre", "ELO", "valor", profile.elo()
                + " (max " + profile.eloPeak() + ")");
        sendRaw(sender, "stats.linea", "nombre", "Kills", "valor", profile.kills());
        sendRaw(sender, "stats.linea", "nombre", "Muertes", "valor", profile.deaths());
        sendRaw(sender, "stats.linea", "nombre", "K/D", "valor", Cc.decimal(profile.kd()));
        sendRaw(sender, "stats.linea", "nombre", "Victorias", "valor", profile.wins());
        sendRaw(sender, "stats.linea", "nombre", "Derrotas", "valor", profile.losses());
        sendRaw(sender, "stats.linea", "nombre", "Winrate", "valor", Cc.decimal(profile.winRate()) + "%");
        sendRaw(sender, "stats.linea", "nombre", "Racha actual", "valor", profile.streak());
        sendRaw(sender, "stats.linea", "nombre", "Mejor racha", "valor", profile.bestStreak());
        sendRaw(sender, "stats.linea", "nombre", "FFA kills", "valor", profile.ffaKills()
                + " (K/D " + Cc.decimal(profile.ffaKd()) + ")");
        sendRaw(sender, "stats.linea", "nombre", "Tiempo jugado", "valor", profile.formattedPlaytime());
        for (var entry : profile.kitRecords().entrySet()) {
            if (entry.getValue()[0] + entry.getValue()[1] == 0) {
                continue;
            }
            sendRaw(sender, "stats.linea", "nombre", "Kit " + entry.getKey(),
                    "valor", entry.getValue()[0] + "V / " + entry.getValue()[1] + "D");
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
