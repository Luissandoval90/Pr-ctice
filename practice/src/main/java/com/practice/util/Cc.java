package com.practice.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utilidades de color, texto y formato.
 * <p>
 * Toda la paleta del plugin es pastel y se aplica mediante MiniMessage
 * para poder usar colores hexadecimales suaves en cualquier version de Paper.
 */
public final class Cc {

    // ---------------- Paleta pastel oficial de BaltiaMC ----------------
    public static final String PINK = "#FFD1DC";
    public static final String BLUE = "#B0E0E6";
    public static final String GREEN = "#C1E1C1";
    public static final String PURPLE = "#E6D5F7";
    public static final String YELLOW = "#FFF5BA";
    public static final String RED = "#FFB3BA";
    public static final String TEXT = "#E8E8F0";
    public static final String MUTED = "#BFC4CC";
    public static final String ACCENT = "#AEC6CF";

    public static final String SEPARATOR = "<dark_gray><st>                    </st></dark_gray>";

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private Cc() {
    }

    /** Parsea una cadena MiniMessage. */
    public static Component mm(String text) {
        if (text == null) {
            return Component.empty();
        }
        return MM.deserialize(text);
    }

    /** Parsea una cadena MiniMessage con reemplazos. */
    public static Component mm(String text, TagResolver... resolvers) {
        if (text == null) {
            return Component.empty();
        }
        return MM.deserialize(text, TagResolver.resolver(resolvers));
    }

    /** Parsea con pares clave/valor simples. */
    public static Component mm(String text, Object... placeholders) {
        return MM.deserialize(text, resolver(placeholders));
    }

    /** Construye un resolver de placeholders a partir de pares clave/valor. */
    public static TagResolver resolver(Object... placeholders) {
        List<TagResolver> resolvers = new ArrayList<>();
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String value = String.valueOf(placeholders[i + 1]);
            resolvers.add(Placeholder.unparsed(key, value));
        }
        return TagResolver.resolver(resolvers);
    }

    /** Placeholder sin escapar. */
    public static TagResolver ph(String key, Object value) {
        return Placeholder.unparsed(key, String.valueOf(value));
    }

    /** Componente de texto sin cursiva. */
    public static Component plain(String text, String hexColor) {
        return Component.text(text)
                .color(TextColor.fromHexString(hexColor) == null ? NamedTextColor.WHITE : TextColor.fromHexString(hexColor))
                .decoration(TextDecoration.ITALIC, false);
    }

    /** Texto sin cursiva en la paleta indicada. */
    public static Component pastel(String text, String hexColor) {
        return plain(text, hexColor);
    }

    /** Componente vacio. */
    public static Component empty() {
        return Component.empty();
    }

    /** Convierte un Component a texto plano. */
    public static String plain(Component component) {
        return component == null ? "" : PLAIN.serialize(component);
    }

    /** Formatea milisegundos a texto compacto (1m 20s). */
    public static String time(long millis) {
        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0L) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    /** Formatea una cuenta atras en segundos con un decimal. */
    public static String seconds(long millis) {
        return String.valueOf(TimeUnit.MILLISECONDS.toSeconds(millis));
    }

    /** Numero con decimales controlados. */
    public static String decimal(double value) {
        return String.format("%.2f", value);
    }

    /** Numero entero con separador de miles (1,250). */
    public static String number(long value) {
        return String.format("%,d", value);
    }

    /** Barra de progreso pastel. */
    public static String bar(double percent, int size, String filled, String emptyColor) {
        double clamped = Math.max(0.0D, Math.min(1.0D, percent));
        int filledAmount = (int) Math.round(clamped * size);
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(filled).append('>');
        for (int i = 0; i < filledAmount; i++) {
            sb.append('|');
        }
        sb.append("</").append(filled).append('>');
        sb.append('<').append(emptyColor).append('>');
        for (int i = filledAmount; i < size; i++) {
            sb.append('|');
        }
        sb.append("</").append(emptyColor).append('>');
        return sb.toString();
    }

    /** Corazones de vida como texto. */
    public static String hearts(double health) {
        int amount = (int) Math.ceil(Math.max(0.0D, health) / 2.0D);
        StringBuilder sb = new StringBuilder("<" + RED + ">");
        for (int i = 0; i < amount; i++) {
            sb.append('❤');
        }
        sb.append("</" + RED + ">");
        return sb.toString();
    }

    /** Convierte legacy & a MiniMessage basico (por comodidad en configs). */
    public static String legacyToMini(String input) {
        if (input == null || input.indexOf('&') < 0) {
            return input;
        }
        return input.replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>").replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
    }

    /** Une lineas separando cada una con salto de linea. */
    public static Component join(List<Component> components) {
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < components.size(); i++) {
            if (i > 0) {
                builder.append(Component.newline());
            }
            builder.append(components.get(i));
        }
        return builder.build();
    }
}
