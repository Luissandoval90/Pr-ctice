package com.practice.listener;

import com.practice.PracticePlugin;
import com.practice.model.Party;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * Formato del chat con diseno pastel, hover de estadisticas y chat de party.
 * <p>
 * Optimizacion: el componente del nombre se construye una sola vez por
 * mensaje y el renderer se aplica sin crear tareas adicionales.
 */
public final class ChatListener implements Listener {

    private final PracticePlugin plugin;

    public ChatListener(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String plain = PlainTextComponentSerializer.plainText().serialize(event.message());

        // ---------- Chat de party con prefijo @ ----------
        String prefix = this.plugin.settings().partyPrefix;
        if (prefix != null && !prefix.isEmpty() && plain.startsWith(prefix)) {
            event.viewers().clear();
            Party party = this.plugin.parties().of(player.getUniqueId());
            String content = plain.substring(prefix.length()).trim();
            if (content.isEmpty()) {
                return;
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (party == null) {
                    this.plugin.messages().send(player, "parties.no-en-party");
                    return;
                }
                Component message = Cc.mm(this.plugin.messages().raw("parties.chat"),
                        "jugador", player.getName(), "mensaje", content);
                for (Player member : this.plugin.parties().onlineMembers(party)) {
                    member.sendMessage(message);
                }
            });
            return;
        }

        // ---------- Chat normal ----------
        Component displayName = buildName(player);
        Component rank = Cc.mm(this.plugin.ranks().of(player).prefix());
        if (event.message().equals(Component.empty())) {
            return;
        }
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            String raw = this.plugin.settings().chatFormat
                    .replace("%rank%", "<rank>")
                    .replace("%name%", "<name>")
                    .replace("%mensaje%", "<msg>");
            if (raw.indexOf("<name>") < 0) {
                raw = "<name> <dark_gray>❯</dark_gray> <msg>";
            }
            return Cc.mm(Cc.legacyToMini(raw),
                    Placeholder.component("rank", rank),
                    Placeholder.component("name", Cc.mm("<#E8E8F0>" + source.getName())),
                    Placeholder.component("msg", message));
        });
    }

    /** Nombre con prefijo de rango, hover de estadisticas y click para susurrar. */
    private Component buildName(Player player) {
        Component name = this.plugin.tab().name(player);
        if (this.plugin.settings().chatHoverStats) {
            PlayerProfile profile = this.plugin.profiles().profile(player);
            List<Component> lines = new ArrayList<>(7);
            lines.add(Cc.mm("<gradient:#FFD1DC:#B0E0E6><bold>" + player.getName() + "</bold></gradient>"));
            lines.add(Cc.mm("<#BFC4CC>Division: " + profile.division().colored() + " <#BFC4CC>· <#FFF5BA>" + profile.elo() + " ELO"));
            lines.add(Cc.mm("<#BFC4CC>Kills: <#C1E1C1>" + profile.kills() + " <dark_gray>| <#BFC4CC>Muertes: <#FFB3BA>" + profile.deaths()));
            lines.add(Cc.mm("<#BFC4CC>K/D: <#E6D5F7>" + Cc.decimal(profile.kd())));
            lines.add(Cc.mm("<#BFC4CC>Victorias: <#C1E1C1>" + profile.wins() + " <dark_gray>| <#BFC4CC>Racha: <#FFD1DC>" + profile.streak()));
            lines.add(Cc.mm("<dark_gray>Click para susurrar"));
            name = name.hoverEvent(HoverEvent.showText(Cc.join(lines)));
        }
        if (this.plugin.settings().chatClickMessage) {
            name = name.clickEvent(ClickEvent.suggestCommand("/msg " + player.getName() + " "));
        }
        return name;
    }
}
