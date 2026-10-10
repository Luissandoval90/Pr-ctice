package com.practice.hud;

import com.practice.PracticePlugin;
import com.practice.manager.ChallengeManager;
import com.practice.model.Cosmetic;
import com.practice.model.Division;
import com.practice.model.Kit;
import com.practice.model.MatchRecord;
import com.practice.model.PlayerProfile;
import com.practice.util.Cc;
import com.practice.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Menus nuevos de la v2: ajustes, cosmeticos, historial, desafios, division y editor de kits.
 */
public final class ExtraMenus {

    private final PracticePlugin plugin;

    public ExtraMenus(PracticePlugin plugin) {
        this.plugin = plugin;
    }

    public void openSettings(Player player) {
        PlayerProfile profile = this.plugin.profiles().profile(player);
        MenuHolder holder = new MenuHolder("settings");
        Inventory inventory = inventory(holder, 27, "<gradient:#E6D5F7:#B0E0E6><bold>Ajustes</bold></gradient>");
        fill(inventory, Material.PINK_STAINED_GLASS_PANE);
        inventory.setItem(10, toggle(Material.PAINTING, "Scoreboard", profile.showBoard()));
        inventory.setItem(11, toggle(Material.IRON_SWORD, "Peticiones de duelo", profile.duelRequests()));
        inventory.setItem(12, toggle(Material.NETHER_STAR, "Invitaciones de party", profile.partyInvites()));
        inventory.setItem(13, toggle(Material.ENDER_EYE, "Espectadores", profile.allowSpectators()));
        inventory.setItem(14, toggle(Material.NOTE_BLOCK, "Sonidos", profile.personalSounds()));
        inventory.setItem(16, new ItemBuilder(Material.PLAYER_HEAD).skull(player)
                .name(profile.division().colored() + " <bold>" + profile.division().display() + "</bold>")
                .lore("<#BFC4CC>ELO: <#FFF5BA>" + profile.elo(),
                        "<#BFC4CC>Monedas: <#E6D5F7>" + profile.coins(),
                        "<#BFC4CC>" + Cc.bar(profile.division().progress(profile.elo()), 10, profile.division().color(), "#3A3A44"))
                .hideAll().build());
        inventory.setItem(22, closeItem());
        player.openInventory(inventory);
        click(player);
    }

    public void openCosmetics(Player player) {
        PlayerProfile profile = this.plugin.profiles().profile(player);
        MenuHolder holder = new MenuHolder("cosmetics");
        Inventory inventory = inventory(holder, 54, "<gradient:#FFD1DC:#FFF5BA><bold>Cosmeticos</bold></gradient>");
        fill(inventory, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        int slot = 10;
        for (Cosmetic cosmetic : Cosmetic.values()) {
            if (slot == 17 || slot == 18) {
                slot = 19;
            }
            if (slot == 26 || slot == 27) {
                slot = 28;
            }
            if (slot >= 35) {
                break;
            }
            boolean owned = cosmetic.price() == 0 || cosmetic.id().equals(profile.killEffect()) || cosmetic.id().equals(profile.winEffect());
            boolean selected = cosmetic.id().equals(profile.killEffect()) || cosmetic.id().equals(profile.winEffect());
            List<String> lore = new ArrayList<>();
            lore.add("<#BFC4CC>Tipo: <#FFD1DC>" + (cosmetic.killEffect() && cosmetic.winEffect() ? "ambos" : cosmetic.killEffect() ? "kill" : "victoria"));
            lore.add("<#BFC4CC>Precio: <#FFF5BA>" + (cosmetic.price() == 0 ? "gratis" : cosmetic.price() + " monedas"));
            lore.add("<#BFC4CC>Tus monedas: <#E6D5F7>" + profile.coins());
            lore.add("");
            lore.add(selected ? "<#C1E1C1>Equipado" : owned ? "<#B0E0E6>Click para equipar" : "<#FFF5BA>Click para comprar y equipar");
            inventory.setItem(slot++, new ItemBuilder(icon(cosmetic))
                    .name((selected ? "<#C1E1C1>" : "<#E8E8F0>") + cosmetic.display())
                    .lore(lore)
                    .glow(selected)
                    .hideAll().build());
        }
        inventory.setItem(49, closeItem());
        player.openInventory(inventory);
        click(player);
    }

    public void openHistory(Player player, Player target) {
        PlayerProfile profile = this.plugin.profiles().profile(target);
        MenuHolder holder = new MenuHolder("history").put("uuid", target.getUniqueId().toString());
        Inventory inventory = inventory(holder, 54, "<gradient:#B0E0E6:#C1E1C1><bold>Historial</bold></gradient>");
        fill(inventory, Material.PINK_STAINED_GLASS_PANE);
        int slot = 10;
        List<MatchRecord> history = profile.history();
        if (history.isEmpty()) {
            inventory.setItem(22, new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Sin partidas todavia").build());
        }
        for (MatchRecord record : history) {
            if (slot >= 44) {
                break;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            String when = Cc.time(System.currentTimeMillis() - record.time());
            inventory.setItem(slot++, new ItemBuilder(record.won() ? Material.LIME_DYE : Material.RED_DYE)
                    .name((record.won() ? "<#C1E1C1>Victoria" : "<#FFB3BA>Derrota") + " <#E8E8F0>vs " + record.opponent())
                    .lore("<#BFC4CC>Kit: <#FFD1DC>" + record.kit(),
                            "<#BFC4CC>Marcador: <#E8E8F0>" + record.score(),
                            "<#BFC4CC>ELO: " + (record.eloDelta() >= 0 ? "<#C1E1C1>+" : "<#FFB3BA>") + record.eloDelta(),
                            "<#BFC4CC>Modo: <#FFF5BA>" + (record.ranked() ? "Ranked" : "Normal"),
                            "<#BFC4CC>Hace: <#E8E8F0>" + when)
                    .hideAll().build());
        }
        inventory.setItem(49, closeItem());
        player.openInventory(inventory);
        click(player);
    }

    public void openChallenges(Player player) {
        PlayerProfile profile = this.plugin.profiles().profile(player);
        this.plugin.challenges().ensure(profile);
        MenuHolder holder = new MenuHolder("challenges");
        Inventory inventory = inventory(holder, 27, "<gradient:#FFF5BA:#C1E1C1><bold>Desafios de hoy</bold></gradient>");
        fill(inventory, Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        int slot = 11;
        for (ChallengeManager.Def def : this.plugin.challenges().today()) {
            int progress = profile.challengeProgress(def.id());
            boolean done = profile.challengeClaimed().contains(def.id());
            inventory.setItem(slot++, new ItemBuilder(done ? Material.LIME_DYE : Material.PAPER)
                    .name((done ? "<#C1E1C1>" : "<#FFF5BA>") + def.name())
                    .lore("<#BFC4CC>" + def.description(),
                            "<#BFC4CC>Progreso: <#E8E8F0>" + Math.min(progress, def.goal()) + "/" + def.goal(),
                            "<#BFC4CC>" + Cc.bar(progress / (double) def.goal(), 10, "#C1E1C1", "#3A3A44"),
                            "<#BFC4CC>Recompensa: <#FFF5BA>" + def.reward() + " monedas",
                            done ? "<#C1E1C1>Completado" : "<#BFC4CC>Se reinicia a medianoche")
                    .glow(done)
                    .hideAll().build());
        }
        inventory.setItem(22, closeItem());
        player.openInventory(inventory);
        click(player);
    }

    public void openDivision(Player player) {
        PlayerProfile profile = this.plugin.profiles().profile(player);
        MenuHolder holder = new MenuHolder("division");
        Inventory inventory = inventory(holder, 45, "<gradient:#FFD1DC:#B0E0E6><bold>Divisiones</bold></gradient>");
        fill(inventory, Material.PINK_STAINED_GLASS_PANE);
        int slot = 10;
        for (Division division : Division.values()) {
            if (slot == 17) {
                slot = 19;
            }
            if (slot == 26) {
                slot = 28;
            }
            boolean current = division == profile.division();
            inventory.setItem(slot++, new ItemBuilder(current ? Material.NETHER_STAR : Material.PAPER)
                    .name(division.badge() + " " + division.colored())
                    .lore("<#BFC4CC>ELO: <#FFF5BA>" + division.min() + " - " + (division.top() ? "∞" : String.valueOf(division.max())),
                            current ? "<#C1E1C1>Tu division" : "<#BFC4CC> ")
                    .glow(current)
                    .hideAll().build());
        }
        inventory.setItem(40, closeItem());
        player.openInventory(inventory);
        click(player);
    }

    public void openEditor(Player player, Kit kit) {
        MenuHolder holder = new MenuHolder("editor").put("kit", kit.name());
        Inventory inventory = inventory(holder, 54, "<gradient:#C1E1C1:#B0E0E6><bold>Editor " + kit.name() + "</bold></gradient>");
        ItemStack[] contents = kit.inventory();
        for (int slot = 0; slot < 36; slot++) {
            if (contents != null && slot < contents.length && contents[slot] != null) {
                inventory.setItem(slot, contents[slot].clone());
            }
        }
        ItemStack[] armor = kit.armor();
        if (armor != null) {
            inventory.setItem(45, clone(armor, 0));
            inventory.setItem(46, clone(armor, 1));
            inventory.setItem(47, clone(armor, 2));
            inventory.setItem(48, clone(armor, 3));
        }
        inventory.setItem(50, kit.offHand() == null ? null : kit.offHand().clone());
        inventory.setItem(51, new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Mano vacia").lore("<#BFC4CC>Aqui aparece lo que tienes cogido").build());
        inventory.setItem(52, new ItemBuilder(Material.CLOCK).name("<#FFF5BA>Restablecer").lore("<#BFC4CC>Vuelve al kit original").build());
        inventory.setItem(53, new ItemBuilder(Material.LIME_DYE).name("<#C1E1C1>Guardar layout").lore("<#BFC4CC>Se usara en tus proximas partidas").build());
        player.openInventory(inventory);
        click(player);
    }

    public boolean handle(Player player, MenuHolder holder, int slot, ClickType click, ItemStack item) {
        return switch (holder.type()) {
            case "settings" -> {
                toggleSetting(player, slot);
                yield true;
            }
            case "cosmetics" -> {
                buy(player, slot, item);
                yield true;
            }
            case "history", "challenges", "division" -> {
                if (slot == 49 || slot == 22 || slot == 40) {
                    player.closeInventory();
                }
                yield true;
            }
            case "editor" -> {
                edit(player, holder, slot);
                yield true;
            }
            default -> false;
        };
    }

    private void toggleSetting(Player player, int slot) {
        PlayerProfile profile = this.plugin.profiles().profile(player);
        switch (slot) {
            case 10 -> profile.showBoard(!profile.showBoard());
            case 11 -> profile.duelRequests(!profile.duelRequests());
            case 12 -> profile.partyInvites(!profile.partyInvites());
            case 13 -> profile.allowSpectators(!profile.allowSpectators());
            case 14 -> profile.personalSounds(!profile.personalSounds());
            case 22 -> {
                player.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }
        this.plugin.profiles().markDirty();
        this.plugin.board().update(player);
        openSettings(player);
    }

    private void buy(Player player, int slot, ItemStack item) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (item == null || !item.hasItemMeta() || item.getItemMeta().displayName() == null) {
            return;
        }
        String plain = Cc.plain(item.getItemMeta().displayName());
        Cosmetic cosmetic = null;
        for (Cosmetic candidate : Cosmetic.values()) {
            if (plain.contains(candidate.display())) {
                cosmetic = candidate;
                break;
            }
        }
        if (cosmetic == null) {
            return;
        }
        PlayerProfile profile = this.plugin.profiles().profile(player);
        boolean owned = cosmetic.price() == 0
                || cosmetic.id().equals(profile.killEffect())
                || cosmetic.id().equals(profile.winEffect())
                || profile.coins() >= cosmetic.price() && alreadyBought(profile, cosmetic);
        if (!owned && cosmetic.price() > 0) {
            if (!profile.takeCoins(cosmetic.price())) {
                this.plugin.messages().send(player, "economia.sin-monedas", "precio", cosmetic.price());
                return;
            }
            this.plugin.messages().send(player, "economia.comprado", "nombre", cosmetic.display(), "precio", cosmetic.price());
        }
        if (cosmetic.killEffect()) {
            profile.killEffect(cosmetic.id());
        }
        if (cosmetic.winEffect()) {
            profile.winEffect(cosmetic.id());
        }
        if (cosmetic == Cosmetic.NINGUNO) {
            profile.killEffect("ninguno");
            profile.winEffect("ninguno");
        }
        this.plugin.profiles().markDirty();
        openCosmetics(player);
    }

    private boolean alreadyBought(PlayerProfile profile, Cosmetic cosmetic) {
        // Una vez gastadas las monedas el efecto queda equipado; si lo cambia, puede volver gratis
        // solo si es el que tiene puesto. El resto se vuelve a pagar: evita un inventario de desbloqueos extra.
        return false;
    }

    private void edit(Player player, MenuHolder holder, int slot) {
        Inventory inventory = holder.getInventory();
        if (slot == 53) {
            saveEditor(player, holder, inventory);
            return;
        }
        if (slot == 52) {
            Kit kit = this.plugin.kits().get(holder.string("kit"));
            if (kit != null) {
                this.plugin.layouts().reset(player.getUniqueId(), kit.name());
                this.plugin.messages().send(player, "editor.restablecido", "kit", kit.name());
                openEditor(player, kit);
            }
            return;
        }
        if (!editable(slot)) {
            return;
        }
        ItemStack cursor = inventory.getItem(51);
        boolean emptyCursor = cursor == null || cursor.getType() == Material.BARRIER || cursor.getType() == Material.AIR;
        ItemStack current = inventory.getItem(slot);
        if (emptyCursor) {
            inventory.setItem(51, current == null ? new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Mano vacia").build() : current.clone());
            inventory.setItem(slot, null);
        } else {
            inventory.setItem(slot, cursor.clone());
            inventory.setItem(51, current == null ? new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Mano vacia").build() : current.clone());
        }
    }

    private void saveEditor(Player player, MenuHolder holder, Inventory inventory) {
        String kitName = holder.string("kit");
        ItemStack hand = inventory.getItem(51);
        if (hand != null && hand.getType() != Material.BARRIER && hand.getType() != Material.AIR) {
            for (int slot = 0; slot < 36; slot++) {
                if (inventory.getItem(slot) == null) {
                    inventory.setItem(slot, hand);
                    inventory.setItem(51, null);
                    break;
                }
            }
        }
        ItemStack[] layout = new ItemStack[41];
        for (int slot = 0; slot < 36; slot++) {
            layout[slot] = clean(inventory.getItem(slot));
        }
        layout[36] = clean(inventory.getItem(45));
        layout[37] = clean(inventory.getItem(46));
        layout[38] = clean(inventory.getItem(47));
        layout[39] = clean(inventory.getItem(48));
        layout[40] = clean(inventory.getItem(50));
        this.plugin.layouts().save(player.getUniqueId(), kitName, layout);
        this.plugin.messages().send(player, "editor.guardado", "kit", kitName);
        player.closeInventory();
    }

    private ItemStack clean(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getType() == Material.BARRIER) {
            return null;
        }
        return item.clone();
    }

    private boolean editable(int slot) {
        return slot < 36 || (slot >= 45 && slot <= 48) || slot == 50;
    }

    private ItemStack toggle(Material material, String name, boolean on) {
        return new ItemBuilder(material)
                .name("<#E8E8F0>" + name)
                .lore(on ? "<#C1E1C1>Activado" : "<#FFB3BA>Desactivado", "", "<#BFC4CC>Click para cambiar")
                .glow(on)
                .hideAll().build();
    }

    private Material icon(Cosmetic cosmetic) {
        return switch (cosmetic) {
            case CORAZONES -> Material.PINK_DYE;
            case LLAMAS -> Material.BLAZE_POWDER;
            case TOTEM -> Material.TOTEM_OF_UNDYING;
            case ALMAS -> Material.SOUL_LANTERN;
            case FUEGOS -> Material.FIREWORK_ROCKET;
            case ESTRELLAS -> Material.END_ROD;
            case BRILLO -> Material.GLOW_BERRIES;
            default -> Material.BARRIER;
        };
    }

    private ItemStack clone(ItemStack[] items, int index) {
        return items != null && index < items.length && items[index] != null ? items[index].clone() : null;
    }

    private Inventory inventory(MenuHolder holder, int size, String title) {
        Inventory inventory = Bukkit.createInventory(holder, size, Cc.mm(title));
        holder.inventory(inventory);
        return inventory;
    }

    private void fill(Inventory inventory, Material material) {
        ItemStack filler = new ItemBuilder(material).name("<#E8E8F0> ").build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack closeItem() {
        return new ItemBuilder(Material.BARRIER).name("<#FFB3BA>Cerrar").build();
    }

    private void click(Player player) {
        if (this.plugin.settings().sounds) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4F, 1.6F);
        }
    }
}
