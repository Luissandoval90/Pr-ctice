package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Division;
import com.practice.model.Leaderboard;
import com.practice.util.Cc;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Clasificacion flotante en el lobby con entidades TextDisplay.
 * Se recrea al activarse y se actualiza el texto sin respawnear.
 */
public final class HologramManager {

    public static final String TAG = "practicemc_holo";

    private final PracticePlugin plugin;
    private final List<TextDisplay> lines = new ArrayList<>(12);
    private Location anchor;

    public HologramManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAnchor() {
        String stored = this.plugin.getConfig().getString("hologramas.posicion", "");
        if (stored != null && !stored.isEmpty()) {
            try {
                String[] parts = stored.split(",");
                World world = Bukkit.getWorld(parts[0]);
                if (world != null) {
                    this.anchor = new Location(world,
                            Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                    return;
                }
            } catch (RuntimeException ignored) {
                this.anchor = null;
            }
        }
        Location spawn = this.plugin.lobby().spawn();
        this.anchor = spawn.clone().add(0.0D, 2.4D, 4.0D);
    }

    public void anchor(Location location) {
        this.anchor = location.clone();
        this.plugin.getConfig().set("hologramas.posicion", location.getWorld().getName() + ","
                + location.getX() + "," + location.getY() + "," + location.getZ());
        this.plugin.saveConfig();
        respawn();
    }

    public void respawn() {
        remove();
        if (!this.plugin.settings().holograms || this.anchor == null || this.anchor.getWorld() == null) {
            return;
        }
        for (int i = 0; i < 12; i++) {
            Location at = this.anchor.clone().add(0.0D, -i * 0.28D, 0.0D);
            TextDisplay display;
            try {
                display = at.getWorld().spawn(at, TextDisplay.class);
            } catch (RuntimeException exception) {
                this.plugin.getLogger().warning("Holograma no disponible: " + exception.getMessage());
                remove();
                return;
            }
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setLineWidth(220);
            display.setViewRange(48.0F);
            display.setShadowRadius(0.0F);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(70, 28, 22, 36));
            display.setPersistent(true);
            display.setGravity(false);
            display.addScoreboardTag(TAG);
            this.lines.add(display);
        }
        update();
    }

    public void update() {
        if (this.lines.isEmpty()) {
            return;
        }
        List<Component> text = build();
        for (int i = 0; i < this.lines.size(); i++) {
            TextDisplay display = this.lines.get(i);
            if (display == null || !display.isValid()) {
                continue;
            }
            display.text(i < text.size() ? text.get(i) : Component.empty());
        }
    }

    private List<Component> build() {
        List<Component> out = new ArrayList<>(12);
        out.add(Cc.mm("<gradient:#FFD1DC:#B0E0E6><bold>BALTIA TOP</bold></gradient>"));
        out.add(Cc.mm("<#BFC4CC>" + this.plugin.settings().serverIp));
        List<Leaderboard.Entry> top = this.plugin.profiles().top(Leaderboard.Type.ELO, 8);
        if (top.isEmpty()) {
            out.add(Cc.mm("<#BFC4CC>Aun no hay ranked"));
        } else {
            for (Leaderboard.Entry entry : top) {
                Division division = Division.of((int) entry.value());
                out.add(Cc.mm(division.badge() + " <#FFD1DC>" + entry.position() + ". <#E8E8F0>"
                        + entry.name() + " <#FFF5BA>" + (int) entry.value()));
            }
        }
        while (out.size() < 12) {
            out.add(Component.empty());
        }
        return out;
    }

    public void remove() {
        for (TextDisplay display : this.lines) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        this.lines.clear();
        // Limpia hologramas huerfanos de un reinicio anterior
        for (World world : Bukkit.getWorlds()) {
            for (TextDisplay display : world.getEntitiesByClass(TextDisplay.class)) {
                if (display.getScoreboardTags().contains(TAG)) {
                    display.remove();
                }
            }
        }
    }

    public boolean enabled() {
        return !this.lines.isEmpty();
    }

    public int size() {
        int count = 0;
        for (Entity entity : this.lines) {
            if (entity != null && entity.isValid()) {
                count++;
            }
        }
        return count;
    }
}
