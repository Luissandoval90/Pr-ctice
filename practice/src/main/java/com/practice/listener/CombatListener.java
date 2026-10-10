package com.practice.listener;

import com.practice.PracticePlugin;
import com.practice.model.Cosmetic;
import com.practice.model.DuelMatch;
import com.practice.model.GameState;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Combo en la actionbar y efectos de golpe. Solo reacciona a eventos, sin tareas extra.
 */
public final class CombatListener implements Listener {

    private static final long COMBO_WINDOW = 1600L;

    private final PracticePlugin plugin;

    public CombatListener(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = attacker(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        DuelMatch match = this.plugin.duels().match(attacker.getUniqueId());
        if (match == null || match.state() != GameState.FIGHTING || match.teamOf(attacker.getUniqueId()) == match.teamOf(victim.getUniqueId())) {
            return;
        }
        PlayerProfile profile = this.plugin.profiles().profile(attacker);
        long now = System.currentTimeMillis();
        int combo = now - profile.comboAt() <= COMBO_WINDOW ? profile.combo() + 1 : 1;
        profile.combo(combo);
        if (profile.personalSounds() || this.plugin.settings().sounds) {
            attacker.sendActionBar(Cc.mm("<#FFD1DC>Combo <#FFF5BA>x" + combo
                    + " <dark_gray>| <#FFB3BA>" + hearts(victim.getHealth())));
        }
        if (combo >= 3) {
            this.plugin.challenges().onStreak(attacker, combo);
        }
    }

    public static void killEffect(PracticePlugin plugin, Player killer, Player victim) {
        if (killer == null) {
            return;
        }
        Cosmetic cosmetic = Cosmetic.byId(plugin.profiles().profile(killer).killEffect());
        if (cosmetic.particle() == null) {
            return;
        }
        victim.getWorld().spawnParticle(cosmetic.particle(), victim.getLocation().add(0, 1, 0), 24, 0.4D, 0.6D, 0.4D, 0.02D);
        if (cosmetic.sound() != null && plugin.settings().sounds) {
            killer.playSound(killer.getLocation(), cosmetic.sound(), 0.7F, 1.2F);
        }
    }

    public static void winEffect(PracticePlugin plugin, Player winner) {
        Cosmetic cosmetic = Cosmetic.byId(plugin.profiles().profile(winner).winEffect());
        if (cosmetic.particle() == null || !cosmetic.winEffect()) {
            return;
        }
        winner.getWorld().spawnParticle(cosmetic.particle(), winner.getLocation().add(0, 1, 0), 40, 0.5D, 0.8D, 0.5D, 0.04D);
        if (cosmetic.sound() != null && plugin.settings().sounds) {
            winner.playSound(winner.getLocation(), cosmetic.sound(), 0.8F, 1.1F);
        }
    }

    private static String hearts(double health) {
        return String.valueOf(Math.round(health / 2.0D * 10.0D) / 10.0D);
    }

    private static Player attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }
}
