package com.practice.hud;

import com.practice.PracticePlugin;
import com.practice.model.DuelMatch;
import com.practice.model.FfaSession;
import com.practice.model.GameState;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scoreboard lateral (sidebar) con diseno pastel.
 * <p>
 * Optimizacion:
 * <ul>
 *     <li>Un scoreboard propio por jugador creado una sola vez.</li>
 *     <li>Envio diferencial: solo las lineas cuyo texto cambia se reenvian.</li>
 *     <li>Los mensajes se parsean una unica vez por refresco.</li>
 * </ul>
 */
public final class BoardManager {

    /** El sidebar admite como maximo 15 lineas. */
    private static final int MAX_LINES = 15;
    private static final String[] ENTRIES = new String[MAX_LINES];
    private static final ChatColor[] COLORS = ChatColor.values();

    static {
        // Entradas unicas e invisibles para cada linea (formato §a§r, §b§r...)
        int index = 0;
        for (ChatColor color : COLORS) {
            if (index >= ENTRIES.length) {
                break;
            }
            if (color.isFormat()) {
                continue;
            }
            ENTRIES[index++] = color.toString() + ChatColor.RESET;
        }
        for (int i = index; i < ENTRIES.length; i++) {
            ENTRIES[i] = ChatColor.RESET.toString() + ChatColor.values()[i % COLORS.length].toString();
        }
    }

    private final PracticePlugin plugin;
    private final Map<UUID, BoardData> boards = new HashMap<>(128);

    public BoardManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    private static final class BoardData {
        final Scoreboard scoreboard;
        final Objective objective;
        final List<String> rawLines = new ArrayList<>(MAX_LINES);
        final List<Team> lineTeams = new ArrayList<>(MAX_LINES);
        String rawTitle = "";

        BoardData(Scoreboard scoreboard, Objective objective) {
            this.scoreboard = scoreboard;
            this.objective = objective;
        }
    }

    public Map<UUID, BoardData> rawBoards() {
        return this.boards;
    }

    /** Devuelve (creando si hace falta) el scoreboard del jugador. */
    public Scoreboard scoreboard(Player player) {
        BoardData data = this.boards.get(player.getUniqueId());
        if (data == null) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("baltia", Criteria.DUMMY,
                    Cc.mm(this.plugin.settings().boardTitle), RenderType.INTEGER);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            data = new BoardData(scoreboard, objective);
            this.boards.put(player.getUniqueId(), data);
            player.setScoreboard(scoreboard);
        }
        return data.scoreboard;
    }

    public boolean hasBoard(Player player) {
        return this.boards.containsKey(player.getUniqueId());
    }

    /** Refresca el sidebar de todos los jugadores (una sola pasada por tick). */
    public void updateAll() {
        if (!this.plugin.settings().boardEnabled) {
            return;
        }
        int online = Bukkit.getOnlinePlayers().size();
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player, online);
        }
    }

    public void update(Player player) {
        update(player, Bukkit.getOnlinePlayers().size());
    }

    public void update(Player player, int online) {
        if (!this.plugin.settings().boardEnabled) {
            return;
        }
        BoardData data = this.boards.get(player.getUniqueId());
        if (data == null) {
            scoreboard(player);
            data = this.boards.get(player.getUniqueId());
            if (data == null) {
                return;
            }
        }
        List<String> lines = renderLines(player, online);
        int max = Math.min(lines.size(), MAX_LINES);

        for (int i = 0; i < max; i++) {
            String value = lines.get(i);
            int score = max - i;
            if (i >= data.lineTeams.size()) {
                String entry = ENTRIES[i];
                Team team = data.scoreboard.registerNewTeam("line" + (i < 10 ? "0" + i : String.valueOf(i)));
                team.addEntry(entry);
                Component component = Cc.mm(Cc.legacyToMini(value));
                team.prefix(component);
                data.objective.getScore(entry).setScore(score);
                data.lineTeams.add(team);
                data.rawLines.add(value);
            } else {
                Team team = data.lineTeams.get(i);
                String previous = data.rawLines.get(i);
                if (!previous.equals(value)) {
                    team.prefix(Cc.mm(Cc.legacyToMini(value)));
                    data.rawLines.set(i, value);
                }
                // El score solo se reenvia cuando cambia el numero de lineas
                if (data.objective.getScore(ENTRIES[i]).getScore() != score) {
                    data.objective.getScore(ENTRIES[i]).setScore(score);
                }
            }
        }
        // Borrar lineas sobrantes (por ejemplo al cambiar de estado)
        for (int i = max; i < data.lineTeams.size(); i++) {
            Team team = data.lineTeams.get(i);
            team.unregister();
        }
        if (data.lineTeams.size() > max) {
            for (int i = data.lineTeams.size() - 1; i >= max; i--) {
                data.lineTeams.remove(i);
                data.rawLines.remove(i);
            }
        }
    }

    /** Construye las lineas segun el estado del jugador. */
    private List<String> renderLines(Player player, int online) {
        PlayerProfile profile = this.plugin.profiles().profile(player);
        DuelMatch match = this.plugin.duels().match(player.getUniqueId());
        FfaSession ffa = this.plugin.ffa().session(player.getUniqueId());
        String ping = String.valueOf(player.getPing());
        String elo = String.valueOf(profile.elo());

        List<String> template;
        Map<String, String> values = new HashMap<>(24);
        values.put("jugador", player.getName());
        values.put("elo", elo);
        values.put("online", String.valueOf(online));
        values.put("ping", ping);
        values.put("discord", this.plugin.settings().discord);
        values.put("ip", this.plugin.settings().serverIp);

        if (profile.spectatingId() != null) {
            DuelMatch observed = this.plugin.duels().matchById(profile.spectatingId());
            template = this.plugin.messages().rawList("scoreboard.lineas-espectador");
            values.put("jugador_observado", observed == null || observed.team(0).isEmpty()
                    ? "?" : this.plugin.duels().nameOf(observed.team(0).get(0)));
            values.put("kit", observed == null ? "?" : observed.kitName());
            values.put("arena", observed == null ? "?" : observed.arenaName());
        } else if (match != null) {
            UUID rival = match.firstOpponent(player.getUniqueId());
            Player rivalPlayer = rival == null ? null : Bukkit.getPlayer(rival);
            template = this.plugin.messages().rawList("scoreboard.lineas-partida");
            values.put("estado", match.state().display());
            values.put("kit", match.kitName());
            values.put("arena", match.arenaName());
            values.put("rival", rival == null ? "?" : this.plugin.duels().nameOf(rival));
            values.put("vida_rival", rivalPlayer == null ? "0.0"
                    : String.valueOf(Math.round(rivalPlayer.getHealth() / 2.0D * 10.0D) / 10.0D));
            values.put("rondas", match.scores()[0] + " - " + match.scores()[1]);
            values.put("duracion", match.state() == GameState.FIGHTING ? Cc.time(match.duration()) : "-");
        } else if (ffa != null) {
            template = this.plugin.messages().rawList("scoreboard.lineas-ffa");
            values.put("arena", ffa.arena());
            values.put("kills", String.valueOf(ffa.kills()));
            values.put("muertes", String.valueOf(ffa.deaths()));
            values.put("racha", String.valueOf(ffa.streak()));
            values.put("mejor_racha", String.valueOf(ffa.bestStreak()));
            values.put("jugadores_ffa", String.valueOf(this.plugin.ffa().population(ffa.arena())));
            values.put("vida", String.valueOf(Math.round(player.getHealth() / 2.0D * 10.0D) / 10.0D));
        } else if (this.plugin.queue().queued(player.getUniqueId())) {
            template = this.plugin.messages().rawList("scoreboard.lineas-cola");
            values.put("kit", this.plugin.queue().kitOf(player.getUniqueId()));
            values.put("tiempo", Cc.time(System.currentTimeMillis() - this.plugin.queue().joinedAt(player.getUniqueId())));
        } else {
            template = this.plugin.messages().rawList("scoreboard.lineas-lobby");
            values.put("kills", String.valueOf(profile.kills()));
            values.put("muertes", String.valueOf(profile.deaths()));
            values.put("kd", Cc.decimal(profile.kd()));
            values.put("racha", String.valueOf(profile.streak()));
        }
        List<String> out = new ArrayList<>(template.size());
        for (String line : template) {
            out.add(replace(line, values));
        }
        // titulo dinamico (solo se actualiza si cambia el texto)
        BoardData data = this.boards.get(player.getUniqueId());
        if (data != null) {
            String title = replace(this.plugin.settings().boardTitle, values);
            if (!title.equals(data.rawTitle)) {
                data.rawTitle = title;
                data.objective.displayName(Cc.mm(Cc.legacyToMini(title)));
            }
        }
        return out;
    }

    private String replace(String input, Map<String, String> values) {
        String out = input;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (out.indexOf('%') < 0) {
                break;
            }
            out = out.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return out;
    }

    public void remove(Player player) {
        this.boards.remove(player.getUniqueId());
    }

    public void clear() {
        this.boards.clear();
    }

    public int size() {
        return this.boards.size();
    }
}
