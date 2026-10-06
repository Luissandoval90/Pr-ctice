package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.Kit;
import com.practice.model.MatchType;
import com.practice.util.Cc;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando de kits: /kit
 */
public final class KitCommand extends BaseCommand {

    private static final List<String> SUBS = List.of("lista", "editor", "crear", "borrar", "tipo", "rondas", "descripcion");

    public KitCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("lista")) {
            sendRaw(sender, "kits.lista-header");
            for (Kit kit : this.plugin.kits().all()) {
                sendRaw(sender, "kits.lista-item", "kit", kit.name(), "descripcion", kit.description());
            }
            return true;
        }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        switch (sub) {
            case "editor", "menu" -> this.plugin.menus().openKits(player);
            case "crear" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (args.length < 2) {
                    usage(sender, "/kit crear <nombre>");
                    return true;
                }
                String name = args[1].toLowerCase(java.util.Locale.ROOT);
                if (this.plugin.kits().exists(name)) {
                    send(sender, "kits.creado", "kit", name);
                    return true;
                }
                this.plugin.kits().createFrom(player, name);
                this.plugin.kits().save();
                send(sender, "kits.creado", "kit", name);
            }
            case "borrar" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (args.length < 2) {
                    usage(sender, "/kit borrar <nombre>");
                    return true;
                }
                this.plugin.kits().delete(args[1]);
                this.plugin.kits().save();
                send(sender, "kits.borrado", "kit", args[1]);
            }
            case "tipo" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (args.length < 3) {
                    usage(sender, "/kit tipo <kit> <SOLO|DUO|TEAM|FFA>");
                    return true;
                }
                Kit kit = kit(args[1]);
                if (kit == null) {
                    send(sender, "kits.no-existe", "kit", args[1]);
                    return true;
                }
                kit.type(MatchType.fromId(args[2]));
                this.plugin.kits().save();
                sender.sendMessage(Cc.mm("<#C1E1C1>Kit <#FFD1DC>" + kit.name() + "</#FFD1DC> ahora es de tipo <#FFF5BA>"
                        + kit.type().name()));
            }
            case "rondas" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (args.length < 3) {
                    usage(sender, "/kit rondas <kit> <cantidad>");
                    return true;
                }
                Kit kit = kit(args[1]);
                if (kit == null) {
                    send(sender, "kits.no-existe", "kit", args[1]);
                    return true;
                }
                try {
                    kit.maxRounds(Math.max(0, Integer.parseInt(args[2])));
                    this.plugin.kits().save();
                    sender.sendMessage(Cc.mm("<#C1E1C1>Rondas del kit <#FFD1DC>" + kit.name() + "</#FFD1DC>: <#FFF5BA>"
                            + (kit.maxRounds() == 0 ? "por defecto" : String.valueOf(kit.maxRounds()))));
                } catch (NumberFormatException exception) {
                    usage(sender, "/kit rondas <kit> <cantidad>");
                }
            }
            case "descripcion" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (args.length < 3) {
                    usage(sender, "/kit descripcion <kit> <texto>");
                    return true;
                }
                Kit kit = kit(args[1]);
                if (kit == null) {
                    send(sender, "kits.no-existe", "kit", args[1]);
                    return true;
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    builder.append(args[i]).append(' ');
                }
                kit.description(builder.toString().trim());
                this.plugin.kits().save();
                sender.sendMessage(Cc.mm("<#C1E1C1>Descripcion actualizada."));
            }
            default -> {
                // /kit <nombre>
                Kit kit = kit(args[0]);
                if (kit == null) {
                    send(sender, "kits.no-existe", "kit", args[0]);
                    return true;
                }
                if (!kit.permission().isEmpty() && !player.hasPermission(kit.permission())) {
                    send(sender, "generales.sin-permiso");
                    return true;
                }
                if (kit.cooldown() > 0 && this.plugin.duels().inMatch(player.getUniqueId())) {
                    send(sender, "kits.en-cooldown", "tiempo", kit.cooldown() + "s");
                    return true;
                }
                if (this.plugin.duels().inMatch(player.getUniqueId()) || this.plugin.ffa().inFfa(player.getUniqueId())) {
                    send(sender, "generales.en-partida");
                    return true;
                }
                com.practice.util.PlayerState.giveKit(player, kit);
                send(sender, "kits.dado", "kit", kit.name());
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(SUBS);
            options.addAll(kitListNames());
            return filter(options, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(java.util.Locale.ROOT);
            if (sub.equals("tipo")) {
                return filter(kitListNames(), args[1]);
            }
            if (sub.equals("crear")) {
                return filter(List.of("nuevo_kit"), args[1]);
            }
            return filter(kitListNames(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("tipo")) {
            return filter(List.of("SOLO", "DUO", "TEAM", "FFA"), args[2]);
        }
        return new ArrayList<>();
    }
}
