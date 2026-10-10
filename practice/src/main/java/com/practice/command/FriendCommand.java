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
import java.util.UUID;

/**
 * Amigos: /amigo
 */
public final class FriendCommand extends BaseCommand {

    private static final List<String> SUBS = List.of("agregar", "aceptar", "rechazar", "eliminar", "lista");

    public FriendCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("lista")) {
            list(player);
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "agregar", "add" -> {
                if (args.length < 2) {
                    usage(sender, "/amigo agregar <jugador>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    send(sender, "generales.jugador-desconocido");
                    return true;
                }
                this.plugin.friends().request(player, target);
            }
            case "aceptar", "accept" -> this.plugin.friends().accept(player);
            case "rechazar", "deny" -> this.plugin.friends().deny(player);
            case "eliminar", "remove" -> {
                if (args.length < 2) {
                    usage(sender, "/amigo eliminar <jugador>");
                    return true;
                }
                PlayerProfile other = this.plugin.profiles().offline(args[1]);
                if (other == null) {
                    send(sender, "generales.jugador-no-encontrado");
                    return true;
                }
                this.plugin.friends().remove(player, other);
            }
            default -> usage(sender, "/amigo <agregar|aceptar|rechazar|eliminar|lista>");
        }
        return true;
    }

    private void list(Player player) {
        PlayerProfile profile = this.plugin.profiles().profile(player);
        if (profile.friends().isEmpty()) {
            send(player, "amigos.vacio");
            return;
        }
        player.sendMessage(Cc.mm("<gradient:#E6D5F7:#FFD1DC><bold>Amigos</bold></gradient>"));
        for (String raw : profile.friends()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(raw);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            PlayerProfile friend = this.plugin.profiles().profile(uuid);
            String name = friend == null ? "?" : friend.name();
            boolean online = Bukkit.getPlayer(uuid) != null;
            player.sendMessage(Cc.mm("<#BFC4CC>• " + (online ? "<#C1E1C1>" : "<#E8E8F0>") + name
                    + " <dark_gray>(" + (online ? "online" : "offline") + ")"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (args.length == 2) {
            return filter(onlineNames(), args[1]);
        }
        return new ArrayList<>();
    }
}
