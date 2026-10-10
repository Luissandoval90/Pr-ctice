package com.practice.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Acceso tipado y cacheado a la configuracion.
 * <p>
 * Todos los valores se leen una sola vez al cargar para evitar
 * llamadas repetidas a {@link FileConfiguration} (optimizacion).
 */
public final class Settings {

    private final JavaPlugin plugin;

    // Servidor
    public String serverName;
    public String serverIp;
    public String discord;
    public String store;

    // Rendimiento
    public int boardInterval;
    public int tabInterval;
    public int matchInterval;
    public int autosaveMinutes;
    public boolean differentialBoard;

    // Lobby
    public String lobbyWorld;
    public boolean teleportOnJoin;
    public boolean giveLobbyItems;
    public boolean lobbyProtection;
    public boolean clearOnJoin;
    public boolean clearWeather;
    public long fixedTime;
    public boolean antiVoid;
    public int voidY;

    // Duelos
    public int requestExpire;
    public int countdown;
    public int defaultRounds;
    public int summarySeconds;
    public int matchLimitMinutes;
    public boolean healOnStart;
    public boolean titles;
    public boolean allowPartyInternal;
    public int eloRange;

    // ELO
    public boolean eloEnabled;
    public int eloStart;
    public int eloWin;
    public int eloLoss;
    public int eloStreakBonus;
    public int eloMin;
    public int eloMax;
    public double eloRoundsMultiplier;
    public boolean eloReal;
    public int eloK;

    // Economia, eventos y HUD v2
    public int coinsWinRanked;
    public int coinsWinUnranked;
    public int coinsLoss;
    public boolean holograms;
    public int announceMinutes;
    public boolean challengesEnabled;
    public boolean tournamentsEnabled;
    public int tournamentPrize;

    // Scoreboard
    public boolean boardEnabled;
    public String boardTitle;
    public boolean boardCompact;

    // TAB
    public boolean tabEnabled;
    public String tabHeader;
    public String tabFooter;
    public String tabNameFormat;
    public boolean nametags;
    public boolean nametagMode;

    // Chat
    public String chatFormat;
    public boolean chatHoverStats;
    public boolean chatClickMessage;
    public String partyPrefix;

    // Partidas
    public boolean spectators;
    public boolean ffaRespawn;
    public int ffaRespawnTicks;
    public boolean deathMessages;

    // Efectos
    public boolean sounds;
    public boolean particles;
    public boolean effectTitles;
    public boolean welcomeMessage;

    public Settings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = this.plugin.getConfig();

        this.serverName = c.getString("servidor.nombre", "<gradient:#FFD1DC:#B0E0E6><bold>BaltiaMC</bold></gradient>");
        this.serverIp = c.getString("servidor.ip", "play.baltiamc.net");
        this.discord = c.getString("servidor.discord", "discord.gg/baltiamc");
        this.store = c.getString("servidor.tienda", "store.baltiamc.net");

        this.boardInterval = Math.max(5, c.getInt("rendimiento.intervalo-scoreboard", 20));
        this.tabInterval = Math.max(10, c.getInt("rendimiento.intervalo-tab", 40));
        this.matchInterval = Math.max(5, c.getInt("rendimiento.intervalo-partidas", 10));
        this.autosaveMinutes = Math.max(1, c.getInt("rendimiento.autosave-minutos", 5));
        this.differentialBoard = c.getBoolean("rendimiento.envio-diferencial", true);

        this.lobbyWorld = c.getString("lobby.mundo", "");
        this.teleportOnJoin = c.getBoolean("lobby.teleportar-al-entrar", true);
        this.giveLobbyItems = c.getBoolean("lobby.dar-items", true);
        this.lobbyProtection = c.getBoolean("lobby.proteccion", true);
        this.clearOnJoin = c.getBoolean("lobby.limpiar-al-entrar", true);
        this.clearWeather = c.getBoolean("lobby.clima-despejado", true);
        this.fixedTime = c.getLong("lobby.hora-fija", 6000L);
        this.antiVoid = c.getBoolean("lobby.anti-void", true);
        this.voidY = c.getInt("lobby.y-minima", 0);

        this.requestExpire = Math.max(5, c.getInt("duelos.expiracion-peticion", 30));
        this.countdown = Math.max(0, c.getInt("duelos.cuenta-atras", 3));
        this.defaultRounds = Math.max(1, c.getInt("duelos.rondas-por-defecto", 1));
        this.summarySeconds = Math.max(1, c.getInt("duelos.duracion-resumen", 5));
        this.matchLimitMinutes = Math.max(0, c.getInt("duelos.limite-partida", 15));
        this.healOnStart = c.getBoolean("duelos.curar-al-empezar", true);
        this.titles = c.getBoolean("duelos.titulos", true);
        this.allowPartyInternal = c.getBoolean("duelos.permitir-duelos-internos", false);
        this.eloRange = Math.max(0, c.getInt("duelos.rango-elo", 0));

        this.eloEnabled = c.getBoolean("elo.activado", true);
        this.eloStart = c.getInt("elo.inicial", 1000);
        this.eloWin = c.getInt("elo.victoria", 25);
        this.eloLoss = c.getInt("elo.derrota", 20);
        this.eloStreakBonus = c.getInt("elo.bonus-racha", 5);
        this.eloMin = c.getInt("elo.minimo", 0);
        this.eloMax = c.getInt("elo.maximo", 5000);
        this.eloRoundsMultiplier = c.getDouble("elo.multiplicador-rondas", 1.5D);
        this.eloReal = c.getBoolean("elo.sistema-real", true);
        this.eloK = Math.max(8, c.getInt("elo.factor-k", 32));

        this.coinsWinRanked = c.getInt("economia.monedas-victoria-ranked", 15);
        this.coinsWinUnranked = c.getInt("economia.monedas-victoria-normal", 10);
        this.coinsLoss = c.getInt("economia.monedas-derrota", 3);
        this.holograms = c.getBoolean("hologramas.activado", true);
        this.announceMinutes = Math.max(1, c.getInt("anuncios.intervalo-minutos", 4));
        this.challengesEnabled = c.getBoolean("desafios.activado", true);
        this.tournamentsEnabled = c.getBoolean("torneos.activado", true);
        this.tournamentPrize = c.getInt("torneos.premio", 100);

        this.boardEnabled = c.getBoolean("scoreboard.activado", true);
        this.boardTitle = c.getString("scoreboard.titulo", "<gradient:#FFD1DC:#C1E1C1><bold>  BALTIA  </bold></gradient>");
        this.boardCompact = c.getBoolean("scoreboard.compacto", false);

        this.tabEnabled = c.getBoolean("tab.activado", true);
        this.tabHeader = c.getString("tab.header", "<gradient:#FFD1DC:#B0E0E6><bold>BALTIA MC</bold></gradient>");
        this.tabFooter = c.getString("tab.footer", "<#FFD1DC>play.baltiamc.net");
        this.tabNameFormat = c.getString("tab.formato-nombre", "%rank%<#E8E8F0>%name%");
        this.nametags = c.getBoolean("tab.nametags", true);
        this.nametagMode = c.getBoolean("tab.nametag-modo", true);

        this.chatFormat = c.getString("chat.formato", "<#BFC4CC>%rank% <dark_gray>»</dark_gray> <#E8E8F0>%name% <dark_gray>❯</dark_gray> <white>%mensaje%");
        this.chatHoverStats = c.getBoolean("chat.hover-stats", true);
        this.chatClickMessage = c.getBoolean("chat.click-susurrar", true);
        this.partyPrefix = c.getString("chat.party-prefijo", "@");

        this.spectators = c.getBoolean("partidas.espectadores", true);
        this.ffaRespawn = c.getBoolean("partidas.ffa-reaparecer", true);
        this.ffaRespawnTicks = Math.max(1, c.getInt("partidas.ffa-respawn-ticks", 40));
        this.deathMessages = c.getBoolean("partidas.mensajes-muerte", true);

        this.sounds = c.getBoolean("efectos.sonidos", true);
        this.particles = c.getBoolean("efectos.particulas", true);
        this.effectTitles = c.getBoolean("efectos.titulos", true);
        this.welcomeMessage = c.getBoolean("efectos.mensaje-bienvenida", true);
    }

    public String itemPath(String key, String def) {
        return this.plugin.getConfig().getString("items-lobby." + key, def);
    }
}
