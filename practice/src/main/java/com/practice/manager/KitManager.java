package com.practice.manager;

import com.practice.PracticePlugin;
import com.practice.model.Kit;
import com.practice.model.MatchType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gestiona los kits del servidor.
 * <p>
 * Los kits por defecto se generan en memoria la primera vez y se guardan en
 * kits.yml, de forma que el servidor es jugable sin configuracion manual.
 */
public final class KitManager {

    private final PracticePlugin plugin;
    private final Map<String, Kit> kits = new LinkedHashMap<>(32);
    private final File file;

    public KitManager(PracticePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
    }

    public void load() {
        this.kits.clear();
        if (!this.file.exists()) {
            this.registerDefaults();
            this.save();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection root = config.getConfigurationSection("kits");
        if (root == null) {
            this.registerDefaults();
            this.save();
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            Kit kit = new Kit(key);
            kit.displayName(section.getString("nombre", "<#FFD1DC>" + key));
            kit.description(section.getString("descripcion", "Kit de Practice"));
            Material icon = Material.matchMaterial(section.getString("icono", "DIAMOND_SWORD"));
            kit.icon(icon == null ? Material.DIAMOND_SWORD : icon);
            kit.type(MatchType.fromId(section.getString("tipo", "SOLO")));
            kit.cooldown(section.getInt("cooldown", 0));
            kit.permission(section.getString("permiso", ""));
            kit.maxRounds(section.getInt("rondas", 0));
            kit.allowHunger(section.getBoolean("hambre", true));
            kit.keepInventory(section.getBoolean("mantener-inventario", false));
            kit.naturalRegen(section.getBoolean("regeneracion", true));
            kit.fallDamage(section.getBoolean("dano-caida", true));
            kit.blockBreaking(section.getBoolean("romper-bloques", false));
            kit.blockPlacing(section.getBoolean("colocar-bloques", false));
            kit.showHealthBar(section.getBoolean("barra-vida", true));
            kit.inventory(section.getList("inventario") == null ? new ItemStack[36]
                    : section.getList("inventario").toArray(new ItemStack[0]));
            kit.armor(section.getList("armadura") == null ? new ItemStack[4]
                    : section.getList("armadura").toArray(new ItemStack[0]));
            kit.offHand(section.getItemStack("mano-secundaria"));
            if (section.isConfigurationSection("efectos")) {
                for (String effect : section.getConfigurationSection("efectos").getKeys(false)) {
                    kit.effects().put(effect.toUpperCase(Locale.ROOT), section.getInt("efectos." + effect, 1));
                }
            }
            this.kits.put(kit.name(), kit);
        }
        this.plugin.getLogger().info("Cargados " + this.kits.size() + " kits.");
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Kit kit : this.kits.values()) {
            String path = "kits." + kit.name() + ".";
            config.set(path + "nombre", kit.displayName());
            config.set(path + "descripcion", kit.description());
            config.set(path + "icono", kit.icon().name());
            config.set(path + "tipo", kit.type().name());
            config.set(path + "cooldown", kit.cooldown());
            config.set(path + "permiso", kit.permission());
            config.set(path + "rondas", kit.maxRounds());
            config.set(path + "hambre", kit.allowHunger());
            config.set(path + "mantener-inventario", kit.keepInventory());
            config.set(path + "regeneracion", kit.naturalRegen());
            config.set(path + "dano-caida", kit.fallDamage());
            config.set(path + "romper-bloques", kit.blockBreaking());
            config.set(path + "colocar-bloques", kit.blockPlacing());
            config.set(path + "barra-vida", kit.showHealthBar());
            config.set(path + "inventario", java.util.Arrays.asList(kit.inventory()));
            config.set(path + "armadura", java.util.Arrays.asList(kit.armor()));
            config.set(path + "mano-secundaria", kit.offHand());
            for (Map.Entry<String, Integer> entry : kit.effects().entrySet()) {
                config.set(path + "efectos." + entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }
        try {
            File parent = this.file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(this.file);
        } catch (IOException exception) {
            this.plugin.getLogger().warning("No se pudo guardar kits.yml: " + exception.getMessage());
        }
    }

    public Kit get(String name) {
        return name == null ? null : this.kits.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String name) {
        return get(name) != null;
    }

    public void register(Kit kit) {
        this.kits.put(kit.name(), kit);
    }

    public void delete(String name) {
        this.kits.remove(name.toLowerCase(Locale.ROOT));
    }

    public Collection<Kit> all() {
        return this.kits.values();
    }

    public List<String> names() {
        return new ArrayList<>(this.kits.keySet());
    }

    public List<Kit> byType(MatchType type) {
        List<Kit> list = new ArrayList<>();
        for (Kit kit : this.kits.values()) {
            if (kit.type() == type) {
                list.add(kit);
            }
        }
        return list;
    }

    /** Crea un kit desde el inventario del jugador (editor). */
    public Kit createFrom(Player player, String name) {
        Kit kit = new Kit(name);
        kit.displayName("<#FFD1DC>" + name);
        kit.icon(player.getInventory().getItemInMainHand() != null
                && player.getInventory().getItemInMainHand().getType() != Material.AIR
                ? player.getInventory().getItemInMainHand().getType() : Material.DIAMOND_SWORD);
        kit.captureFrom(player);
        this.kits.put(kit.name(), kit);
        return kit;
    }

    // ------------------------------------------------------------------
    //  Kits por defecto (pastel + equilibrados)
    // ------------------------------------------------------------------
    private void registerDefaults() {
        // --- NoDebuff ---
        Kit nodebuff = kit("nodebuff", "NoDebuff", "Pociones de curacion y espada rapida", Material.SPLASH_POTION, MatchType.SOLO);
        armor(nodebuff, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        nodebuff.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3);
        nodebuff.inventory()[1] = potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING, 34);
        nodebuff.inventory()[2] = new ItemStack(Material.GOLDEN_CARROT, 64);
        nodebuff.inventory()[3] = new ItemStack(Material.ENDER_PEARL, 16);
        effects(nodebuff, "SPEED:1", "FIRE_RESISTANCE:1");
        nodebuff.description("Diamond + pociones de curacion II");

        // --- Debuff ---
        Kit debuff = kit("debuff", "Debuff", "Pociones de dano y veneno", Material.LINGERING_POTION, MatchType.SOLO);
        armor(debuff, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        debuff.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2);
        debuff.inventory()[1] = potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING, 24);
        debuff.inventory()[2] = potion(Material.SPLASH_POTION, PotionType.STRONG_POISON, 8);
        debuff.inventory()[3] = potion(Material.SPLASH_POTION, PotionType.STRONG_SLOWNESS, 8);
        debuff.inventory()[4] = new ItemStack(Material.GOLDEN_CARROT, 64);
        effects(debuff, "SPEED:1");
        debuff.description("Pociones de veneno y lentitud");

        // --- Gapple ---
        Kit gapple = kit("gapple", "Gapple", "Manzanas doradas y buena armadura", Material.GOLDEN_APPLE, MatchType.SOLO);
        armor(gapple, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        gapple.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2);
        gapple.inventory()[1] = new ItemStack(Material.GOLDEN_APPLE, 16);
        gapple.inventory()[2] = new ItemStack(Material.COOKED_BEEF, 64);
        gapple.inventory()[3] = new ItemStack(Material.ENDER_PEARL, 16);
        effects(gapple, "SPEED:1", "FIRE_RESISTANCE:1", "REGENERATION:1");
        gapple.description("Regeneracion y manzanas doradas");

        // --- Combo ---
        Kit combo = kit("combo", "Combo", "Espada con knockback y combos largos", Material.DIAMOND_SWORD, MatchType.SOLO);
        armor(combo, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        combo.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 1);
        combo.inventory()[0].addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        combo.inventory()[1] = new ItemStack(Material.GOLDEN_APPLE, 32);
        combo.inventory()[2] = new ItemStack(Material.COOKED_BEEF, 64);
        effects(combo, "SPEED:2");
        combo.description("Knockback I + velocidad II");

        // --- Sumo ---
        Kit sumo = kit("sumo", "Sumo", "Sin armadura, empuja a tu rival fuera", Material.SLIME_BALL, MatchType.SOLO);
        armor(sumo, null, null, null, null);
        sumo.inventory()[0] = sword(Material.STICK, Enchantment.KNOCKBACK, 2);
        effects(sumo, "SPEED:2");
        sumo.description("Solo knockback: saca a tu rival");

        // --- Boxing ---
        Kit boxing = kit("boxing", "Boxing", "Combate a puños con armadura ligera", Material.LEATHER_CHESTPLATE, MatchType.SOLO);
        armor(boxing, Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET);
        boxing.inventory()[0] = sword(Material.SLIME_BALL, Enchantment.KNOCKBACK, 1);
        boxing.inventory()[1] = new ItemStack(Material.COOKED_BEEF, 64);
        effects(boxing, "SPEED:2");
        boxing.description("Duelo de golpes con knockback");

        // --- Archer ---
        Kit archer = kit("archer", "Archer", "Arco potenciado y movilidad", Material.BOW, MatchType.SOLO);
        armor(archer, Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET);
        archer.inventory()[0] = sword(Material.WOODEN_SWORD, Enchantment.SHARPNESS, 1);
        archer.inventory()[1] = bow(Enchantment.POWER, 3);
        archer.inventory()[2] = new ItemStack(Material.ARROW, 64);
        archer.inventory()[3] = new ItemStack(Material.GOLDEN_APPLE, 8);
        effects(archer, "SPEED:1");
        archer.description("Arco Power III y flechas");

        // --- BuildUHC ---
        Kit buhc = kit("builduhc", "BuildUHC", "Construye, bloquea y pelea", Material.LAVA_BUCKET, MatchType.SOLO);
        armor(buhc, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        buhc.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3);
        buhc.inventory()[1] = bow(Enchantment.POWER, 1);
        buhc.inventory()[2] = new ItemStack(Material.ARROW, 16);
        buhc.inventory()[3] = new ItemStack(Material.GOLDEN_APPLE, 12);
        buhc.inventory()[4] = new ItemStack(Material.OAK_PLANKS, 64);
        buhc.inventory()[5] = new ItemStack(Material.LAVA_BUCKET, 1);
        buhc.inventory()[6] = new ItemStack(Material.WATER_BUCKET, 1);
        buhc.inventory()[7] = new ItemStack(Material.COBBLESTONE, 64);
        buhc.blockPlacing(true);
        buhc.blockBreaking(true);
        buhc.description("PvP con construccion y cubos");

        // --- Soup ---
        Kit soup = kit("soup", "Soup", "Curacion por sopa de champiñones", Material.MUSHROOM_STEW, MatchType.SOLO);
        armor(soup, Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET);
        soup.inventory()[0] = sword(Material.IRON_SWORD, Enchantment.SHARPNESS, 1);
        for (int slot = 1; slot <= 8; slot++) {
            soup.inventory()[slot] = new ItemStack(Material.MUSHROOM_STEW, 1);
        }
        effects(soup, "SPEED:1");
        soup.description("Sopas que curan 3 corazones");

        // --- Iron ---
        Kit iron = kit("iron", "Iron", "Armadura y espada de hierro", Material.IRON_CHESTPLATE, MatchType.SOLO);
        armor(iron, Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET);
        iron.inventory()[0] = sword(Material.IRON_SWORD, Enchantment.SHARPNESS, 2);
        iron.inventory()[1] = new ItemStack(Material.COOKED_BEEF, 64);
        effects(iron, "SPEED:1");
        iron.description("Kit equilibrado de hierro");

        // --- Diamond ---
        Kit diamond = kit("diamond", "Diamond", "Kit clasico de diamante", Material.DIAMOND_CHESTPLATE, MatchType.SOLO);
        armor(diamond, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        diamond.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2);
        diamond.inventory()[1] = new ItemStack(Material.COOKED_BEEF, 64);
        diamond.inventory()[2] = new ItemStack(Material.GOLDEN_APPLE, 8);
        effects(diamond, "SPEED:1", "FIRE_RESISTANCE:1");
        diamond.description("Diamante full con manzanas");

        // --- SkyWars ---
        Kit skywars = kit("skywars", "SkyWars", "Islas, bloques y arco", Material.GRASS_BLOCK, MatchType.SOLO);
        armor(skywars, Material.IRON_BOOTS, Material.IRON_LEGGINGS, Material.IRON_CHESTPLATE, Material.IRON_HELMET);
        skywars.inventory()[0] = sword(Material.IRON_SWORD, Enchantment.SHARPNESS, 1);
        skywars.inventory()[1] = bow(Enchantment.POWER, 1);
        skywars.inventory()[2] = new ItemStack(Material.ARROW, 24);
        skywars.inventory()[3] = new ItemStack(Material.OAK_PLANKS, 64);
        skywars.inventory()[4] = new ItemStack(Material.GOLDEN_APPLE, 6);
        skywars.blockPlacing(true);
        skywars.description("Kit de islas con bloques");

        // --- HCF / Sopa de equipo ---
        Kit hcf = kit("hcf", "HCF", "PvP competitivo con pociones y manzanas", Material.DIAMOND_HELMET, MatchType.SOLO);
        armor(hcf, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        hcf.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2);
        hcf.inventory()[1] = new ItemStack(Material.GOLDEN_APPLE, 24);
        hcf.inventory()[2] = potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING, 8);
        hcf.inventory()[3] = new ItemStack(Material.ENDER_PEARL, 8);
        effects(hcf, "SPEED:1", "FIRE_RESISTANCE:1", "REGENERATION:1");
        hcf.description("Estilo HCF con pociones");

        // --- Duo / Equipos ---
        Kit duo = kit("duo_nodebuff", "Duo NoDebuff", "NoDebuff para 2v2", Material.SPLASH_POTION, MatchType.DUO);
        armor(duo, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        duo.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3);
        duo.inventory()[1] = potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING, 34);
        duo.inventory()[2] = new ItemStack(Material.GOLDEN_CARROT, 64);
        effects(duo, "SPEED:1", "FIRE_RESISTANCE:1");
        duo.description("NoDebuff pensado para 2v2");

        Kit teamSumo = kit("team_sumo", "Team Sumo", "Sumo para equipos", Material.SLIME_BALL, MatchType.TEAM);
        armor(teamSumo, null, null, null, null);
        teamSumo.inventory()[0] = sword(Material.STICK, Enchantment.KNOCKBACK, 2);
        effects(teamSumo, "SPEED:2");
        teamSumo.description("Sumo con tu party");

        Kit teamNodebuff = kit("team_nodebuff", "Team NoDebuff", "NoDebuff para equipos grandes", Material.SPLASH_POTION, MatchType.TEAM);
        armor(teamNodebuff, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        teamNodebuff.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3);
        teamNodebuff.inventory()[1] = potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING, 34);
        teamNodebuff.inventory()[2] = new ItemStack(Material.GOLDEN_CARROT, 64);
        teamNodebuff.inventory()[3] = new ItemStack(Material.ENDER_PEARL, 16);
        effects(teamNodebuff, "SPEED:1", "FIRE_RESISTANCE:1");
        teamNodebuff.description("NoDebuff para equipos");

        // --- FFA ---
        Kit ffa = kit("ffa", "FFA", "Kit de combate libre", Material.DIAMOND_SWORD, MatchType.FFA);
        armor(ffa, Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET);
        ffa.inventory()[0] = sword(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 2);
        ffa.inventory()[1] = new ItemStack(Material.GOLDEN_APPLE, 16);
        ffa.inventory()[2] = potion(Material.SPLASH_POTION, PotionType.STRONG_HEALING, 8);
        ffa.inventory()[3] = new ItemStack(Material.COOKED_BEEF, 64);
        ffa.inventory()[4] = new ItemStack(Material.ENDER_PEARL, 8);
        effects(ffa, "SPEED:1");
        ffa.description("Kit para arenas FFA");
    }

    // ------------------- helpers -------------------
    private Kit kit(String name, String display, String description, Material icon, MatchType type) {
        Kit kit = new Kit(name);
        kit.displayName("<#FFD1DC>" + display);
        kit.description(description);
        kit.icon(icon);
        kit.type(type);
        this.kits.put(kit.name(), kit);
        return kit;
    }

    private void armor(Kit kit, Material boots, Material leggings, Material chestplate, Material helmet) {
        ItemStack[] armor = new ItemStack[4];
        if (boots != null) {
            armor[0] = enchanted(boots, Enchantment.PROTECTION, 2);
        }
        if (leggings != null) {
            armor[1] = enchanted(leggings, Enchantment.PROTECTION, 2);
        }
        if (chestplate != null) {
            armor[2] = enchanted(chestplate, Enchantment.PROTECTION, 2);
        }
        if (helmet != null) {
            armor[3] = enchanted(helmet, Enchantment.PROTECTION, 2);
        }
        kit.armor(armor);
    }

    private ItemStack enchanted(Material material, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack sword(Material material, Enchantment enchantment, int level) {
        return enchanted(material, enchantment, level);
    }

    private ItemStack bow(Enchantment enchantment, int level) {
        ItemStack item = enchanted(Material.BOW, enchantment, level);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack potion(Material material, PotionType type, int amount) {
        ItemStack item = new ItemStack(material, amount);
        if (item.getItemMeta() instanceof PotionMeta meta) {
            meta.setBasePotionType(type);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void effects(Kit kit, String... specs) {
        for (String spec : specs) {
            String[] parts = spec.split(":");
            kit.effects().put(parts[0].toUpperCase(Locale.ROOT), parts.length > 1 ? Integer.parseInt(parts[1]) : 1);
        }
    }
}
