package com.practice.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Definicion de un kit de Practice.
 * <p>
 * Guarda el contenido del inventario, la armadura, los efectos de pocion
 * y las reglas particulares de la pelea.
 */
public final class Kit {

    private final String name;
    private String displayName;
    private String description;
    private Material icon;
    private MatchType type;
    private int cooldown;
    private String permission;
    private int maxRounds;

    // Contenido
    private ItemStack[] inventory = new ItemStack[36];
    private ItemStack[] armor = new ItemStack[4];
    private ItemStack offHand;
    private final Map<String, Integer> effects = new LinkedHashMap<>();

    // Reglas
    private boolean allowHunger = true;
    private boolean keepInventory = false;
    private boolean naturalRegen = true;
    private boolean fallDamage = true;
    private boolean blockBreaking = false;
    private boolean blockPlacing = false;
    private boolean showHealthBar = true;

    public Kit(String name) {
        this.name = name.toLowerCase(java.util.Locale.ROOT);
        this.displayName = "<#FFD1DC>" + name;
        this.description = "Kit de Practice";
        this.icon = Material.DIAMOND_SWORD;
        this.type = MatchType.SOLO;
        this.cooldown = 0;
        this.permission = "";
        this.maxRounds = 0;
    }

    // ---------------- Getters / Setters ----------------
    public String name() {
        return this.name;
    }

    public String displayName() {
        return this.displayName;
    }

    public void displayName(String value) {
        this.displayName = value;
    }

    public String description() {
        return this.description;
    }

    public void description(String value) {
        this.description = value;
    }

    public Material icon() {
        return this.icon;
    }

    public void icon(Material value) {
        this.icon = value;
    }

    public MatchType type() {
        return this.type;
    }

    public void type(MatchType value) {
        this.type = value;
    }

    public int cooldown() {
        return this.cooldown;
    }

    public void cooldown(int value) {
        this.cooldown = value;
    }

    public String permission() {
        return this.permission;
    }

    public void permission(String value) {
        this.permission = value;
    }

    public int maxRounds() {
        return this.maxRounds;
    }

    public void maxRounds(int value) {
        this.maxRounds = value;
    }

    public ItemStack[] inventory() {
        return this.inventory;
    }

    public void inventory(ItemStack[] value) {
        this.inventory = value == null ? new ItemStack[36] : value;
    }

    public ItemStack[] armor() {
        return this.armor;
    }

    public void armor(ItemStack[] value) {
        this.armor = value == null ? new ItemStack[4] : value;
    }

    public ItemStack offHand() {
        return this.offHand;
    }

    public void offHand(ItemStack value) {
        this.offHand = value;
    }

    public Map<String, Integer> effects() {
        return this.effects;
    }

    public boolean allowHunger() {
        return this.allowHunger;
    }

    public void allowHunger(boolean value) {
        this.allowHunger = value;
    }

    public boolean keepInventory() {
        return this.keepInventory;
    }

    public void keepInventory(boolean value) {
        this.keepInventory = value;
    }

    public boolean naturalRegen() {
        return this.naturalRegen;
    }

    public void naturalRegen(boolean value) {
        this.naturalRegen = value;
    }

    public boolean fallDamage() {
        return this.fallDamage;
    }

    public void fallDamage(boolean value) {
        this.fallDamage = value;
    }

    public boolean blockBreaking() {
        return this.blockBreaking;
    }

    public void blockBreaking(boolean value) {
        this.blockBreaking = value;
    }

    public boolean blockPlacing() {
        return this.blockPlacing;
    }

    public void blockPlacing(boolean value) {
        this.blockPlacing = value;
    }

    public boolean showHealthBar() {
        return this.showHealthBar;
    }

    public void showHealthBar(boolean value) {
        this.showHealthBar = value;
    }

    /** Copia los items del jugador dentro del kit. */
    public void captureFrom(org.bukkit.entity.Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        ItemStack[] copy = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            copy[i] = contents[i] == null ? null : contents[i].clone();
        }
        this.inventory = copy;
        ItemStack[] armorContents = inv.getArmorContents();
        ItemStack[] armorCopy = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            armorCopy[i] = i < armorContents.length && armorContents[i] != null ? armorContents[i].clone() : null;
        }
        this.armor = armorCopy;
        this.offHand = inv.getItemInOffHand() == null ? null : inv.getItemInOffHand().clone();
    }

    /** Lista de items no vacios (para iconos de menu). */
    public List<ItemStack> displayItems() {
        List<ItemStack> items = new ArrayList<>(5);
        if (this.armor[3] != null) {
            items.add(this.armor[3]);
        }
        if (this.offHand != null) {
            items.add(this.offHand);
        }
        for (ItemStack item : this.inventory) {
            if (item != null && !item.getType().isAir()) {
                items.add(item);
                if (items.size() >= 5) {
                    break;
                }
            }
        }
        return items;
    }
}
