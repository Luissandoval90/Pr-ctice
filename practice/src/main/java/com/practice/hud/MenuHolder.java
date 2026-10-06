package com.practice.hud;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Identificador de los menus del plugin.
 * <p>
 * Permite reconocer una interfaz propia en los eventos de inventario sin
 * comparar titulos (mucho mas rapido y robusto).
 */
public final class MenuHolder implements InventoryHolder {

    private final String type;
    private final Map<String, Object> data = new HashMap<>(4);
    private Inventory inventory;

    public MenuHolder(String type) {
        this.type = type;
    }

    public String type() {
        return this.type;
    }

    public MenuHolder put(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public Object get(String key) {
        return this.data.get(key);
    }

    public String string(String key) {
        Object value = this.data.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
