package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Leaderboard;
import com.practice.model.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestiona los perfiles y estadisticas de los jugadores.
 * <p>
 * Optimizacion: los perfiles viven en un mapa concurrente, las escrituras a
 * disco se hacen de forma asincrona (nunca en el hilo principal) y el guardado
 * es incremental (solo perfiles modificados).
 */
public final class ProfileManager {

    private final PracticePlugin plugin;
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>(256);
    private final Map<UUID, Long> loginTime = new ConcurrentHashMap<>(128);
    private final File file;
    private YamlConfiguration data;
    private volatile boolean dirty = false;

    public ProfileManager(PracticePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/stats.yml");
    }

    /** Carga todos los perfiles desde disco (una sola lectura al arrancar). */
    public void loadAll() {
        this.profiles.clear();
        if (!this.file.exists()) {
            this.data = new YamlConfiguration();
            return;
        }
        this.data = YamlConfiguration.loadConfiguration(this.file);
        if (this.data.isConfigurationSection("jugadores")) {
            for (String key : this.data.getConfigurationSection("jugadores").getKeys(false)) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(key);
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                PlayerProfile profile = new PlayerProfile(uuid, this.data.getString("jugadores." + key + ".nombre", "Desconocido"));
                profile.elo(this.data.getInt("jugadores." + key + ".elo", this.plugin.settings().eloStart));
                profile.eloPeak(this.data.getInt("jugadores." + key + ".elo-max", profile.elo()));
                profile.kills(this.data.getInt("jugadores." + key + ".kills", 0));
                profile.deaths(this.data.getInt("jugadores." + key + ".muertes", 0));
                profile.wins(this.data.getInt("jugadores." + key + ".victorias", 0));
                profile.losses(this.data.getInt("jugadores." + key + ".derrotas", 0));
                profile.streak(this.data.getInt("jugadores." + key + ".racha", 0));
                profile.bestStreak(this.data.getInt("jugadores." + key + ".mejor-racha", 0));
                profile.ffaKills(this.data.getInt("jugadores." + key + ".ffa-kills", 0));
                profile.ffaDeaths(this.data.getInt("jugadores." + key + ".ffa-muertes", 0));
                profile.ffaBestStreak(this.data.getInt("jugadores." + key + ".ffa-mejor-racha", 0));
                profile.playtimeMillis(this.data.getLong("jugadores." + key + ".tiempo", 0L));
                profile.coins(this.data.getInt("jugadores." + key + ".monedas", 0));
                profile.seasonWins(this.data.getInt("jugadores." + key + ".temporada-victorias", 0));
                profile.seasonLosses(this.data.getInt("jugadores." + key + ".temporada-derrotas", 0));
                profile.killEffect(this.data.getString("jugadores." + key + ".efecto-kill", "ninguno"));
                profile.winEffect(this.data.getString("jugadores." + key + ".efecto-victoria", "ninguno"));
                profile.showBoard(this.data.getBoolean("jugadores." + key + ".ajuste-scoreboard", true));
                profile.duelRequests(this.data.getBoolean("jugadores." + key + ".ajuste-duelos", true));
                profile.partyInvites(this.data.getBoolean("jugadores." + key + ".ajuste-party", true));
                profile.allowSpectators(this.data.getBoolean("jugadores." + key + ".ajuste-espectadores", true));
                profile.personalSounds(this.data.getBoolean("jugadores." + key + ".ajuste-sonidos", true));
                profile.challengeDay(this.data.getString("jugadores." + key + ".desafio-dia", ""));
                if (this.data.isConfigurationSection("jugadores." + key + ".desafios")) {
                    for (String challenge : this.data.getConfigurationSection("jugadores." + key + ".desafios").getKeys(false)) {
                        profile.challengeProgress(challenge, this.data.getInt("jugadores." + key + ".desafios." + challenge, 0));
                    }
                }
                for (String claimed : this.data.getStringList("jugadores." + key + ".desafios-reclamados")) {
                    profile.challengeClaimed().add(claimed);
                }
                for (String friend : this.data.getStringList("jugadores." + key + ".amigos")) {
                    profile.friends().add(friend);
                }
                for (String line : this.data.getStringList("jugadores." + key + ".historial")) {
                    com.practice.model.MatchRecord record = com.practice.model.MatchRecord.decode(line);
                    if (record != null) {
                        profile.history().add(record);
                    }
                }
                if (this.data.isConfigurationSection("jugadores." + key + ".kits")) {
                    for (String kit : this.data.getConfigurationSection("jugadores." + key + ".kits").getKeys(false)) {
                        int[] record = profile.kitRecord(kit);
                        record[0] = this.data.getInt("jugadores." + key + ".kits." + kit + ".victorias", 0);
                        record[1] = this.data.getInt("jugadores." + key + ".kits." + kit + ".derrotas", 0);
                        record[2] = this.data.getInt("jugadores." + key + ".kits." + kit + ".kills", 0);
                    }
                }
                this.profiles.put(uuid, profile);
            }
        }
        this.plugin.getLogger().info("Cargados " + this.profiles.size() + " perfiles de jugadores.");
    }

    /** Devuelve (creando si hace falta) el perfil de un jugador. */
    public PlayerProfile profile(Player player) {
        PlayerProfile profile = this.profiles.get(player.getUniqueId());
        if (profile == null) {
            profile = new PlayerProfile(player.getUniqueId(), player.getName());
            profile.elo(this.plugin.settings().eloStart);
            this.profiles.put(player.getUniqueId(), profile);
            this.dirty = true;
        } else if (!player.getName().equals(profile.name())) {
            profile.name(player.getName());
        }
        this.loginTime.putIfAbsent(player.getUniqueId(), System.currentTimeMillis());
        return profile;
    }

    public PlayerProfile profile(UUID uuid) {
        return this.profiles.get(uuid);
    }

    public PlayerProfile profileOrCreate(UUID uuid, String name) {
        return this.profiles.computeIfAbsent(uuid, key -> {
            PlayerProfile created = new PlayerProfile(key, name);
            created.elo(this.plugin.settings().eloStart);
            this.dirty = true;
            return created;
        });
    }

    public PlayerProfile offline(String name) {
        for (PlayerProfile profile : this.profiles.values()) {
            if (profile.name().equalsIgnoreCase(name)) {
                return profile;
            }
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.getUniqueId() == null ? null : this.profiles.get(offline.getUniqueId());
    }

    public Collection<PlayerProfile> all() {
        return this.profiles.values();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void startSession(UUID uuid) {
        this.loginTime.put(uuid, System.currentTimeMillis());
    }

    /** Cierra la sesion y acumula el tiempo jugado. */
    public void endSession(UUID uuid) {
        Long start = this.loginTime.remove(uuid);
        PlayerProfile profile = this.profiles.get(uuid);
        if (start != null && profile != null) {
            profile.addPlaytime(System.currentTimeMillis() - start);
            this.dirty = true;
        }
    }

    /** Guarda todos los perfiles de forma asincrona. */
    public void saveAsync() {
        if (!this.dirty) {
            return;
        }
        this.dirty = false;
        YamlConfiguration snapshot = snapshot();
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> write(snapshot));
    }

    private YamlConfiguration snapshot() {
        YamlConfiguration snapshot = new YamlConfiguration();
        for (PlayerProfile profile : this.profiles.values()) {
            String path = "jugadores." + profile.uuid() + ".";
            snapshot.set(path + "nombre", profile.name());
            snapshot.set(path + "elo", profile.elo());
            snapshot.set(path + "elo-max", profile.eloPeak());
            snapshot.set(path + "kills", profile.kills());
            snapshot.set(path + "muertes", profile.deaths());
            snapshot.set(path + "victorias", profile.wins());
            snapshot.set(path + "derrotas", profile.losses());
            snapshot.set(path + "racha", profile.streak());
            snapshot.set(path + "mejor-racha", profile.bestStreak());
            snapshot.set(path + "ffa-kills", profile.ffaKills());
            snapshot.set(path + "ffa-muertes", profile.ffaDeaths());
            snapshot.set(path + "ffa-mejor-racha", profile.ffaBestStreak());
            snapshot.set(path + "tiempo", profile.playtimeMillis());
            snapshot.set(path + "monedas", profile.coins());
            snapshot.set(path + "temporada-victorias", profile.seasonWins());
            snapshot.set(path + "temporada-derrotas", profile.seasonLosses());
            snapshot.set(path + "efecto-kill", profile.killEffect());
            snapshot.set(path + "efecto-victoria", profile.winEffect());
            snapshot.set(path + "ajuste-scoreboard", profile.showBoard());
            snapshot.set(path + "ajuste-duelos", profile.duelRequests());
            snapshot.set(path + "ajuste-party", profile.partyInvites());
            snapshot.set(path + "ajuste-espectadores", profile.allowSpectators());
            snapshot.set(path + "ajuste-sonidos", profile.personalSounds());
            snapshot.set(path + "desafio-dia", profile.challengeDay());
            for (java.util.Map.Entry<String, Integer> challenge : profile.challengeProgress().entrySet()) {
                snapshot.set(path + "desafios." + challenge.getKey(), challenge.getValue());
            }
            snapshot.set(path + "desafios-reclamados", new java.util.ArrayList<>(profile.challengeClaimed()));
            snapshot.set(path + "amigos", new java.util.ArrayList<>(profile.friends()));
            java.util.List<String> history = new java.util.ArrayList<>(profile.history().size());
            for (com.practice.model.MatchRecord record : profile.history()) {
                history.add(record.encode());
            }
            snapshot.set(path + "historial", history);
            for (Map.Entry<String, int[]> entry : profile.kitRecords().entrySet()) {
                String kitPath = path + "kits." + entry.getKey() + ".";
                snapshot.set(kitPath + "victorias", entry.getValue()[0]);
                snapshot.set(kitPath + "derrotas", entry.getValue()[1]);
                snapshot.set(kitPath + "kills", entry.getValue()[2]);
            }
        }
        return snapshot;
    }

    private void write(YamlConfiguration snapshot) {
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            snapshot.save(this.file);
        } catch (IOException exception) {
            this.plugin.getLogger().warning("No se pudieron guardar las estadisticas: " + exception.getMessage());
        }
    }

    /** Guardado sincrono (solo en el apagado del servidor, cuando el scheduler ya no corre). */
    public void saveSync() {
        this.dirty = false;
        write(snapshot());
    }

    /** Reinicia el ELO de la temporada sin borrar el historial de por vida. */
    public int resetSeason() {
        for (PlayerProfile profile : this.profiles.values()) {
            profile.elo(this.plugin.settings().eloStart);
            profile.seasonWins(0);
            profile.seasonLosses(0);
            profile.streak(0);
        }
        this.dirty = true;
        return this.profiles.size();
    }

    public List<Leaderboard.Entry> top(Leaderboard.Type type, int limit) {
        return Leaderboard.top(this.profiles.values(), type, limit);
    }

    public int rank(PlayerProfile profile, Leaderboard.Type type) {
        double value = Leaderboard.value(profile, type);
        int position = 1;
        for (PlayerProfile other : this.profiles.values()) {
            if (other == profile) {
                continue;
            }
            if (Leaderboard.value(other, type) > value) {
                position++;
            }
        }
        return position;
    }

    public int size() {
        return this.profiles.size();
    }

    public List<PlayerProfile> onlineProfiles() {
        List<PlayerProfile> list = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = this.profiles.get(player.getUniqueId());
            if (profile != null) {
                list.add(profile);
            }
        }
        return list;
    }
}
