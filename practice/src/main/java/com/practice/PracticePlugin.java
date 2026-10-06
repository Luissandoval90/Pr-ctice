package com.practice;

import com.practice.command.ArenaCommand;
import com.practice.command.DuelCommand;
import com.practice.command.FfaCommand;
import com.practice.command.KitCommand;
import com.practice.command.MenuCommand;
import com.practice.command.PartyCommand;
import com.practice.command.PracticeCommand;
import com.practice.command.SpawnCommand;
import com.practice.command.SpectateCommand;
import com.practice.command.StatsCommand;
import com.practice.command.TopCommand;
import com.practice.config.Messages;
import com.practice.config.Settings;
import com.practice.hud.BoardManager;
import com.practice.hud.MenuManager;
import com.practice.hud.TabManager;
import com.practice.listener.ChatListener;
import com.practice.listener.MenuListener;
import com.practice.listener.PlayerListener;
import com.practice.listener.ProtectionListener;
import com.practice.manager.ArenaManager;
import com.practice.manager.DuelManager;
import com.practice.manager.FfaManager;
import com.practice.manager.KitManager;
import com.practice.manager.LobbyManager;
import com.practice.manager.PartyManager;
import com.practice.manager.ProfileManager;
import com.practice.manager.QueueManager;
import com.practice.util.Cc;
import com.practice.util.Ranks;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * PracticeMC - plugin de practice completo para Paper 1.21.11.
 * <p>
 * Arquitectura:
 * <ul>
 *     <li>{@code manager}: logica de negocio (perfiles, kits, arenas, duelos, FFA, colas).</li>
 *     <li>{@code hud}: scoreboard, TAB, nametags y menus.</li>
 *     <li>{@code listener}: eventos del servidor.</li>
 *     <li>{@code command}: comandos y autocompletado.</li>
 * </ul>
 * <p>
 * Optimizacion: tareas periodicas unicas, envio diferencial al scoreboard,
 * guardado asincrono de datos y ausencia de tareas por jugador.
 */
public final class PracticePlugin extends JavaPlugin {

    private static PracticePlugin mInstance;

    private Settings mSettings;
    private Messages mMessages;
    private Ranks mRanks;
    private ProfileManager mProfiles;
    private KitManager mKits;
    private ArenaManager mArenas;
    private PartyManager mParties;
    private DuelManager mDuels;
    private FfaManager mFfa;
    private QueueManager mQueue;
    private LobbyManager mLobby;
    private BoardManager mBoard;
    private TabManager mTab;
    private MenuManager mMenus;

    public static PracticePlugin get() {
        return mInstance;
    }

    @Override
    public void onEnable() {
        this.mInstance = this;
        long start = System.currentTimeMillis();
        this.saveDefaultConfig();

        this.mSettings = new Settings(this);
        this.mMessages = new Messages(this);
        this.mRanks = new Ranks();
        this.mProfiles = new ProfileManager(this);
        this.mKits = new KitManager(this);
        this.mArenas = new ArenaManager(this);
        this.mParties = new PartyManager(this);
        this.mDuels = new DuelManager(this);
        this.mFfa = new FfaManager(this);
        this.mQueue = new QueueManager(this);
        this.mBoard = new BoardManager(this);
        this.mTab = new TabManager(this);
        this.mLobby = new LobbyManager(this);
        this.mMenus = new MenuManager(this);

        reloadAll();

        // Arenas por defecto para que el servidor sea jugable al instante
        if (getConfig().getBoolean("arenas.generar-por-defecto", true)) {
            World world = Bukkit.getWorlds().get(0);
            if (world != null) {
                if (this.mArenas.available(com.practice.model.MatchType.SOLO).isEmpty()) {
                    this.mArenas.createFallback(world);
                    getLogger().info("Arena de duelo por defecto generada (baltia_default).");
                }
                if (this.mFfa.arenas().isEmpty()) {
                    this.mArenas.createFfaFallback(world);
                    getLogger().info("Arena FFA por defecto generada (baltia_ffa).");
                }
            }
        }

        registerCommands();
        registerListeners();
        startTasks();

        // Jugadores que ya estaban conectados (recarga del plugin)
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.mProfiles.profile(player);
            this.mProfiles.startSession(player.getUniqueId());
            this.mLobby.sendToLobby(player);
            this.mTab.updateName(player);
        }

        getLogger().info("PracticeMC habilitado en " + (System.currentTimeMillis() - start) + " ms ("
                + this.mKits.all().size() + " kits, " + this.mArenas.count() + " arenas).");
    }

    @Override
    public void onDisable() {
        if (this.mDuels != null) {
            this.mDuels.clear();
        }
        if (this.mQueue != null) {
            this.mQueue.clear();
        }
        if (this.mFfa != null) {
            this.mFfa.clear();
        }
        if (this.mProfiles != null) {
            this.mProfiles.saveSync();
        }
        if (this.mKits != null) {
            this.mKits.save();
        }
        if (this.mArenas != null) {
            this.mArenas.save();
        }
        getLogger().info("PracticeMC deshabilitado. Datos guardados.");
    }

    /** Recarga toda la configuracion, mensajes, kits y arenas. */
    public void reloadAll() {
        reloadConfig();
        this.mSettings.load();
        this.mMessages.load();
        this.mRanks.load(this);
        this.mProfiles.loadAll();
        this.mKits.load();
        this.mArenas.load();
        this.mLobby.load();
    }

    private void registerCommands() {
        register("practice", new PracticeCommand(this));
        register("duel", new DuelCommand(this));
        register("party", new PartyCommand(this));
        register("ffa", new FfaCommand(this));
        register("kit", new KitCommand(this));
        register("arena", new ArenaCommand(this));
        register("stats", new StatsCommand(this));
        register("spawn", new SpawnCommand(this));
        register("top", new TopCommand(this));
        register("menu", new MenuCommand(this));
        register("spectate", new SpectateCommand(this));
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("El comando /" + name + " no esta definido en plugin.yml");
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
    }

    private void startTasks() {
        // Scoreboard: cada X ticks (por defecto 1s)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (this.mSettings.boardEnabled) {
                this.mBoard.updateAll();
            }
        }, 20L, this.mSettings.boardInterval);
        // TAB: cada X ticks (por defecto 2s)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (this.mSettings.tabEnabled) {
                this.mTab.updateAll();
            }
        }, 40L, this.mSettings.tabInterval);
        // Logica de partidas y colas: cada X ticks (por defecto 0.5s)
        getServer().getScheduler().runTaskTimer(this, () -> {
            this.mDuels.tick();
            this.mQueue.tick();
            if (this.mSettings.antiVoid) {
                checkVoid();
            }
        }, 10L, this.mSettings.matchInterval);
        // Actionbar del lobby
        getServer().getScheduler().runTaskTimer(this, this::updateActionBars, 60L, 40L);
        // Guardado asincrono
        long ticks = 20L * 60L * this.mSettings.autosaveMinutes;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> this.mProfiles.saveAsync(), ticks, ticks);
        // Mensajes de bienvenida rotatorios (actionbar) y limpieza de peticiones
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (this.mDuels.inMatch(player.getUniqueId()) || this.mFfa.inFfa(player.getUniqueId())) {
                    continue;
                }
                this.mTab.updateName(player);
            }
        }, 100L, 100L);
    }

    private void updateActionBars() {
        if (!this.mSettings.tabEnabled) {
            return;
        }
        int online = Bukkit.getOnlinePlayers().size();
        String raw = this.mMessages.raw("lobby.actionbar").replace("%online%", String.valueOf(online));
        net.kyori.adventure.text.Component component = Cc.mm(raw);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.mDuels.inMatch(player.getUniqueId()) || this.mFfa.inFfa(player.getUniqueId())) {
                continue;
            }
            player.sendActionBar(component);
        }
    }

    /** Anti-caida al vacio (comprobacion global barata). */
    private void checkVoid() {
        int minimum = this.mSettings.voidY;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().getY() > minimum) {
                continue;
            }
            if (this.mDuels.inMatch(player.getUniqueId())) {
                var match = this.mDuels.match(player.getUniqueId());
                if (match == null) {
                    continue;
                }
                var arena = this.mArenas.get(match.arenaName());
                int team = match.teamOf(player.getUniqueId());
                if (arena != null) {
                    player.teleport(team == 0 ? arena.spawn1() : arena.spawn2());
                }
                this.mMessages.send(player, "proteccion.void");
            } else if (this.mFfa.inFfa(player.getUniqueId())) {
                var session = this.mFfa.session(player.getUniqueId());
                var arena = session == null ? null : this.mArenas.get(session.arena());
                if (arena != null) {
                    player.teleport(this.mFfa.randomSpawn(arena));
                }
                this.mMessages.send(player, "proteccion.void");
            } else if (this.mLobby.isLobbyWorld(player)) {
                player.teleport(this.mLobby.spawn());
                this.mMessages.send(player, "proteccion.void");
            }
        }
    }

    // ------------------------------------------------------------------
    //  Accesos
    // ------------------------------------------------------------------
    public Settings settings() {
        return this.mSettings;
    }

    public Messages messages() {
        return this.mMessages;
    }

    public Ranks ranks() {
        return this.mRanks;
    }

    public ProfileManager profiles() {
        return this.mProfiles;
    }

    public KitManager kits() {
        return this.mKits;
    }

    public ArenaManager arenas() {
        return this.mArenas;
    }

    public PartyManager parties() {
        return this.mParties;
    }

    public DuelManager duels() {
        return this.mDuels;
    }

    public FfaManager ffa() {
        return this.mFfa;
    }

    public QueueManager queue() {
        return this.mQueue;
    }

    public LobbyManager lobby() {
        return this.mLobby;
    }

    public BoardManager board() {
        return this.mBoard;
    }

    public TabManager tab() {
        return this.mTab;
    }

    public MenuManager menus() {
        return this.mMenus;
    }
}
