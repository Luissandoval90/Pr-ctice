package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Party;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona las parties (grupos) del servidor.
 * <p>
 * Todas las operaciones son O(1) gracias a un indice jugador -> party.
 */
public final class PartyManager {

    public static final int MAX_SIZE = 16;

    private final PracticePlugin plugin;
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>(64);
    private final Map<UUID, Party> byMember = new ConcurrentHashMap<>(256);

    public PartyManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public Party of(UUID uuid) {
        return this.byMember.get(uuid);
    }

    public Party of(Player player) {
        return of(player.getUniqueId());
    }

    public boolean inParty(UUID uuid) {
        return this.byMember.containsKey(uuid);
    }

    public Collection<Party> all() {
        return this.parties.values();
    }

    public Party create(Player owner) {
        if (inParty(owner.getUniqueId())) {
            return null;
        }
        Party party = new Party(owner.getUniqueId());
        this.parties.put(party.id(), party);
        this.byMember.put(owner.getUniqueId(), party);
        return party;
    }

    public void disband(Party party) {
        if (party == null) {
            return;
        }
        for (UUID member : party.members()) {
            this.byMember.remove(member);
        }
        party.invites().clear();
        this.parties.remove(party.id());
    }

    public void join(Party party, Player player) {
        party.add(player.getUniqueId());
        this.byMember.put(player.getUniqueId(), party);
    }

    public void leave(Player player) {
        Party party = of(player.getUniqueId());
        if (party == null) {
            return;
        }
        party.remove(player.getUniqueId());
        this.byMember.remove(player.getUniqueId());
        if (party.empty() || party.size() <= 0) {
            disband(party);
        }
    }

    public void kick(Party party, UUID uuid) {
        party.remove(uuid);
        this.byMember.remove(uuid);
        if (party.empty()) {
            disband(party);
        }
    }

    /** Miembros de la party como jugadores conectados. */
    public List<Player> onlineMembers(Party party) {
        List<Player> list = new ArrayList<>(party.size());
        for (UUID uuid : party.members()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                list.add(player);
            }
        }
        return list;
    }

    public List<Player> onlineMembers(UUID uuid) {
        Party party = of(uuid);
        return party == null ? new ArrayList<>() : onlineMembers(party);
    }

    /** ELO medio de la party (para el emparejamiento). */
    public int averageElo(Party party) {
        int total = 0;
        int count = 0;
        for (UUID uuid : party.members()) {
            total += this.plugin.profiles().profileOrCreate(uuid, "Jugador").elo();
            count++;
        }
        return count == 0 ? this.plugin.settings().eloStart : total / count;
    }

    /** Envia un mensaje a todos los miembros de una party. */
    public void broadcast(Party party, net.kyori.adventure.text.Component message) {
        for (Player player : onlineMembers(party)) {
            player.sendMessage(message);
        }
    }

    /** Nombre del duenio de la party. */
    public String ownerName(Party party) {
        Player owner = Bukkit.getPlayer(party.owner());
        if (owner != null) {
            return owner.getName();
        }
        var profile = this.plugin.profiles().profile(party.owner());
        return profile == null ? "Desconocido" : profile.name();
    }
}
