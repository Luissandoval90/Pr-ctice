package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Amigos persistentes y peticiones en memoria.
 */
public final class FriendManager {

    private final PracticePlugin plugin;
    private final ConcurrentHashMap<UUID, UUID> incoming = new ConcurrentHashMap<>();

    public FriendManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public UUID incoming(UUID target) {
        return this.incoming.get(target);
    }

    public void request(Player from, Player target) {
        if (from.getUniqueId().equals(target.getUniqueId())) {
            this.plugin.messages().send(from, "amigos.no-propio");
            return;
        }
        PlayerProfile fromProfile = this.plugin.profiles().profile(from);
        if (fromProfile.friend(target.getUniqueId())) {
            this.plugin.messages().send(from, "amigos.ya-amigos", "jugador", target.getName());
            return;
        }
        this.incoming.put(target.getUniqueId(), from.getUniqueId());
        this.plugin.messages().send(from, "amigos.enviada", "jugador", target.getName());
        Component accept = Cc.mm("<#C1E1C1>[Aceptar]").clickEvent(ClickEvent.runCommand("/amigo aceptar"));
        Component deny = Cc.mm("<#FFB3BA>[Rechazar]").clickEvent(ClickEvent.runCommand("/amigo rechazar"));
        target.sendMessage(Cc.mm("<#E6D5F7>" + from.getName() + " <#BFC4CC>quiere ser tu amigo."));
        target.sendMessage(accept.append(Component.text(" ")).append(deny));
    }

    public void accept(Player target) {
        UUID fromId = this.incoming.remove(target.getUniqueId());
        if (fromId == null) {
            this.plugin.messages().send(target, "amigos.sin-peticion");
            return;
        }
        PlayerProfile targetProfile = this.plugin.profiles().profile(target);
        PlayerProfile fromProfile = this.plugin.profiles().profileOrCreate(fromId, this.plugin.duels().nameOf(fromId));
        targetProfile.addFriend(fromId);
        fromProfile.addFriend(target.getUniqueId());
        this.plugin.profiles().markDirty();
        this.plugin.messages().send(target, "amigos.aceptada", "jugador", fromProfile.name());
        Player from = Bukkit.getPlayer(fromId);
        if (from != null) {
            this.plugin.messages().send(from, "amigos.aceptada", "jugador", target.getName());
        }
    }

    public void deny(Player target) {
        UUID fromId = this.incoming.remove(target.getUniqueId());
        if (fromId == null) {
            this.plugin.messages().send(target, "amigos.sin-peticion");
            return;
        }
        this.plugin.messages().send(target, "amigos.rechazada");
    }

    public void remove(Player player, PlayerProfile other) {
        this.plugin.profiles().profile(player).removeFriend(other.uuid());
        other.removeFriend(player.getUniqueId());
        this.plugin.profiles().markDirty();
        this.plugin.messages().send(player, "amigos.eliminado", "jugador", other.name());
    }

    public void announceJoin(Player player) {
        Component message = Cc.mm("<#C1E1C1>✦ <#E8E8F0>" + player.getName() + " <#BFC4CC>se ha conectado");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (this.plugin.profiles().profile(online).friend(player.getUniqueId())) {
                online.sendMessage(message);
            }
        }
    }
}
