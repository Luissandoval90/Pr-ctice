package com.practice.hud;

import com.practice.PracticePlugin;
import com.practice.model.Arena;
import com.practice.model.Kit;
import com.practice.model.Leaderboard;
import com.practice.model.MatchType;
import com.practice.model.Party;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import com.practice.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Menus interactivos del plugin (estilo pastel).
 * <p>
 * Optimizacion: cada menu se construye con su propio {@link MenuHolder} para
 * identificar los clicks sin comparar titulos, y los items se generan bajo
 * demanda (no se mantienen inventarios en memoria).
 */
public final class MenuManager {

    private final PracticePlugin plugin;
    private final ExtraMenus extra;

    public MenuManager(PracticePlugin plugin) {
        this.plugin = plugin;
        this.extra = new ExtraMenus(plugin);
    }

    public void openSettings(Player player) {
        this.extra.openSettings(player);
    }

    public void openCosmetics(Player player) {
        this.extra.openCosmetics(player);
    }

    public void openHistory(Player player, Player target) {
        this.extra.openHistory(player, target);
    }

    public void openChallenges(Player player) {
        this.extra.openChallenges(player);
    }

    public void openDivision(Player player) {
        this.extra.openDivision(player);
    }

    public void openEditor(Player player, Kit kit) {
        this.extra.openEditor(player, kit);
    }

    // ------------------------------------------------------------------
    //  Construccion de menus
    // ------------------------------------------------------------------
    public void openMain(Player player) {
        MenuHolder holder = new MenuHolder("main");
        Inventory inventory = Bukkit.createInventory(holder, 45, Cc.mm(this.plugin.messages().raw("menu.titulo-principal")));
        holder.inventory(inventory);
        fill(inventory, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        PlayerProfile profile = this.plugin.profiles().profile(player);
        inventory.setItem(10, new ItemBuilder(Material.DIAMOND_SWORD)
                .name("<gradient:#FFD1DC:#B0E0E6><bold>Duelos</bold></gradient>")
                .lore("<#BFC4CC>Ranked, normal y retos",
                        "<#E8E8F0>Kits: <#FFD1DC>" + this.plugin.kits().all().size(),
                        "<#BFC4CC>En cola: <#FFF5BA>" + this.plugin.queue().waitingPlayers(),
                        "",
                        "<#C1E1C1>Click para elegir kit")
                .hideAll().build());
        inventory.setItem(12, new ItemBuilder(Material.IRON_SWORD)
                .name("<gradient:#FFF5BA:#FFD1DC><bold>FFA</bold></gradient>")
                .lore("<#BFC4CC>Jugadores: <#C1E1C1>" + this.plugin.ffa().total(), "", "<#C1E1C1>Click para entrar")
                .hideAll().build());
        inventory.setItem(14, new ItemBuilder(Material.GOLDEN_APPLE)
                .name("<gradient:#FFF5BA:#E6D5F7><bold>Torneo</bold></gradient>")
                .lore("<#BFC4CC>Eliminacion directa",
                        "<#BFC4CC>Estado: <#E8E8F0>" + this.plugin.tournaments().phase().name(),
                        "",
                        "<#C1E1C1>/torneo unirse")
                .hideAll().build());
        inventory.setItem(16, new ItemBuilder(Material.NETHER_STAR)
                .name("<gradient:#FFF5BA:#C1E1C1><bold>Clasificacion</bold></gradient>")
                .lore("<#BFC4CC>Los mejores del servidor", "", "<#C1E1C1>Click para abrir")
                .hideAll().build());
        inventory.setItem(20, new ItemBuilder(Material.BOOK)
                .name("<gradient:#E6D5F7:#B0E0E6><bold>Estadisticas</bold></gradient>")
                .lore("<#BFC4CC>ELO: <#FFF5BA>" + profile.elo(),
                        "<#BFC4CC>Division: " + profile.division().colored(),
                        "<#BFC4CC>K/D: <#E6D5F7>" + Cc.decimal(profile.kd()))
                .hideAll().build());
        inventory.setItem(22, new ItemBuilder(Material.PLAYER_HEAD).skull(player)
                .name("<gradient:#E6D5F7:#FFD1DC><bold>Party</bold></gradient>")
                .lore("<#BFC4CC>Miembros: <#FFD1DC>" + partySize(player), "", "<#C1E1C1>Click para gestionar")
                .hideAll().build());
        inventory.setItem(24, new ItemBuilder(Material.EMERALD)
                .name("<gradient:#C1E1C1:#B0E0E6><bold>Amigos</bold></gradient>")
                .lore("<#BFC4CC>Conectados y peticiones", "", "<#C1E1C1>/amigo lista")
                .hideAll().build());
        inventory.setItem(28, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("<#B0E0E6><bold>Historial</bold>")
                .lore("<#BFC4CC>Tus ultimas partidas", "", "<#C1E1C1>Click para ver")
                .hideAll().build());
        inventory.setItem(30, new ItemBuilder(Material.BLAZE_POWDER)
                .name("<gradient:#FFD1DC:#FFF5BA><bold>Cosmeticos</bold></gradient>")
                .lore("<#BFC4CC>Monedas: <#FFF5BA>" + profile.coins(), "", "<#C1E1C1>Click para la tienda")
                .hideAll().build());
        inventory.setItem(32, new ItemBuilder(Material.CLOCK)
                .name("<gradient:#FFF5BA:#C1E1C1><bold>Desafios</bold></gradient>")
                .lore("<#BFC4CC>Tres retos diarios", "", "<#C1E1C1>Click para ver")
                .hideAll().build());
        inventory.setItem(34, new ItemBuilder(Material.COMPARATOR)
                .name("<#E6D5F7><bold>Ajustes</bold>")
                .lore("<#BFC4CC>Scoreboard, duelos, sonidos", "", "<#C1E1C1>Click para abrir")
                .hideAll().build());
        inventory.setItem(40, new ItemBuilder(Material.PLAYER_HEAD).skull(player)
                .name(profile.division().badge() + " <#E8E8F0>" + player.getName())
                .lore("<#BFC4CC>Division: " + profile.division().colored(),
                        "<#BFC4CC>ELO: <#FFF5BA>" + profile.elo(),
                        "",
                        "<#C1E1C1>Click para ver las divisiones")
                .hideAll().build());
        player.openInventory(inventory);
        click(player);
    }

    public void openKits(Player player) {
        MenuHolder holder = new MenuHolder("kits").put("modo", "duelo");
        Inventory inventory = Bukkit.createInventory(holder, 54, Cc.mm(this.plugin.messages().raw("menu.titulo-kits")));
        holder.inventory(inventory);
        fill(inventory, Material.PINK_STAINED_GLASS_PANE);
        int[] slots = slots();
        int index = 0;
        for (Kit kit : this.plugin.kits().all()) {
            if (index >= slots.length) {
                break;
            }
            List<String> lore = new ArrayList<>(5);
            lore.add("<#BFC4CC>" + kit.description());
            lore.add("<dark_gray>Tipo: <#FFD1DC>" + kit.type().display());
            lore.add("<dark_gray>En cola: <#FFF5BA>" + this.plugin.queue().size(kit.name()));
            lore.add("");
            lore.add("<#C1E1C1>Click: cola ranked");
            lore.add("<#B0E0E6>Click derecho: retar");
            lore.add("<#FFF5BA>Shift: cola normal");
            lore.add("<#E6D5F7>Drop: editar layout");
            inventory.setItem(slots[index], new ItemBuilder(kit.icon())
                    .name(kit.displayName())
                    .lore(lore)
                    .hideAll().build());
            index++;
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .name("<#FFB3BA>Volver al menu")
                .lore("<#BFC4CC>Click para volver")
                .build());
        inventory.setItem(45, new ItemBuilder(Material.CLOCK)
                .name("<#B0E0E6>Salir de la cola")
                .lore("<#BFC4CC>Click para cancelar la busqueda")
                .build());
        player.openInventory(inventory);
        click(player);
    }

    public void openPlayers(Player player, String kitName) {
        MenuHolder holder = new MenuHolder("players").put("kit", kitName);
        Inventory inventory = Bukkit.createInventory(holder, 54, Cc.mm("<gradient:#FFD1DC:#B0E0E6><bold>Retar con " + kitName + "</bold></gradient>"));
        holder.inventory(inventory);
        fill(inventory, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        int[] slots = slots();
        int index = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (target.getUniqueId().equals(player.getUniqueId()) || index >= slots.length) {
                continue;
            }
            PlayerProfile profile = this.plugin.profiles().profile(target);
            List<String> lore = new ArrayList<>(5);
            lore.add("<#BFC4CC>ELO: <#FFF5BA>" + profile.elo());
            lore.add("<#BFC4CC>Kills: <#C1E1C1>" + profile.kills() + " <dark_gray>| <#BFC4CC>K/D: <#E6D5F7>" + Cc.decimal(profile.kd()));
            lore.add("<#BFC4CC>Racha: <#FFD1DC>" + profile.streak());
            lore.add("");
            lore.add(this.plugin.duels().inMatch(target.getUniqueId())
                    ? "<#FFB3BA>Ya esta en partida"
                    : "<#C1E1C1>Click para retar");
            inventory.setItem(slots[index], new ItemBuilder(Material.PLAYER_HEAD)
                    .skull(target)
                    .name("<#E8E8F0>" + target.getName())
                    .lore(lore)
                    .hideAll().build());
            index++;
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER)
                .name("<#FFB3BA>Volver a los kits")
                .build());
        player.openInventory(inventory);
        click(player);
    }

    public void openStats(Player player, Player target) {
        MenuHolder holder = new MenuHolder("stats").put("uuid", target.getUniqueId().toString());
        Inventory inventory = Bukkit.createInventory(holder, 27, Cc.mm(this.plugin.messages().raw("menu.titulo-stats")));
        holder.inventory(inventory);
        fill(inventory, Material.PINK_STAINED_GLASS_PANE);
        PlayerProfile profile = this.plugin.profiles().profile(target);
        inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                .skull(target)
                .name("<gradient:#FFD1DC:#B0E0E6><bold>" + target.getName() + "</bold></gradient>")
                .lore("<#BFC4CC>ELO: <#FFF5BA>" + profile.elo(),
                        "<#BFC4CC>Elo maximo: <#E8E8F0>" + profile.eloPeak())
                .hideAll().build());
        inventory.setItem(10, statItem(Material.DIAMOND_SWORD, "Duelos",
                "<#BFC4CC>Victorias: <#C1E1C1>" + profile.wins(),
                "<#BFC4CC>Derrotas: <#FFB3BA>" + profile.losses(),
                "<#BFC4CC>Winrate: <#FFD1DC>" + Cc.decimal(profile.winRate()) + "%"));
        inventory.setItem(12, statItem(Material.IRON_SWORD, "Combate",
                "<#BFC4CC>Kills: <#C1E1C1>" + profile.kills(),
                "<#BFC4CC>Muertes: <#FFB3BA>" + profile.deaths(),
                "<#BFC4CC>K/D: <#E6D5F7>" + Cc.decimal(profile.kd())));
        inventory.setItem(14, statItem(Material.NETHER_STAR, "Rachas",
                "<#BFC4CC>Racha actual: <#FFD1DC>" + profile.streak(),
                "<#BFC4CC>Mejor racha: <#FFF5BA>" + profile.bestStreak(),
                "<#BFC4CC>Partidas: <#E8E8F0>" + (profile.wins() + profile.losses())));
        inventory.setItem(16, statItem(Material.CLOCK, "Progreso",
                "<#BFC4CC>Tiempo jugado: <#E8E8F0>" + profile.formattedPlaytime(),
                "<#BFC4CC>FFA kills: <#C1E1C1>" + profile.ffaKills(),
                "<#BFC4CC>FFA K/D: <#E6D5F7>" + Cc.decimal(profile.ffaKd())));
        inventory.setItem(22, new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Cerrar").build());
        player.openInventory(inventory);
        click(player);
    }

    private ItemStack statItem(Material material, String title, String... lines) {
        return new ItemBuilder(material)
                .name("<gradient:#FFD1DC:#B0E0E6><bold>" + title + "</bold></gradient>")
                .lore(lines)
                .hideAll().build();
    }

    public void openTop(Player player, Leaderboard.Type type) {
        MenuHolder holder = new MenuHolder("top").put("tipo", type.id());
        Inventory inventory = Bukkit.createInventory(holder, 54, Cc.mm(this.plugin.messages().raw("menu.titulo-top")));
        holder.inventory(inventory);
        fill(inventory, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        Leaderboard.Type[] types = Leaderboard.Type.values();
        int button = 2;
        for (Leaderboard.Type category : types) {
            boolean selected = category == type;
            inventory.setItem(button++, new ItemBuilder(icon(category))
                    .name((selected ? "<#C1E1C1>" : "<#BFC4CC>") + category.display())
                    .lore(selected ? "<#FFF5BA>Categoria actual" : "<#BFC4CC>Click para cambiar")
                    .glow(selected)
                    .hideAll().build());
        }
        List<Leaderboard.Entry> entries = this.plugin.profiles().top(type, 10);
        int slot = 18;
        for (Leaderboard.Entry entry : entries) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(entry.uuid());
            inventory.setItem(slot++, new ItemBuilder(Material.PLAYER_HEAD)
                    .skull(offline)
                    .name("<#FFD1DC>" + entry.position() + ". <#E8E8F0>" + entry.name())
                    .lore("<#BFC4CC>" + type.display() + ": <#FFF5BA>" + Cc.number((long) entry.value()))
                    .hideAll().build());
        }
        if (entries.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Sin datos todavia").build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Cerrar").build());
        player.openInventory(inventory);
        click(player);
    }

    private Material icon(Leaderboard.Type type) {
        return switch (type) {
            case KILLS -> Material.DIAMOND_SWORD;
            case ELO -> Material.NETHER_STAR;
            case WINS -> Material.GOLDEN_APPLE;
            case STREAK -> Material.BLAZE_POWDER;
            case FFA_KILLS -> Material.IRON_SWORD;
        };
    }

    public void openFfa(Player player) {
        MenuHolder holder = new MenuHolder("ffa");
        Inventory inventory = Bukkit.createInventory(holder, 27, Cc.mm(this.plugin.messages().raw("menu.titulo-ffa")));
        holder.inventory(inventory);
        fill(inventory, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        int slot = 10;
        for (Arena arena : this.plugin.ffa().arenas()) {
            if (slot > 16) {
                break;
            }
            inventory.setItem(slot++, new ItemBuilder(Material.DIAMOND_SWORD)
                    .name("<gradient:#FFF5BA:#FFD1DC><bold>" + arena.name() + "</bold></gradient>")
                    .lore("<#BFC4CC>Jugadores: <#C1E1C1>" + this.plugin.ffa().population(arena.name()),
                            "<#BFC4CC>Region: <#E8E8F0>" + arena.volume() + " bloques",
                            "",
                            "<#C1E1C1>Click para entrar")
                    .hideAll().build());
        }
        if (slot == 10) {
            inventory.setItem(13, new ItemBuilder(Material.BARRIER)
                    .name("<#FFB3BA>Sin arenas FFA")
                    .lore("<#BFC4CC>Pide al staff que cree una con /arena")
                    .build());
        }
        inventory.setItem(22, new ItemBuilder(Material.OAK_DOOR)
                .name("<#FFB3BA>Salir de la FFA")
                .lore("<#BFC4CC>Click para volver al lobby")
                .build());
        player.openInventory(inventory);
        click(player);
    }

    public void openParty(Player player) {
        Party party = this.plugin.parties().of(player.getUniqueId());
        MenuHolder holder = new MenuHolder("party");
        Inventory inventory = Bukkit.createInventory(holder, 27, Cc.mm(this.plugin.messages().raw("menu.titulo-party")));
        holder.inventory(inventory);
        fill(inventory, Material.PINK_STAINED_GLASS_PANE);
        if (party == null) {
            inventory.setItem(13, new ItemBuilder(Material.NETHER_STAR)
                    .name("<gradient:#E6D5F7:#FFD1DC><bold>Crear party</bold></gradient>")
                    .lore("<#BFC4CC>Forma un grupo para jugar duelos en equipo",
                            "",
                            "<#C1E1C1>Click para crear")
                    .hideAll().build());
        } else {
            inventory.setItem(4, new ItemBuilder(Material.PLAYER_HEAD)
                    .skull(Bukkit.getOfflinePlayer(party.owner()))
                    .name("<gradient:#E6D5F7:#B0E0E6><bold>Party de " + this.plugin.parties().ownerName(party) + "</bold></gradient>")
                    .lore("<#BFC4CC>Miembros: <#FFD1DC>" + party.size() + "/" + com.practice.manager.PartyManager.MAX_SIZE)
                    .hideAll().build());
            int slot = 10;
            for (UUID member : party.members()) {
                if (slot > 16) {
                    break;
                }
                OfflinePlayer offline = Bukkit.getOfflinePlayer(member);
                PlayerProfile profile = this.plugin.profiles().profileOrCreate(member, "Jugador");
                boolean owner = party.isOwner(member);
                inventory.setItem(slot++, new ItemBuilder(Material.PLAYER_HEAD)
                        .skull(offline)
                        .name((owner ? "<#FFF5BA>" : "<#E8E8F0>") + profile.name() + (owner ? " <dark_gray>(lider)" : ""))
                        .lore("<#BFC4CC>ELO: <#FFF5BA>" + profile.elo(),
                                owner ? "<#BFC4CC>Lider de la party" : "<#FFB3BA>Click derecho para expulsar")
                        .hideAll().build());
            }
            inventory.setItem(22, new ItemBuilder(Material.WRITABLE_BOOK)
                    .name("<#C1E1C1>Invitar jugador")
                    .lore("<#BFC4CC>Click para ver jugadores conectados")
                    .hideAll().build());
            inventory.setItem(18, new ItemBuilder(Material.OAK_DOOR)
                    .name("<#FFB3BA>Salir de la party")
                    .lore("<#BFC4CC>Click para salir")
                    .build());
            if (party.isOwner(player.getUniqueId())) {
                inventory.setItem(26, new ItemBuilder(Material.BARRIER)
                        .name("<#FFB3BA>Disolver party")
                        .lore("<#BFC4CC>Click para disolver la party")
                        .build());
            }
        }
        player.openInventory(inventory);
        click(player);
    }

    public void openInvite(Player player) {
        Party party = this.plugin.parties().of(player.getUniqueId());
        if (party == null) {
            this.plugin.messages().send(player, "parties.no-en-party");
            return;
        }
        MenuHolder holder = new MenuHolder("invite");
        Inventory inventory = Bukkit.createInventory(holder, 54, Cc.mm("<gradient:#C1E1C1:#B0E0E6><bold>Invitar a la party</bold></gradient>"));
        holder.inventory(inventory);
        fill(inventory, Material.PINK_STAINED_GLASS_PANE);
        int[] slots = slots();
        int index = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (party.isMember(target.getUniqueId()) || index >= slots.length) {
                continue;
            }
            inventory.setItem(slots[index++], new ItemBuilder(Material.PLAYER_HEAD)
                    .skull(target)
                    .name("<#E8E8F0>" + target.getName())
                    .lore("<#BFC4CC>Click para invitar")
                    .hideAll().build());
        }
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Volver").build());
        player.openInventory(inventory);
        click(player);
    }

    // ------------------------------------------------------------------
    //  Manejo de clicks
    // ------------------------------------------------------------------
    public void handleClick(Player player, MenuHolder holder, int slot, ClickType click, ItemStack item) {
        switch (holder.type()) {
            case "main" -> {
                switch (slot) {
                    case 10 -> openKits(player);
                    case 12 -> openFfa(player);
                    case 14 -> player.performCommand("torneo info");
                    case 16 -> openTop(player, Leaderboard.Type.ELO);
                    case 20 -> openStats(player, player);
                    case 22 -> openParty(player);
                    case 24 -> player.performCommand("amigo lista");
                    case 28 -> openHistory(player, player);
                    case 30 -> openCosmetics(player);
                    case 32 -> openChallenges(player);
                    case 34 -> openSettings(player);
                    case 40 -> openDivision(player);
                    default -> {
                    }
                }
            }
            case "kits" -> handleKitsClick(player, slot, click, item);
            case "players" -> handlePlayersClick(player, holder, slot, item);
            case "stats" -> {
                if (slot == 22) {
                    player.closeInventory();
                }
            }
            case "top" -> handleTopClick(player, holder, slot, item);
            case "ffa" -> handleFfaClick(player, slot);
            case "party" -> handlePartyClick(player, slot, click);
            case "invite" -> handleInviteClick(player, slot, item);
            default -> this.extra.handle(player, holder, slot, click, item);
        }
    }

    private void handleKitsClick(Player player, int slot, ClickType click, ItemStack item) {
        if (slot == 49) {
            openMain(player);
            return;
        }
        if (slot == 45) {
            if (this.plugin.queue().queued(player.getUniqueId())) {
                this.plugin.queue().leave(player);
            } else {
                this.plugin.messages().send(player, "duelos.sin-peticion");
            }
            player.closeInventory();
            this.plugin.board().update(player);
            return;
        }
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        int[] slots = slots();
        int index = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        List<Kit> kits = new ArrayList<>(this.plugin.kits().all());
        if (index >= kits.size()) {
            return;
        }
        Kit kit = kits.get(index);
        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            openEditor(player, kit);
            return;
        }
        if (click.isRightClick()) {
            openPlayers(player, kit.name());
            return;
        }
        boolean ranked = !click.isShiftClick();
        if (this.plugin.queue().queued(player.getUniqueId())) {
            this.plugin.queue().leave(player);
        }
        String error = this.plugin.queue().join(player, kit, ranked);
        if (error != null) {
            this.plugin.messages().send(player, error);
            return;
        }
        player.closeInventory();
        this.plugin.board().update(player);
        if (this.plugin.settings().sounds) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.6F);
        }
    }

    private void handlePlayersClick(Player player, MenuHolder holder, int slot, ItemStack item) {
        if (slot == 49) {
            openKits(player);
            return;
        }
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()) {
            return;
        }
        String kitName = holder.string("kit");
        Kit kit = this.plugin.kits().get(kitName);
        if (kit == null) {
            return;
        }
        String name = item.getItemMeta().hasDisplayName()
                ? Cc.plain(item.getItemMeta().displayName()) : null;
        if (name == null) {
            return;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            this.plugin.messages().send(player, "generales.jugador-desconocido");
            return;
        }
        player.closeInventory();
        String error = this.plugin.duels().request(player, target, kit,
                kit.maxRounds() > 0 ? kit.maxRounds() : this.plugin.settings().defaultRounds);
        if (error != null) {
            this.plugin.messages().send(player, error);
        }
    }

    private void handleTopClick(Player player, MenuHolder holder, int slot, ItemStack item) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot >= 2 && slot <= 6) {
            Leaderboard.Type type = Leaderboard.Type.values()[slot - 2];
            openTop(player, type);
        }
    }

    private void handleFfaClick(Player player, int slot) {
        if (slot == 22) {
            if (this.plugin.ffa().inFfa(player.getUniqueId())) {
                this.plugin.ffa().leave(player);
            } else {
                this.plugin.messages().send(player, "ffa.no-existe");
            }
            player.closeInventory();
            return;
        }
        List<Arena> arenas = this.plugin.ffa().arenas();
        if (slot < 10 || slot - 10 >= arenas.size()) {
            return;
        }
        Arena arena = arenas.get(slot - 10);
        player.closeInventory();
        this.plugin.ffa().join(player, arena.name());
    }

    private void handlePartyClick(Player player, int slot, ClickType click) {
        Party party = this.plugin.parties().of(player.getUniqueId());
        if (party == null) {
            if (slot == 13) {
                if (this.plugin.parties().create(player) == null) {
                    this.plugin.messages().send(player, "parties.ya-en-party");
                } else {
                    this.plugin.messages().send(player, "parties.creada");
                }
                player.closeInventory();
                this.plugin.tab().updateName(player);
            }
            return;
        }
        switch (slot) {
            case 22 -> openInvite(player);
            case 18 -> {
                boolean wasOwner = party.isOwner(player.getUniqueId());
                if (wasOwner) {
                    this.plugin.parties().broadcast(party, Cc.mm(this.plugin.messages().raw("parties.disuelta")));
                    this.plugin.parties().disband(party);
                } else {
                    this.plugin.parties().leave(player);
                    this.plugin.parties().broadcast(party, this.plugin.messages().get("parties.salio", "jugador", player.getName()));
                }
                player.closeInventory();
                this.plugin.tab().updateName(player);
            }
            case 26 -> {
                if (party.isOwner(player.getUniqueId())) {
                    this.plugin.parties().broadcast(party, Cc.mm(this.plugin.messages().raw("parties.disuelta")));
                    this.plugin.parties().disband(party);
                    player.closeInventory();
                } else {
                    this.plugin.messages().send(player, "parties.solo-duenio");
                }
            }
            default -> {
                if (slot >= 10 && slot <= 16 && click.isRightClick() && party.isOwner(player.getUniqueId())) {
                    List<UUID> members = new ArrayList<>(party.members());
                    int index = slot - 10;
                    if (index < members.size()) {
                        UUID target = members.get(index);
                        if (!target.equals(player.getUniqueId())) {
                            this.plugin.parties().kick(party, target);
                            this.plugin.messages().send(player, "parties.expulsar-ok", "jugador",
                                    this.plugin.duels().nameOf(target));
                            Player kicked = Bukkit.getPlayer(target);
                            if (kicked != null) {
                                this.plugin.messages().send(kicked, "parties.expulsado");
                                this.plugin.tab().updateName(kicked);
                            }
                            openParty(player);
                        }
                    }
                }
            }
        }
    }

    private void handleInviteClick(Player player, int slot, ItemStack item) {
        if (slot == 49) {
            openParty(player);
            return;
        }
        if (item == null || item.getType() != Material.PLAYER_HEAD || !item.hasItemMeta()
                || item.getItemMeta().displayName() == null) {
            return;
        }
        String name = Cc.plain(item.getItemMeta().displayName());
        Player target = Bukkit.getPlayerExact(name);
        Party party = this.plugin.parties().of(player.getUniqueId());
        if (target == null || party == null) {
            return;
        }
        if (party.size() >= com.practice.manager.PartyManager.MAX_SIZE) {
            this.plugin.messages().send(player, "parties.llena", "max", com.practice.manager.PartyManager.MAX_SIZE);
            return;
        }
        if (!this.plugin.profiles().profile(target).partyInvites()) {
            this.plugin.messages().send(player, "parties.invitaciones-off", "jugador", target.getName());
            return;
        }
        party.invite(target.getUniqueId());
        this.plugin.messages().send(player, "parties.invitacion-enviada", "jugador", target.getName());
        this.plugin.messages().sendList(target, "parties.invitacion-recibida",
                "duenio", this.plugin.parties().ownerName(party));
        target.sendMessage(Cc.mm("<#C1E1C1>[Aceptar]").clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party aceptar"))
                .append(net.kyori.adventure.text.Component.text(" "))
                .append(Cc.mm("<#FFB3BA>[Rechazar]").clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/party rechazar"))));
        if (this.plugin.settings().sounds) {
            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 1.8F);
        }
        openInvite(player);
    }

    // ------------------------------------------------------------------
    //  Utilidades
    // ------------------------------------------------------------------
    private void fill(Inventory inventory, Material material) {
        ItemStack filler = new ItemBuilder(material)
                .name("<#E8E8F0> ")
                .build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private int[] slots() {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    }

    private void click(Player player) {
        if (this.plugin.settings().sounds) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4F, 1.6F);
        }
    }

    private int partySize(Player player) {
        Party party = this.plugin.parties().of(player.getUniqueId());
        return party == null ? 0 : party.size();
    }
}
