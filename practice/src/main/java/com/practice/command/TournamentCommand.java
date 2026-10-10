package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.Kit;
import com.practice.util.Cc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Torneos de eliminacion directa: /torneo
 */
public final class TournamentCommand extends BaseCommand {

    private static final List<String> SUBS = List.of("crear", "unirse", "salir", "empezar", "info", "cancelar");

    public TournamentCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            info(sender);
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "crear" -> {
                if (args.length < 2) {
                    usage(sender, "/torneo crear <kit> [4|8|16]");
                    return true;
                }
                Kit kit = kit(args[1]);
                if (kit == null) {
                    send(sender, "kits.no-existe", "kit", args[1]);
                    return true;
                }
                int size = 8;
                if (args.length >= 3) {
                    try {
                        size = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {
                        size = 8;
                    }
                }
                String error = this.plugin.tournaments().create(player, kit, size);
                if (error != null) {
                    send(sender, error, "max", this.plugin.tournaments().size());
                }
            }
            case "unirse", "join" -> {
                String error = this.plugin.tournaments().join(player);
                if (error != null) {
                    send(sender, error);
                }
            }
            case "salir", "leave" -> {
                String error = this.plugin.tournaments().leave(player);
                if (error != null) {
                    send(sender, error);
                } else {
                    send(sender, "torneos.saliste");
                }
            }
            case "empezar", "start" -> {
                String error = this.plugin.tournaments().start(player);
                if (error != null) {
                    send(sender, error);
                }
            }
            case "cancelar", "cancel" -> {
                if (!player.getUniqueId().equals(this.plugin.tournaments().host()) && !hasPermission(sender, "practice.admin")) {
                    return true;
                }
                this.plugin.tournaments().cancel("cancelado por " + player.getName());
            }
            default -> usage(sender, "/torneo <crear|unirse|salir|empezar|info|cancelar>");
        }
        return true;
    }

    private void info(CommandSender sender) {
        var tournament = this.plugin.tournaments();
        if (!tournament.active()) {
            send(sender, "torneos.no-abierto");
            return;
        }
        sender.sendMessage(Cc.mm("<gradient:#FFD1DC:#B0E0E6><bold>Torneo</bold></gradient> <#BFC4CC>"
                + tournament.kitName() + " <dark_gray>| <#E8E8F0>" + tournament.phase().name()
                + " <dark_gray>| <#BFC4CC>Ronda <#FFF5BA>" + tournament.round()));
        sender.sendMessage(Cc.mm("<#BFC4CC>Jugadores: <#FFD1DC>" + tournament.registered().size() + "/" + tournament.size()));
        StringBuilder names = new StringBuilder();
        for (UUID uuid : tournament.registered()) {
            if (names.length() > 0) {
                names.append("<#BFC4CC>, ");
            }
            names.append("<#E8E8F0>").append(this.plugin.duels().nameOf(uuid));
        }
        sender.sendMessage(Cc.mm(names.toString()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("crear")) {
            return filter(kitListNames(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("crear")) {
            return filter(List.of("4", "8", "16"), args[2]);
        }
        return new ArrayList<>();
    }
}
