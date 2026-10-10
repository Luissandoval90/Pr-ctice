package com.practice.manager;

import com.practice.PracticePlugin;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Restaura los bloques de las arenas de construccion al terminar la partida.
 * El trabajo se reparte en tandas para no clavar el TPS.
 */
public final class RollbackManager {

    private static final int BATCH = 250;
    private static final int CAP = 4000;

    private final PracticePlugin plugin;
    private final Map<String, List<Change>> changes = new ConcurrentHashMap<>();

    public RollbackManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public void record(String matchId, Block block) {
        if (block == null) {
            return;
        }
        record(matchId, block, block.getBlockData());
    }

    public void record(String matchId, Block block, BlockData data) {
        if (matchId == null || block == null || data == null || block.getWorld() == null) {
            return;
        }
        List<Change> list = this.changes.computeIfAbsent(matchId, key -> new ArrayList<>(64));
        if (list.size() >= CAP) {
            return;
        }
        list.add(new Change(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), data));
    }

    public void restore(String matchId) {
        List<Change> list = this.changes.remove(matchId);
        if (list == null || list.isEmpty()) {
            return;
        }
        int[] index = {list.size() - 1};
        BukkitTask[] task = new BukkitTask[1];
        task[0] = this.plugin.getServer().getScheduler().runTaskTimer(this.plugin, () -> {
            int done = 0;
            while (index[0] >= 0 && done < BATCH) {
                Change change = list.get(index[0]--);
                org.bukkit.World world = org.bukkit.Bukkit.getWorld(change.world);
                if (world != null) {
                    Block block = world.getBlockAt(change.x, change.y, change.z);
                    block.setBlockData(change.data, false);
                }
                done++;
            }
            if (index[0] < 0 && task[0] != null) {
                task[0].cancel();
            }
        }, 1L, 1L);
    }

    public void clear() {
        this.changes.clear();
    }

    private record Change(String world, int x, int y, int z, BlockData data) {
    }
}
