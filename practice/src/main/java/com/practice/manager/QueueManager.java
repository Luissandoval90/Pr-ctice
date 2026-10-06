package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Kit;
import com.practice.model.MatchType;
import com.practice.model.Party;
import com.practice.model.PlayerProfile;
import com.practice.util.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cola de emparejamiento (matchmaking) por kit.
 * <p>
 * Optimizacion: las colas estan indexadas por kit y el emparejamiento se
 * resuelve con un unico recorrido ordenado por tiempo de espera.
 */
public final class QueueManager {

    public static final int MAX_ELO_DIFFERENCE = 250;

    /** Entrada de la cola (uno o varios jugadores). */
    public static final class Entry {
        private final UUID leader;
        private final List<UUID> members;
        private final String kit;
        private final MatchType type;
        private final long joinedAt;
        private final int averageElo;

        Entry(UUID leader, List<UUID> members, String kit, MatchType type, int averageElo) {
            this.leader = leader;
            this.members = members;
            this.kit = kit;
            this.type = type;
            this.joinedAt = System.currentTimeMillis();
            this.averageElo = averageElo;
        }

        public UUID leader() {
            return this.leader;
        }

        public List<UUID> members() {
            return this.members;
        }

        public String kit() {
            return this.kit;
        }

        public MatchType type() {
            return this.type;
        }

        public long joinedAt() {
            return this.joinedAt;
        }

        public int averageElo() {
            return this.averageElo;
        }

        public int size() {
            return this.members.size();
        }
    }

    private final PracticePlugin plugin;
    private final Map<String, List<Entry>> queues = new ConcurrentHashMap<>(16);
    private final Map<UUID, Entry> byPlayer = new ConcurrentHashMap<>(128);

    public QueueManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean queued(UUID uuid) {
        return this.byPlayer.containsKey(uuid);
    }

    public Entry entry(UUID uuid) {
        return this.byPlayer.get(uuid);
    }

    public String kitOf(UUID uuid) {
        Entry entry = this.byPlayer.get(uuid);
        return entry == null ? "" : entry.kit();
    }

    public long joinedAt(UUID uuid) {
        Entry entry = this.byPlayer.get(uuid);
        return entry == null ? System.currentTimeMillis() : entry.joinedAt();
    }

    public int size(String kit) {
        List<Entry> list = this.queues.get(kit.toLowerCase(java.util.Locale.ROOT));
        if (list == null) {
            return 0;
        }
        int total = 0;
        for (Entry entry : list) {
            total += entry.size();
        }
        return total;
    }

    public int waitingPlayers() {
        int total = 0;
        for (List<Entry> list : this.queues.values()) {
            for (Entry entry : list) {
                total += entry.size();
            }
        }
        return total;
    }

    /** Mete al jugador (y su party si aplica) en la cola del kit. */
    public String join(Player player, Kit kit) {
        if (kit == null) {
            return "kits.no-existe";
        }
        if (queued(player.getUniqueId())) {
            return null;
        }
        if (this.plugin.duels().inMatch(player.getUniqueId()) || this.plugin.ffa().inFfa(player.getUniqueId())) {
            return "generales.en-partida";
        }
        List<UUID> members = new ArrayList<>(4);
        if (kit.type() == MatchType.SOLO) {
            members.add(player.getUniqueId());
        } else {
            Party party = this.plugin.parties().of(player.getUniqueId());
            if (party == null) {
                return "parties.no-en-party";
            }
            if (!party.isOwner(player.getUniqueId())) {
                return "parties.solo-duenio";
            }
            for (UUID member : party.members()) {
                if (Bukkit.getPlayer(member) == null) {
                    continue;
                }
                if (this.plugin.duels().inMatch(member) || this.plugin.ffa().inFfa(member)) {
                    return "generales.en-partida";
                }
                members.add(member);
            }
            if (members.size() < 2) {
                return "parties.no-en-party";
            }
        }
        int averageElo = 0;
        for (UUID uuid : members) {
            PlayerProfile profile = this.plugin.profiles().profileOrCreate(uuid, "Jugador");
            averageElo += profile.elo();
        }
        averageElo = averageElo / members.size();
        Entry entry = new Entry(player.getUniqueId(), members, kit.name(), kit.type(), averageElo);
        this.queues.computeIfAbsent(kit.name(), key -> new ArrayList<>(16)).add(entry);
        for (UUID uuid : members) {
            this.byPlayer.put(uuid, entry);
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                this.plugin.messages().send(member, "duelos.cola-entrada",
                        "kit", kit.name(), "jugadores", size(kit.name()));
                if (this.plugin.settings().sounds) {
                    PlayerState.sound(member, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7F, 1.5F);
                }
            }
        }
        return null;
    }

    /** Saca al jugador (y a su equipo) de la cola. */
    public void leave(Player player) {
        Entry entry = this.byPlayer.get(player.getUniqueId());
        if (entry == null) {
            return;
        }
        remove(entry);
        for (UUID uuid : entry.members()) {
            this.byPlayer.remove(uuid);
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) {
                this.plugin.messages().send(member, "duelos.cola-salida", "kit", entry.kit());
            }
        }
    }

    private void remove(Entry entry) {
        List<Entry> list = this.queues.get(entry.kit());
        if (list != null) {
            list.remove(entry);
        }
    }

    /** Intenta emparejar jugadores de la cola. */
    public void tick() {
        if (this.queues.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<Entry>> queueEntry : this.queues.entrySet()) {
            List<Entry> list = queueEntry.getValue();
            if (list.size() < 2) {
                continue;
            }
            list.sort(Comparator.comparingLong(Entry::joinedAt));
            Entry first = list.get(0);
            Entry partner = null;
            for (int i = 1; i < list.size(); i++) {
                Entry candidate = list.get(i);
                if (candidate.size() != first.size()) {
                    continue;
                }
                if (Math.abs(candidate.averageElo() - first.averageElo()) > maxDifference()) {
                    continue;
                }
                partner = candidate;
                break;
            }
            if (partner == null) {
                continue;
            }
            Kit kit = this.plugin.kits().get(first.kit());
            if (kit == null) {
                continue;
            }
            list.remove(first);
            list.remove(partner);
            for (UUID uuid : first.members()) {
                this.byPlayer.remove(uuid);
            }
            for (UUID uuid : partner.members()) {
                this.byPlayer.remove(uuid);
            }
            List<List<UUID>> teams = new ArrayList<>(2);
            teams.add(first.members());
            teams.add(partner.members());
            MatchType type = first.size() > 1 ? MatchType.TEAM : MatchType.SOLO;
            for (UUID uuid : first.members()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    this.plugin.messages().send(member, "duelos.cola-emparejada", "kit", kit.name());
                }
            }
            for (UUID uuid : partner.members()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    this.plugin.messages().send(member, "duelos.cola-emparejada", "kit", kit.name());
                }
            }
            boolean started = this.plugin.duels().startMatch(type, kit, teams,
                    kit.maxRounds() > 0 ? kit.maxRounds() : this.plugin.settings().defaultRounds);
            if (!started) {
                // devolver a la cola si no hay arena
                this.queues.computeIfAbsent(first.kit(), key -> new ArrayList<>(16)).add(first);
                this.queues.computeIfAbsent(partner.kit(), key -> new ArrayList<>(16)).add(partner);
                for (UUID uuid : first.members()) {
                    this.byPlayer.put(uuid, first);
                }
                for (UUID uuid : partner.members()) {
                    this.byPlayer.put(uuid, partner);
                }
            }
        }
    }

    private int maxDifference() {
        return this.plugin.settings().eloRange > 0 ? this.plugin.settings().eloRange : MAX_ELO_DIFFERENCE;
    }

    /** Kits con jugadores en cola y su cantidad (para el menu). */
    public Map<String, Integer> snapshot() {
        Map<String, Integer> map = new HashMap<>(16);
        for (Map.Entry<String, List<Entry>> entry : this.queues.entrySet()) {
            int total = 0;
            for (Entry queueEntry : entry.getValue()) {
                total += queueEntry.size();
            }
            if (total > 0) {
                map.put(entry.getKey(), total);
            }
        }
        return map;
    }

    public void clear() {
        this.queues.clear();
        this.byPlayer.clear();
    }
}
