package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Kit;
import com.practice.model.MatchType;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import com.practice.util.ItemBuilder;
import com.practice.util.PlayerState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona el lobby: spawn, items, proteccion y bienvenida.
 */
public final class LobbyManager {

    private final PracticePlugin plugin;
    private Location spawn;

    public LobbyManager(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String worldName = this.plugin.settings().lobbyWorld;
        World world = worldName == null || worldName.isEmpty() ? Bukkit.getWorlds().get(0) : Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        String stored = this.plugin.getConfig().getString("lobby.spawn");
        if (stored != null && !stored.isEmpty()) {
            try {
                String[] parts = stored.split(",");
                World storedWorld = Bukkit.getWorld(parts[0]);
                if (storedWorld != null) {
                    this.spawn = new Location(storedWorld,
                            Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                            Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
                }
            } catch (RuntimeException ignored) {
                this.spawn = null;
            }
        }
        if (this.spawn == null) {
            this.spawn = world.getSpawnLocation();
        }
        if (this.plugin.settings().clearWeather) {
            for (World world1 : Bukkit.getWorlds()) {
                world1.setStorm(false);
                world1.setThundering(false);
                world1.setTime(this.plugin.settings().fixedTime);
            }
        }
    }

    public Location spawn() {
        return this.spawn == null ? Bukkit.getWorlds().get(0).getSpawnLocation() : this.spawn;
    }

    public void spawn(Location location) {
        this.spawn = location;
        this.plugin.getConfig().set("lobby.spawn", location.getWorld().getName() + ","
                + location.getX() + "," + location.getY() + "," + location.getZ() + ","
                + location.getYaw() + "," + location.getPitch());
        this.plugin.saveConfig();
    }

    /** Prepara al jugador en el lobby. */
    public void sendToLobby(Player player) {
        sendToLobby(player, true);
    }

    public void sendToLobby(Player player, boolean teleport) {
        PlayerState.lobby(player);
        if (this.plugin.settings().teleportOnJoin && teleport) {
            PlayerState.teleport(player, spawn());
        }
        if (this.plugin.settings().giveLobbyItems) {
            giveItems(player);
        }
        player.setScoreboard(this.plugin.board().scoreboard(player));
    }

    /** Items del menu del lobby. */
    public void giveItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(this.plugin.getConfig().getInt("items-lobby.menu.slot", 4),
                menuItem("menu", Material.COMPASS));
        player.getInventory().setItem(this.plugin.getConfig().getInt("items-lobby.stats.slot", 2),
                menuItem("stats", Material.BOOK));
        player.getInventory().setItem(this.plugin.getConfig().getInt("items-lobby.party.slot", 6),
                menuItem("party", Material.NETHER_STAR));
        player.getInventory().setItem(this.plugin.getConfig().getInt("items-lobby.ffa.slot", 8),
                menuItem("ffa", Material.DIAMOND_SWORD));
        player.updateInventory();
    }

    private ItemStack menuItem(String key, Material fallback) {
        String path = "items-lobby." + key + ".";
        String materialName = this.plugin.getConfig().getString(path + "material");
        Material material = materialName == null ? fallback : Material.matchMaterial(materialName);
        if (material == null) {
            material = fallback;
        }
        String name = this.plugin.getConfig().getString(path + "nombre", "<#FFD1DC>" + key);
        List<String> lore = this.plugin.getConfig().getStringList(path + "lore");
        if (lore.isEmpty()) {
            lore = new ArrayList<>();
        }
        return new ItemBuilder(material).name(name).lore(lore).build();
    }

    /** Comprueba si el item es de un slot del menu. */
    public boolean isMenuSlot(int slot) {
        return slot == this.plugin.getConfig().getInt("items-lobby.menu.slot", 4)
                || slot == this.plugin.getConfig().getInt("items-lobby.stats.slot", 2)
                || slot == this.plugin.getConfig().getInt("items-lobby.party.slot", 6)
                || slot == this.plugin.getConfig().getInt("items-lobby.ffa.slot", 8);
    }

    /** Bienvenida al jugador que entra. */
    public void welcome(Player player) {
        PlayerProfile profile = this.plugin.profiles().profile(player);
        if (this.plugin.settings().welcomeMessage) {
            for (var line : this.plugin.messages().getList("lobby.bienvenida-chat",
                    "jugador", player.getName(), "elo", profile.elo())) {
                player.sendMessage(line);
            }
        }
        if (this.plugin.settings().effectTitles && this.plugin.settings().welcomeMessage) {
            PlayerState.title(player,
                    Cc.mm(this.plugin.messages().raw("lobby.bienvenida-titulo")),
                    Cc.mm(this.plugin.messages().raw("lobby.bienvenida-subtitulo")),
                    10, 40, 10);
        }
    }

    /** Comprueba si el jugador debe estar protegido en el lobby. */
    public boolean protectedGameMode(Player player) {
        if (!this.plugin.settings().lobbyProtection) {
            return false;
        }
        if (player.hasPermission("practice.admin") || player.hasPermission("practice.staff")) {
            return false;
        }
        return player.getGameMode() == GameMode.ADVENTURE
                || (!this.plugin.duels().inMatch(player.getUniqueId()) && !this.plugin.ffa().inFfa(player.getUniqueId()));
    }

    /** Lleva al jugador a una partida (kit y modo supervivencia). */
    public void sendToMatch(Player player, Kit kit) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setScoreboard(this.plugin.board().scoreboard(player));
        if (kit != null) {
            PlayerState.giveKit(player, kit);
        }
    }

    public boolean isLobbyWorld(Player player) {
        String worldName = this.plugin.settings().lobbyWorld;
        if (worldName == null || worldName.isEmpty()) {
            return true;
        }
        return player.getWorld().getName().equals(worldName);
    }

    public String worldName(MatchType type) {
        return this.plugin.settings().lobbyWorld;
    }
}
