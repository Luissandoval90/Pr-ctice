package com.practice.hud;

import com.practice.PracticePlugin;
import com.practice.model.DuelMatch;
import com.practice.model.FfaSession;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import com.practice.util.Ranks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Diseno de la TAB (lista de jugadores) y de los nametags.
 * <p>
 * Optimizacion:
 * <ul>
 *     <li>Cabecera y pie se parsean una sola vez por refresco (mismo componente para todos).</li>
 *     <li>Los nametags usan el scoreboard principal con un equipo por rango: cero paquetes por jugador.</li>
 *     <li>El orden de la lista se calcula con setPlayerListOrder (sin reenviar la tabla completa).</li>
 * </ul>
 */
public final class TabManager {

    private final PracticePlugin plugin;
    private final Map<UUID, Integer> listOrders = new HashMap<>(128);
    private Component cachedHeader;
    private Component cachedFooter;
    private int cachedOnline = -1;
    private long cachedTpsMillis;

    public TabManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    /** Refresca la TAB de todos los jugadores. */
    public void updateAll() {
        if (!this.plugin.settings().tabEnabled) {
            return;
        }
        int online = Bukkit.getOnlinePlayers().size();
        // El encabezado solo se parsea cuando cambia el numero de jugadores o cada 5 segundos (TPS)
        long now = System.currentTimeMillis();
        if (this.cachedHeader == null || online != this.cachedOnline || now - this.cachedTpsMillis > 5000L) {
            String tps = Cc.decimal(Bukkit.getServer().getTPS()[0]);
            this.cachedHeader = parse(this.plugin.settings().tabHeader, online, tps);
            this.cachedFooter = parse(this.plugin.settings().tabFooter, online, tps);
            this.cachedOnline = online;
            this.cachedTpsMillis = now;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(this.cachedHeader, this.cachedFooter);
            updateName(player);
            if (this.plugin.settings().nametags) {
                updateNametag(player);
            }
        }
    }

    private Component parse(String template, int online, String tps) {
        String value = template
                .replace("%online%", String.valueOf(online))
                .replace("%max%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%tps%", tps)
                .replace("%ip%", this.plugin.settings().serverIp)
                .replace("%discord%", this.plugin.settings().discord)
                .replace("%store%", this.plugin.settings().store);
        return Cc.mm(Cc.legacyToMini(value));
    }

    /** Actualiza el nombre del jugador en la lista. */
    public void updateName(Player player) {
        if (!this.plugin.settings().tabEnabled) {
            return;
        }
        player.playerListName(name(player));
        int rankWeight = rankWeight(player);
        Integer previous = this.listOrders.get(player.getUniqueId());
        int order = rankWeight * 10000 + this.plugin.profiles().profile(player).elo();
        if (previous == null || previous != order) {
            player.setPlayerListOrder(order);
            this.listOrders.put(player.getUniqueId(), order);
        }
    }

    /** Componente del nombre en la lista: rango + nombre + ELO. */
    public Component name(Player player) {
        Ranks.Rank rank = this.plugin.ranks().of(player);
        PlayerProfile profile = this.plugin.profiles().profile(player);
        String format = this.plugin.settings().tabNameFormat;
        String value = format
                .replace("%rank%", rank.prefix())
                .replace("%name%", player.getName())
                .replace("%elo%", String.valueOf(profile.elo()))
                .replace("%kills%", String.valueOf(profile.kills()))
                .replace("%ping%", String.valueOf(player.getPing()));
        return Cc.mm(Cc.legacyToMini(value));
    }

    private int rankWeight(Player player) {
        String key = this.plugin.ranks().of(player).key();
        return switch (key) {
            case "admin" -> 4;
            case "staff" -> 3;
            case "vip" -> 2;
            default -> 1;
        };
    }

    /** Actualiza el nametag (equipo en el scoreboard principal). */
    public void updateNametag(Player player) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Ranks.Rank rank = this.plugin.ranks().of(player);
        String teamName = "nt_" + rank.key();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        Team team = main.getTeam(teamName);
        if (team == null) {
            try {
                team = main.registerNewTeam(teamName);
                team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                team.setCanSeeFriendlyInvisibles(true);
                TextColor color = TextColor.fromHexString(rank.color());
                team.color(color == null ? NamedTextColor.WHITE : NamedTextColor.nearestTo(color));
            } catch (IllegalArgumentException exception) {
                team = main.getTeam(teamName);
                if (team == null) {
                    return;
                }
            }
        }
        String suffix = nametagSuffix(player);
        team.prefix(Cc.mm(Cc.legacyToMini(rank.prefix() + " <#E8E8F0>")));
        team.suffix(Cc.mm(Cc.legacyToMini(suffix)));
        String entry = player.getName();
        if (team.hasEntry(entry)) {
            return;
        }
        // Quitar la entrada de cualquier otro equipo antes de anadirla
        Team current = main.getEntryTeam(entry);
        if (current != null && current != team) {
            current.removeEntry(entry);
        }
        team.addEntry(entry);
    }

    private String nametagSuffix(Player player) {
        if (!this.plugin.settings().nametagMode) {
            return "";
        }
        DuelMatch match = this.plugin.duels().match(player.getUniqueId());
        if (match != null) {
            return " <dark_gray>| <#FFD1DC>" + match.kitName();
        }
        FfaSession ffa = this.plugin.ffa().session(player.getUniqueId());
        if (ffa != null) {
            return " <dark_gray>| <#FFF5BA>FFA <#BFC4CC>" + ffa.kills() + " kills";
        }
        if (this.plugin.queue().queued(player.getUniqueId())) {
            return " <dark_gray>| <#C1E1C1>Buscando " + this.plugin.queue().kitOf(player.getUniqueId());
        }
        PlayerProfile profile = this.plugin.profiles().profile(player);
        return " <dark_gray>| <#BFC4CC>" + profile.elo() + " ELO";
    }

    public void remove(Player player) {
        this.listOrders.remove(player.getUniqueId());
    }

    public void clear() {
        this.listOrders.clear();
        this.cachedHeader = null;
        this.cachedFooter = null;
        this.cachedOnline = -1;
    }
}
