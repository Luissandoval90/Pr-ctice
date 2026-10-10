package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando de FFA: /ffa
 */
public final class FfaCommand extends BaseCommand {

    public FfaCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (args.length == 0) {
            this.plugin.menus().openFfa(player);
            return true;
        }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        if (sub.equals("leave") || sub.equals("salir")) {
            if (!this.plugin.ffa().inFfa(player.getUniqueId())) {
                send(sender, "ffa.no-existe");
                return true;
            }
            this.plugin.ffa().leave(player);
            return true;
        }
        if (sub.equals("lista") || sub.equals("list")) {
            sendRaw(sender, "arenas.lista-header");
            for (Arena arena : this.plugin.ffa().arenas()) {
                sendRaw(sender, "arenas.lista-item", "arena", arena.name(), "tipo", "FFA",
                        "estado", this.plugin.ffa().population(arena.name()) + " jugadores");
            }
            return true;
        }
        // /ffa <arena>
        Arena arena = this.plugin.arenas().get(args[0]);
        if (arena == null || !arena.type().equals("FFA")) {
            List<Arena> arenas = this.plugin.ffa().arenas();
            if (arenas.isEmpty()) {
                send(sender, "ffa.no-existe");
                return true;
            }
            arena = arenas.get(0);
        }
        this.plugin.ffa().join(player, arena.name());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("leave", "salir", "lista"));
            for (Arena arena : this.plugin.ffa().arenas()) {
                options.add(arena.name());
            }
            return filter(options, args[0]);
        }
        return new ArrayList<>();
    }
}
