package com.practice.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constructor de items con el diseno pastel del servidor.
 * <p>
 * Optimizado: los metadatos se aplican una sola vez y las listas se
 * pre-dimensionan para evitar reallocaciones innecesarias.
 */
public final class ItemBuilder {

    private final ItemStack item;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
    }

    public ItemBuilder(ItemStack base) {
        this.item = base.clone();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder glass(Material material, String name) {
        return new ItemBuilder(material).name(name);
    }

    /** Nombre en MiniMessage (convierte & de forma automatica). */
    public ItemBuilder name(String miniMessage) {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            meta.displayName(Cc.mm(Cc.legacyToMini(miniMessage)));
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder name(Component component) {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            meta.displayName(component);
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            List<Component> lore = new ArrayList<>(lines.size());
            for (String line : lines) {
                lore.add(Cc.mm(Cc.legacyToMini(line)));
            }
            meta.lore(lore);
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder loreComponent(List<Component> lines) {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            meta.lore(lines);
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            if (glow) {
                meta.setEnchantmentGlintOverride(Boolean.TRUE);
            } else {
                meta.setEnchantmentGlintOverride(null);
            }
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder unbreakable() {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder hideAll() {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
                    org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS,
                    org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
                    org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder skull(OfflinePlayer owner) {
        if (this.item.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
            this.item.setItemMeta(skullMeta);
        }
        return this;
    }

    public ItemBuilder customModel(int data) {
        ItemMeta meta = this.item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data);
            this.item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return this.item;
    }
}
