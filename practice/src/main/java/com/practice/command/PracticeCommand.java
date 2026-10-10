package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Comando de administracion: /practice
 */
public final class PracticeCommand extends BaseCommand {

    private static final List<String> SUBS = List.of("reload", "setspawn", "spawn", "fly", "elo", "stats",
            "save", "info", "arenas", "clearchat", "build", "temporada", "monedas", "holo");

    public PracticeCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(Cc.mm("<gradient:#FFD1DC:#B0E0E6><bold>PracticeMC</bold></gradient> "
                    + "<#BFC4CC>v" + this.plugin.getPluginMeta().getVersion()));
            sender.sendMessage(Cc.mm("<#BFC4CC>Kits: <#FFD1DC>" + this.plugin.kits().all().size()
                    + " <dark_gray>| <#BFC4CC>Arenas: <#FFD1DC>" + this.plugin.arenas().count()
                    + " <dark_gray>| <#BFC4CC>Partidas: <#FFD1DC>" + this.plugin.duels().activeMatches()
                    + " <dark_gray>| <#BFC4CC>FFA: <#FFD1DC>" + this.plugin.ffa().total()
                    + " <dark_gray>| <#BFC4CC>Cola: <#FFD1DC>" + this.plugin.queue().waitingPlayers()));
            sender.sendMessage(Cc.mm("<#BFC4CC>Perfiles cargados: <#FFF5BA>" + this.plugin.profiles().size()));
            sender.sendMessage(Cc.mm("<#BFC4CC>Web/IP: <#C1E1C1>" + this.plugin.settings().serverIp));
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "reload" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                this.plugin.reloadAll();
                send(sender, "generales.recargado");
            }
            case "save" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                this.plugin.profiles().saveAsync();
                this.plugin.kits().save();
                this.plugin.arenas().save();
                send(sender, "generales.guardado");
            }
            case "setspawn" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    send(sender, "generales.jugador-desconocido");
                    return true;
                }
                this.plugin.lobby().spawn(player.getLocation());
                send(sender, "generales.spawn-establecido");
            }
            case "spawn" -> {
                if (!(sender instanceof Player player)) {
                    return true;
                }
                this.plugin.lobby().sendToLobby(player);
                send(sender, "generales.teleport-spawn");
            }
            case "fly" -> {
                if (!hasPermission(sender, "practice.fly")) {
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    return true;
                }
                boolean flying = !player.getAllowFlight();
                player.setAllowFlight(flying);
                if (!flying) {
                    player.setFlying(false);
                }
                send(sender, "generales.volando", "estado", flying ? "activado" : "desactivado");
            }
            case "elo" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (args.length < 3) {
                    usage(sender, "/practice elo <jugador> <valor>");
                    return true;
                }
                PlayerProfile profile = this.plugin.profiles().offline(args[1]);
                if (profile == null) {
                    send(sender, "generales.jugador-no-encontrado");
                    return true;
                }
                try {
                    int value = Integer.parseInt(args[2]);
                    profile.elo(value);
                    this.plugin.profiles().markDirty();
                    send(sender, "stats.elo-cambiado", "jugador", profile.name(), "elo", value);
                } catch (NumberFormatException exception) {
                    usage(sender, "/practice elo <jugador> <valor>");
                }
            }
            case "stats" -> {
                if (!hasPermission(sender, "practice.staff")) {
                    return true;
                }
                if (args.length < 2) {
                    usage(sender, "/practice stats <jugador>");
                    return true;
                }
                PlayerProfile profile = this.plugin.profiles().offline(args[1]);
                if (profile == null) {
                    send(sender, "stats.sin-datos");
                    return true;
                }
                sendRaw(sender, "stats.header", "jugador", profile.name());
                sendRaw(sender, "stats.linea", "nombre", "ELO", "valor", profile.elo());
                sendRaw(sender, "stats.linea", "nombre", "Kills", "valor", profile.kills());
                sendRaw(sender, "stats.linea", "nombre", "Muertes", "valor", profile.deaths());
                sendRaw(sender, "stats.linea", "nombre", "Victorias", "valor", profile.wins());
                sendRaw(sender, "stats.linea", "nombre", "Derrotas", "valor", profile.losses());
                sendRaw(sender, "stats.linea", "nombre", "K/D", "valor", Cc.decimal(profile.kd()));
                sendRaw(sender, "stats.linea", "nombre", "Mejor racha", "valor", profile.bestStreak());
                sendRaw(sender, "stats.linea", "nombre", "Tiempo jugado", "valor", profile.formattedPlaytime());
            }
            case "arenas" -> {
                if (!hasPermission(sender, "practice.staff")) {
                    return true;
                }
                sendRaw(sender, "arenas.lista-header");
                for (Arena arena : this.plugin.arenas().all()) {
                    String state = !arena.enabled() ? "deshabilitada" : (arena.inUse() ? "en partida" : "disponible");
                    sendRaw(sender, "arenas.lista-item", "arena", arena.name(), "tipo", arena.type(), "estado", state);
                }
            }
            case "build" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (sender instanceof Player player) {
                    player.setGameMode(player.getGameMode() == org.bukkit.GameMode.CREATIVE
                            ? org.bukkit.GameMode.ADVENTURE : org.bukkit.GameMode.CREATIVE);
                    send(sender, "generales.volando", "estado", player.getGameMode().name());
                }
            }
            case "temporada", "season" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (args.length < 2 || !args[1].equalsIgnoreCase("reset")) {
                    usage(sender, "/practice temporada reset");
                    return true;
                }
                int affected = this.plugin.profiles().resetSeason();
                Bukkit.broadcast(Cc.mm("<gradient:#FFD1DC:#B0E0E6><bold>Nueva temporada</bold></gradient> <#BFC4CC>El ELO de <#FFF5BA>"
                        + affected + " <#BFC4CC>jugadores vuelve a <#E8E8F0>" + this.plugin.settings().eloStart));
            }
            case "monedas", "coins" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (args.length < 3) {
                    usage(sender, "/practice monedas <jugador> <cantidad>");
                    return true;
                }
                com.practice.model.PlayerProfile profile = this.plugin.profiles().offline(args[1]);
                if (profile == null) {
                    send(sender, "generales.jugador-no-encontrado");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    if (args.length >= 4 && args[3].equalsIgnoreCase("add")) {
                        profile.addCoins(amount);
                    } else {
                        profile.coins(amount);
                    }
                    this.plugin.profiles().markDirty();
                    sender.sendMessage(Cc.mm("<#C1E1C1>Monedas de <#FFD1DC>" + profile.name() + " <#C1E1C1>: <#FFF5BA>" + profile.coins()));
                } catch (NumberFormatException exception) {
                    usage(sender, "/practice monedas <jugador> <cantidad>");
                }
            }
            case "holo" -> {
                if (!hasPermission(sender, "practice.admin")) {
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    return true;
                }
                this.plugin.holograms().anchor(player.getLocation().add(0, 2.2D, 0));
                sender.sendMessage(Cc.mm("<#C1E1C1>Holograma de clasificacion colocado."));
            }
            case "clearchat" -> {
                if (!hasPermission(sender, "practice.staff")) {
                    return true;
                }
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.hasPermission("practice.staff")) {
                        for (int i = 0; i < 60; i++) {
                            online.sendMessage(net.kyori.adventure.text.Component.empty());
                        }
                    }
                }
                Bukkit.broadcast(Cc.mm("<#BFC4CC>El chat ha sido limpiado por <#FFD1DC>" + sender.getName()));
            }
            default -> usage(sender, "/practice <reload|setspawn|spawn|fly|elo|stats|save|info|arenas|clearchat|build>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("elo") || args[0].equalsIgnoreCase("stats"))) {
            return filter(onlineNames(), args[1]);
        }
        return new ArrayList<>();
    }
}
