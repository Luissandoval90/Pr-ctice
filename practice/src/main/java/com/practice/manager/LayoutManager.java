package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Kit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Disposicion personal de cada kit, por jugador.
 * El editor solo reordena los objetos del kit: no se pueden meter items de fuera.
 */
public final class LayoutManager {

    private final PracticePlugin plugin;
    private final File file;
    private final Map<String, ItemStack[]> layouts = new ConcurrentHashMap<>(64);

    public LayoutManager(PracticePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data/layouts.yml");
    }

    public void load() {
        this.layouts.clear();
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        if (!config.isConfigurationSection("layouts")) {
            return;
        }
        for (String key : config.getConfigurationSection("layouts").getKeys(false)) {
            ItemStack[] items = new ItemStack[41];
            for (int slot = 0; slot < items.length; slot++) {
                items[slot] = config.getItemStack("layouts." + key + "." + slot);
            }
            this.layouts.put(key, items);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, ItemStack[]> entry : this.layouts.entrySet()) {
            ItemStack[] items = entry.getValue();
            for (int slot = 0; slot < items.length; slot++) {
                if (items[slot] != null && !items[slot].getType().isAir()) {
                    config.set("layouts." + entry.getKey() + "." + slot, items[slot]);
                }
            }
        }
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.plugin.getLogger().warning("No se pudieron guardar los layouts: " + exception.getMessage());
        }
    }

    private String key(UUID uuid, String kit) {
        return uuid + ":" + kit.toLowerCase(java.util.Locale.ROOT);
    }

    public boolean has(UUID uuid, String kit) {
        return this.layouts.containsKey(key(uuid, kit));
    }

    public ItemStack[] get(UUID uuid, String kit) {
        ItemStack[] stored = this.layouts.get(key(uuid, kit));
        if (stored == null) {
            return null;
        }
        ItemStack[] copy = new ItemStack[stored.length];
        for (int i = 0; i < stored.length; i++) {
            copy[i] = stored[i] == null ? null : stored[i].clone();
        }
        return copy;
    }

    /** 0-35 inventario, 36-39 armadura, 40 mano secundaria. */
    public void save(UUID uuid, String kit, ItemStack[] items) {
        ItemStack[] copy = new ItemStack[41];
        for (int i = 0; i < copy.length && i < items.length; i++) {
            copy[i] = items[i] == null || items[i].getType().isAir() ? null : items[i].clone();
        }
        this.layouts.put(key(uuid, kit), copy);
        save();
    }

    public void reset(UUID uuid, String kit) {
        this.layouts.remove(key(uuid, kit));
        save();
    }

    /** Aplica el layout encima del kit ya entregado. Si no hay layout, no hace nada. */
    public void apply(Player player, Kit kit) {
        ItemStack[] layout = get(player.getUniqueId(), kit.name());
        if (layout == null) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        for (int slot = 0; slot < 36; slot++) {
            if (layout[slot] != null) {
                inventory.setItem(slot, layout[slot].clone());
            }
        }
        inventory.setBoots(clone(layout, 36));
        inventory.setLeggings(clone(layout, 37));
        inventory.setChestplate(clone(layout, 38));
        inventory.setHelmet(clone(layout, 39));
        if (layout.length > 40 && layout[40] != null) {
            inventory.setItemInOffHand(layout[40].clone());
        }
        player.updateInventory();
    }

    private ItemStack clone(ItemStack[] items, int index) {
        return index < items.length && items[index] != null ? items[index].clone() : null;
    }
}
