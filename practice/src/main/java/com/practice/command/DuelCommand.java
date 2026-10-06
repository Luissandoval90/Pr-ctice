package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.DuelMatch;
import com.practice.model.DuelRequest;
import com.practice.model.Kit;
import com.practice.model.Party;
import com.practice.util.Cc;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando de duelos: /duel
 */
public final class DuelCommand extends BaseCommand {

    private static final List<String> SUBS = List.of("aceptar", "rechazar", "queue", "cola", "dejar", "salir",
            "espectar", "info");

    public DuelCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/duel <jugador> [kit] [rondas] | /duel queue <kit> | /duel aceptar | /duel rechazar");
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "aceptar", "accept" -> this.plugin.duels().accept(player);
            case "rechazar", "deny" -> this.plugin.duels().deny(player);
            case "queue", "cola" -> {
                if (args.length < 2) {
                    usage(sender, "/duel queue <kit>");
                    return true;
                }
                Kit kit = kit(args[1]);
                if (kit == null) {
                    send(sender, "kits.no-existe", "kit", args[1]);
                    return true;
                }
                if (this.plugin.queue().queued(player.getUniqueId())) {
                    this.plugin.queue().leave(player);
                }
                String error = this.plugin.queue().join(player, kit);
                if (error != null) {
                    send(sender, error);
                }
            }
            case "dejar", "salir", "leave" -> {
                if (this.plugin.queue().queued(player.getUniqueId())) {
                    this.plugin.queue().leave(player);
                    send(sender, "ffa.salido");
                } else if (this.plugin.duels().inMatch(player.getUniqueId())) {
                    this.plugin.duels().leave(player, true);
                    this.plugin.lobby().sendToLobby(player);
                } else {
                    send(sender, "duelos.sin-peticion");
                }
            }
            case "espectar", "spectate" -> {
                if (args.length < 2) {
                    usage(sender, "/duel espectar <jugador>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    send(sender, "generales.jugador-desconocido");
                    return true;
                }
                DuelMatch match = this.plugin.duels().match(target.getUniqueId());
                if (match == null) {
                    send(sender, "duelos.sin-peticion");
                    return true;
                }
                this.plugin.duels().spectate(player, match);
            }
            case "info" -> {
                DuelMatch match = this.plugin.duels().match(player.getUniqueId());
                if (match == null) {
                    send(sender, "duelos.sin-peticion");
                    return true;
                }
                sender.sendMessage(Cc.mm("<#BFC4CC>Partida: <#FFD1DC>" + match.kitName()
                        + " <dark_gray>| <#BFC4CC>Arena: <#E8E8F0>" + match.arenaName()
                        + " <dark_gray>| <#BFC4CC>Estado: <#FFF5BA>" + match.state().display()
                        + " <dark_gray>| <#BFC4CC>Marcador: <#E8E8F0>" + match.scoreLine()));
            }
            default -> {
                // /duel <jugador> [kit] [rondas]
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null) {
                    send(sender, "generales.jugador-desconocido");
                    return true;
                }
                Kit kit = args.length >= 2 ? kit(args[1]) : defaultKit(player);
                if (kit == null) {
                    send(sender, "kits.no-existe", "kit", args.length >= 2 ? args[1] : "?");
                    return true;
                }
                int rounds = this.plugin.settings().defaultRounds;
                if (args.length >= 3) {
                    try {
                        rounds = Math.max(1, Math.min(9, Integer.parseInt(args[2])));
                    } catch (NumberFormatException ignored) {
                        rounds = this.plugin.settings().defaultRounds;
                    }
                }
                String error = this.plugin.duels().request(player, target, kit, rounds);
                if (error != null) {
                    send(sender, error);
                }
            }
        }
        return true;
    }

    private Kit defaultKit(Player player) {
        Kit last = this.plugin.kits().get("nodebuff");
        return last != null ? last : this.plugin.kits().all().iterator().next();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(onlineNames());
            options.addAll(SUBS);
            return filter(options, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("queue") || args[0].equalsIgnoreCase("cola")) {
                return filter(kitListNames(), args[1]);
            }
            if (args[0].equalsIgnoreCase("espectar")) {
                return filter(onlineNames(), args[1]);
            }
            return filter(kitListNames(), args[1]);
        }
        if (args.length == 3) {
            return filter(List.of("1", "3", "5"), args[2]);
        }
        return new ArrayList<>();
    }
}
