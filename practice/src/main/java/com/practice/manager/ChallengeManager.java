package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tres desafios diarios compartidos por todo el servidor.
 * La semilla es la fecha, asi que no hace falta persistir cuales tocan hoy.
 */
public final class ChallengeManager {

    public record Def(String id, String name, String description, int goal, int reward) {
    }

    private static final Def[] POOL = {
            new Def("win3", "Triunfador", "Gana 3 duelos", 3, 40),
            new Def("kills8", "Cazador", "Consigue 8 kills", 8, 30),
            new Def("play5", "Constante", "Juega 5 partidas", 5, 25),
            new Def("ffa10", "Callejero", "10 kills en FFA", 10, 30),
            new Def("streak3", "En racha", "Alcanza una racha de 3", 3, 35),
            new Def("ranked1", "Ranked", "Gana 1 duelo ranked", 1, 20)
    };

    private final PracticePlugin plugin;

    public ChallengeManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public String todayKey() {
        return LocalDate.now().toString();
    }

    public List<Def> today() {
        int seed = LocalDate.now().getYear() * 1000 + LocalDate.now().getDayOfYear();
        Random random = new Random(seed);
        List<Def> pool = new ArrayList<>(List.of(POOL));
        List<Def> picked = new ArrayList<>(3);
        while (picked.size() < 3 && !pool.isEmpty()) {
            picked.add(pool.remove(random.nextInt(pool.size())));
        }
        return picked;
    }

    public void ensure(PlayerProfile profile) {
        String today = todayKey();
        if (today.equals(profile.challengeDay())) {
            return;
        }
        profile.challengeDay(today);
        profile.challengeProgress().clear();
        profile.challengeClaimed().clear();
        this.plugin.profiles().markDirty();
    }

    public void progress(Player player, String id, int amount) {
        if (!this.plugin.settings().challengesEnabled || player == null) {
            return;
        }
        PlayerProfile profile = this.plugin.profiles().profile(player);
        ensure(profile);
        boolean active = false;
        Def target = null;
        for (Def def : today()) {
            if (def.id().equals(id)) {
                active = true;
                target = def;
                break;
            }
        }
        if (!active || target == null || profile.challengeClaimed().contains(id)) {
            return;
        }
        int next = Math.min(target.goal(), profile.challengeProgress(id) + amount);
        profile.challengeProgress(id, next);
        this.plugin.profiles().markDirty();
        if (next >= target.goal()) {
            profile.challengeClaimed().add(id);
            profile.addCoins(target.reward());
            player.sendMessage(Cc.mm("<gradient:#FFF5BA:#C1E1C1><bold>Desafio</bold></gradient> <#E8E8F0>"
                    + target.name() + " <#C1E1C1>completado. +" + target.reward() + " monedas"));
            if (this.plugin.settings().sounds && profile.personalSounds()) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.4F);
            }
        }
    }

    public void onMatch(Player player, boolean won, boolean ranked) {
        progress(player, "play5", 1);
        if (won) {
            progress(player, "win3", 1);
            if (ranked) {
                progress(player, "ranked1", 1);
            }
        }
    }

    public void onKill(Player player) {
        progress(player, "kills8", 1);
    }

    public void onFfaKill(Player player) {
        progress(player, "ffa10", 1);
        progress(player, "kills8", 1);
    }

    public void onStreak(Player player, int streak) {
        if (streak >= 3) {
            progress(player, "streak3", 3);
        }
    }
}
