package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.DuelMatch;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Observar partidas: /spectate
 */
public final class SpectateCommand extends BaseCommand {

    public SpectateCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (args.length == 0) {
            // Espectar la primera partida disponible
            for (DuelMatch match : this.plugin.duels().all()) {
                this.plugin.duels().spectate(player, match);
                return true;
            }
            send(sender, "duelos.sin-peticion");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (this.plugin.profiles().profile(player).spectatingId() != null) {
            this.plugin.duels().stopSpectating(player);
        }
        DuelMatch match = this.plugin.duels().match(target.getUniqueId());
        if (match == null) {
            send(sender, "duelos.sin-peticion");
            return true;
        }
        this.plugin.duels().spectate(player, match);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (this.plugin.duels().inMatch(player.getUniqueId())) {
                    names.add(player.getName());
                }
            }
            return filter(names, args[0]);
        }
        return new ArrayList<>();
    }
}
