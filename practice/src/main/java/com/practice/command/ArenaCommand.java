package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.util.Cc;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Administracion de arenas: /arena
 */
public final class ArenaCommand extends BaseCommand {

    private static final List<String> SUBS = List.of("crear", "pos1", "pos2", "spawn1", "spawn2", "espectador",
            "tipo", "guardar", "borrar", "lista", "tp", "enable", "disable", "construir");

    public ArenaCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        if (!hasPermission(sender, "practice.admin")) {
            return true;
        }
        if (args.length == 0) {
            usage(sender, "/arena <crear|pos1|pos2|spawn1|spawn2|espectador|tipo|guardar|borrar|lista|tp>");
            return true;
        }
        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        if (sub.equals("lista")) {
            sendRaw(sender, "arenas.lista-header");
            for (Arena arena : this.plugin.arenas().all()) {
                String state = !arena.enabled() ? "deshabilitada" : (arena.inUse() ? "en partida" : "disponible");
                sendRaw(sender, "arenas.lista-item", "arena", arena.name(), "tipo", arena.type(), "estado", state);
            }
            return true;
        }
        if (sub.equals("crear")) {
            if (args.length < 2) {
                usage(sender, "/arena crear <nombre>");
                return true;
            }
            String name = args[1].toLowerCase(java.util.Locale.ROOT);
            if (this.plugin.arenas().exists(name)) {
                send(sender, "arenas.ya-existe");
                return true;
            }
            Arena arena = this.plugin.arenas().create(name);
            arena.worldName(player.getWorld().getName());
            send(sender, "arenas.creada", "arena", name);
            return true;
        }
        if (sub.equals("borrar")) {
            if (args.length < 2) {
                usage(sender, "/arena borrar <nombre>");
                return true;
            }
            if (!this.plugin.arenas().exists(args[1])) {
                send(sender, "arenas.no-existe");
                return true;
            }
            this.plugin.arenas().delete(args[1]);
            this.plugin.arenas().save();
            send(sender, "arenas.borrada", "arena", args[1]);
            return true;
        }
        if (sub.equals("tp")) {
            if (args.length < 2) {
                usage(sender, "/arena tp <nombre>");
                return true;
            }
            Arena arena = this.plugin.arenas().get(args[1]);
            if (arena == null || arena.spawn1() == null) {
                send(sender, "arenas.no-existe");
                return true;
            }
            player.teleport(arena.spawn1());
            send(sender, "arenas.teletransporte", "arena", arena.name());
            return true;
        }
        // El resto de subcomandos requieren una arena en edicion
        if (this.plugin.arenas().count() == 0) {
            send(sender, "arenas.no-existe");
            return true;
        }
        Arena arena = args.length >= 2 ? this.plugin.arenas().get(args[1]) : this.plugin.arenas().get(lastEdited(player));
        if (arena == null) {
            send(sender, "arenas.no-existe");
            return true;
        }
        switch (sub) {
            case "pos1" -> {
                arena.corner1(player.getLocation());
                editar(player, arena.name());
                send(sender, "arenas.pos-establecida", "pos", "Posicion 1", "arena", arena.name());
            }
            case "pos2" -> {
                arena.corner2(player.getLocation());
                editar(player, arena.name());
                send(sender, "arenas.pos-establecida", "pos", "Posicion 2", "arena", arena.name());
            }
            case "spawn1" -> {
                arena.spawn1(player.getLocation());
                editar(player, arena.name());
                send(sender, "arenas.pos-establecida", "pos", "Spawn 1", "arena", arena.name());
            }
            case "spawn2" -> {
                arena.spawn2(player.getLocation());
                editar(player, arena.name());
                send(sender, "arenas.pos-establecida", "pos", "Spawn 2", "arena", arena.name());
            }
            case "espectador" -> {
                arena.spectator(player.getLocation());
                editar(player, arena.name());
                send(sender, "arenas.pos-establecida", "pos", "Espectador", "arena", arena.name());
            }
            case "tipo" -> {
                if (args.length < 3) {
                    usage(sender, "/arena tipo <arena> <SOLO|DUO|TEAM|FFA>");
                    return true;
                }
                arena.type(args[2]);
                this.plugin.arenas().save();
                player.sendMessage(Cc.mm("<#C1E1C1>Arena <#FFD1DC>" + arena.name() + "</#FFD1DC> ahora es de tipo <#FFF5BA>"
                        + arena.type()));
            }
            case "construir" -> {
                arena.buildAllowed(!arena.buildAllowed());
                this.plugin.arenas().save();
                player.sendMessage(Cc.mm("<#C1E1C1>Construccion en la arena <#FFD1DC>" + arena.name() + "</#FFD1DC>: <#FFF5BA>"
                        + (arena.buildAllowed() ? "permitida" : "bloqueada")));
            }
            case "enable", "disable" -> {
                arena.enabled(sub.equals("enable"));
                this.plugin.arenas().save();
                player.sendMessage(Cc.mm("<#C1E1C1>Arena <#FFD1DC>" + arena.name() + "</#FFD1DC> "
                        + (arena.enabled() ? "<#C1E1C1>habilitada" : "<#FFB3BA>deshabilitada")));
            }
            case "guardar" -> {
                String missing = arena.missing();
                if (missing != null) {
                    send(sender, "arenas.incompleta", "falta", missing);
                    return true;
                }
                this.plugin.arenas().save();
                send(sender, "arenas.guardada", "arena", arena.name());
            }
            default -> usage(sender, "/arena <crear|pos1|pos2|spawn1|spawn2|espectador|tipo|guardar|borrar|lista|tp>");
        }
        return true;
    }

    private final java.util.Map<java.util.UUID, String> editing = new java.util.HashMap<>();

    private void editar(Player player, String arena) {
        this.editing.put(player.getUniqueId(), arena);
    }

    private String lastEdited(Player player) {
        String name = this.editing.get(player.getUniqueId());
        if (name != null) {
            return name;
        }
        return this.plugin.arenas().all().isEmpty() ? null : this.plugin.arenas().all().iterator().next().name();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("crear")) {
                return filter(List.of("arena_nueva"), args[1]);
            }
            return filter(arenaNames(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("tipo")) {
            return filter(List.of("SOLO", "DUO", "TEAM", "FFA"), args[2]);
        }
        return new ArrayList<>();
    }
}
