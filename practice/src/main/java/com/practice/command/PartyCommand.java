package com.practice.command;

import com.practice.PracticePlugin;
import com.practice.model.Kit;
import com.practice.model.Party;
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
 * Comando de parties: /party
 */
public final class PartyCommand extends BaseCommand {

    private static final List<String> SUBS = List.of("crear", "invitar", "aceptar", "rechazar", "salir", "expulsar",
            "disolver", "lista", "chat", "duelo", "cola", "abrir");

    public PartyCommand(PracticePlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "generales.jugador-desconocido");
            return true;
        }
        Party party = this.plugin.parties().of(player.getUniqueId());
        if (args.length == 0) {
            this.plugin.menus().openParty(player);
            return true;
        }
        switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
            case "crear", "create" -> {
                if (party != null) {
                    send(sender, "parties.ya-en-party");
                    return true;
                }
                this.plugin.parties().create(player);
                send(sender, "parties.creada");
                this.plugin.tab().updateName(player);
            }
            case "invitar", "invite" -> {
                if (party == null) {
                    send(sender, "parties.no-en-party");
                    return true;
                }
                if (args.length < 2) {
                    this.plugin.menus().openInvite(player);
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    send(sender, "generales.jugador-desconocido");
                    return true;
                }
                if (party.size() >= com.practice.manager.PartyManager.MAX_SIZE) {
                    send(sender, "parties.llena", "max", com.practice.manager.PartyManager.MAX_SIZE);
                    return true;
                }
                if (!this.plugin.profiles().profile(target).partyInvites()) {
                    send(sender, "parties.invitaciones-off", "jugador", target.getName());
                    return true;
                }
                if (!party.invite(target.getUniqueId())) {
                    send(sender, "parties.ya-en-party");
                    return true;
                }
                send(sender, "parties.invitacion-enviada", "jugador", target.getName());
                this.plugin.messages().sendList(target, "parties.invitacion-recibida", "duenio",
                        this.plugin.parties().ownerName(party));
                target.sendMessage(com.practice.util.Cc.mm("<#C1E1C1>[Aceptar]").clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party aceptar"))
                        .append(net.kyori.adventure.text.Component.text(" "))
                        .append(com.practice.util.Cc.mm("<#FFB3BA>[Rechazar]").clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party rechazar"))));
            }
            case "aceptar", "accept", "unir" -> {
                if (party != null) {
                    send(sender, "parties.ya-en-party");
                    return true;
                }
                Party found = null;
                for (Party candidate : this.plugin.parties().all()) {
                    if (candidate.invites().contains(player.getUniqueId())) {
                        found = candidate;
                        break;
                    }
                }
                if (found == null) {
                    send(sender, "parties.invitacion-expirada");
                    return true;
                }
                if (found.size() >= com.practice.manager.PartyManager.MAX_SIZE) {
                    send(sender, "parties.llena", "max", com.practice.manager.PartyManager.MAX_SIZE);
                    return true;
                }
                this.plugin.parties().join(found, player);
                this.plugin.parties().broadcast(found, this.plugin.messages().get("parties.invitacion-aceptada",
                        "jugador", player.getName()));
                this.plugin.tab().updateName(player);
            }
            case "rechazar", "deny" -> {
                for (Party candidate : this.plugin.parties().all()) {
                    candidate.removeInvite(player.getUniqueId());
                }
                send(sender, "parties.invitacion-rechazada");
            }
            case "salir", "leave" -> {
                if (party == null) {
                    send(sender, "parties.no-en-party");
                    return true;
                }
                if (party.isOwner(player.getUniqueId())) {
                    this.plugin.parties().broadcast(party, Cc.mm(this.plugin.messages().raw("parties.disuelta")));
                    this.plugin.parties().disband(party);
                } else {
                    this.plugin.parties().leave(player);
                    this.plugin.parties().broadcast(party, this.plugin.messages().get("parties.salio",
                            "jugador", player.getName()));
                }
                this.plugin.tab().updateName(player);
            }
            case "expulsar", "kick" -> {
                if (party == null) {
                    send(sender, "parties.no-en-party");
                    return true;
                }
                if (!party.isOwner(player.getUniqueId())) {
                    send(sender, "parties.solo-duenio");
                    return true;
                }
                if (args.length < 2) {
                    usage(sender, "/party expulsar <jugador>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null || !party.isMember(target.getUniqueId())) {
                    send(sender, "generales.jugador-desconocido");
                    return true;
                }
                this.plugin.parties().kick(party, target.getUniqueId());
                send(sender, "parties.expulsar-ok", "jugador", target.getName());
                this.plugin.messages().send(target, "parties.expulsado");
                this.plugin.tab().updateName(target);
            }
            case "disolver", "disband" -> {
                if (party == null) {
                    send(sender, "parties.no-en-party");
                    return true;
                }
                if (!party.isOwner(player.getUniqueId())) {
                    send(sender, "parties.solo-duenio");
                    return true;
                }
                this.plugin.parties().broadcast(party, Cc.mm(this.plugin.messages().raw("parties.disuelta")));
                this.plugin.parties().disband(party);
            }
            case "lista", "list" -> {
                if (party == null) {
                    send(sender, "parties.no-en-party");
                    return true;
                }
                sendRaw(sender, "parties.lista-header", "duenio", this.plugin.parties().ownerName(party));
                for (UUID member : party.members()) {
                    PlayerProfile profile = this.plugin.profiles().profileOrCreate(member, "Jugador");
                    sendRaw(sender, "parties.lista-miembro", "nombre", profile.name(), "elo", profile.elo());
                }
            }
            case "chat", "c" -> {
                if (party == null) {
                    send(sender, "parties.no-en-party");
                    return true;
                }
                if (args.length < 2) {
                    usage(sender, "/party chat <mensaje>");
                    return true;
                }
                StringBuilder builder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    builder.append(args[i]).append(' ');
                }
                net.kyori.adventure.text.Component message = Cc.mm(this.plugin.messages().raw("parties.chat"),
                        "jugador", player.getName(), "mensaje", builder.toString().trim());
                for (Player member : this.plugin.parties().onlineMembers(party)) {
                    member.sendMessage(message);
                }
            }
            case "duelo", "duel", "cola", "queue" -> {
                if (party == null) {
                    send(sender, "parties.no-en-party");
                    return true;
                }
                if (!party.isOwner(player.getUniqueId())) {
                    send(sender, "parties.solo-duenio");
                    return true;
                }
                Kit kit = args.length >= 2 ? kit(args[1]) : this.plugin.kits().get("team_sumo");
                if (kit == null) {
                    kit = this.plugin.kits().get("team_nodebuff");
                }
                if (kit == null) {
                    send(sender, "kits.no-existe", "kit", "team");
                    return true;
                }
                String error = this.plugin.queue().join(player, kit);
                if (error != null) {
                    send(sender, error);
                    return true;
                }
                this.plugin.parties().broadcast(party, this.plugin.messages().get("parties.duelo-iniciado"));
            }
            case "abrir", "open" -> {
                if (party == null) {
                    send(sender, "parties.no-en-party");
                    return true;
                }
                if (!party.isOwner(player.getUniqueId())) {
                    send(sender, "parties.solo-duenio");
                    return true;
                }
                party.open(!party.open());
                for (Player member : this.plugin.parties().onlineMembers(party)) {
                    member.sendMessage(Cc.mm("<#BFC4CC>Party " + (party.open() ? "<#C1E1C1>abierta" : "<#FFB3BA>cerrada")));
                }
            }
            default -> usage(sender, "/party <crear|invitar|aceptar|rechazar|salir|expulsar|disolver|lista|chat|duelo>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(java.util.Locale.ROOT);
            if (sub.equals("invitar") || sub.equals("expulsar")) {
                return filter(onlineNames(), args[1]);
            }
            if (sub.equals("duelo") || sub.equals("cola")) {
                return filter(List.of("team_sumo", "team_nodebuff", "duo_nodebuff"), args[1]);
            }
        }
        return new ArrayList<>();
    }
}
