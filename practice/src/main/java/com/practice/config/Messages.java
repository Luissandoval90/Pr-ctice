package com.practice.config;

import com.practice.util.Cc;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sistema de mensajes con MiniMessage y reemplazos cacheados.
 * <p>
 * Los mensajes se cargan en un mapa para acceso O(1) y se parsean
 * bajo demanda (no se guardan Components para permitir /practice reload).
 */
public final class Messages {

    private final JavaPlugin plugin;
    private final Map<String, Object> values = new HashMap<>(256);
    private String prefix = "<gradient:#FFD1DC:#B0E0E6><bold>Practice</bold></gradient> <dark_gray>»</dark_gray> ";

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        this.values.clear();
        File file = new File(this.plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        // defaults desde el jar
        try (InputStream in = this.plugin.getResource("messages.yml")) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
                config.options().copyDefaults(true);
            }
        } catch (Exception ignored) {
            // sin defaults disponibles
        }
        for (String key : config.getKeys(true)) {
            if (config.isConfigurationSection(key)) {
                continue;
            }
            this.values.put(key, config.get(key));
        }
        String configuredPrefix = config.getString("prefijo");
        if (configuredPrefix != null) {
            this.prefix = configuredPrefix;
        }
        // Claves nuevas del jar que el messages.yml del servidor todavia no tiene
        if (config.getDefaults() != null) {
            for (String key : config.getDefaults().getKeys(true)) {
                if (config.getDefaults().isConfigurationSection(key) || this.values.containsKey(key)) {
                    continue;
                }
                this.values.put(key, config.getDefaults().get(key));
            }
        }
    }

    public String prefix() {
        return this.prefix;
    }

    public String raw(String key) {
        Object value = this.values.get(key);
        return value == null ? key : String.valueOf(value);
    }

    public List<String> rawList(String key) {
        Object value = this.values.get(key);
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object entry : list) {
                out.add(String.valueOf(entry));
            }
            return out;
        }
        return List.of(String.valueOf(value == null ? key : value));
    }

    /** Componente del mensaje con placeholders. */
    public Component get(String key, Object... placeholders) {
        return Cc.mm(this.raw(key), Cc.resolver(placeholders));
    }

    /** Componente con prefijo del plugin. */
    public Component prefixed(String key, Object... placeholders) {
        return Cc.mm(this.prefix).append(this.get(key, placeholders));
    }

    /** Lista de componentes (para mensajes multilinea). */
    public List<Component> getList(String key, Object... placeholders) {
        TagResolver resolver = Cc.resolver(placeholders);
        List<Component> out = new ArrayList<>();
        for (String line : this.rawList(key)) {
            out.add(Cc.mm(line, resolver));
        }
        return out;
    }

    /** Envia un mensaje prefijado. */
    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(this.prefixed(key, placeholders));
    }

    /** Envia un mensaje multilinea prefijado. */
    public void sendList(CommandSender sender, String key, Object... placeholders) {
        for (Component line : this.getList(key, placeholders)) {
            sender.sendMessage(line);
        }
    }

    /** Envia un mensaje sin prefijo. */
    public void sendRaw(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(this.get(key, placeholders));
    }
}
