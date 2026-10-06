package com.practice.command;

import com.practice.PracticePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Volver al lobby: /spawn
 */
public final class SpawnCommand extends BaseCommand {

    public SpawnCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (this.plugin.duels().inMatch(player.getUniqueId())) {
            this.plugin.duels().leave(player, true);
        }
        if (this.plugin.queue().queued(player.getUniqueId())) {
            this.plugin.queue().leave(player);
        }
        if (this.plugin.ffa().inFfa(player.getUniqueId())) {
            this.plugin.ffa().leave(player);
        }
        this.plugin.duels().stopSpectating(player);
        this.plugin.lobby().sendToLobby(player);
        this.plugin.board().update(player);
        send(sender, "generales.teleport-spawn");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
