package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.events.PlayerSacrificeItemEvent;
import com.skelril.aurora.exceptions.UnsupportedPrayerException;
import com.skelril.aurora.prayer.Prayer;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Sacrifice", desc = "Sacrifice! Sacrifice! Sacrifice!")
@Depend(plugins = "Pitfall", components = PrayerComponent.class)
public class SacrificeComponent extends BukkitComponent implements Listener, Runnable {

    private static final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private PrayerComponent prayer;

    private LocalConfiguration config;
    private Map<Integer, Integer> entityTaskId = new HashMap<>();

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("good-sacrifices")
        public Set<String> goodSacrifices = new HashSet<>(Arrays.asList(
                BlockID.DIAMOND_BLOCK + ":0" + "#81",
                BlockID.EMERALD_BLOCK + ":0" + "#64",
                //BlockID.GOLD_BLOCK + ":0" + "#64",
                ItemID.GOLD_APPLE + ":1" + "#64",
                ItemID.GOLD_APPLE + ":0" + "#8",
                ItemID.GOLD_AXE + ":0" + "#24",
                //ItemID.GOLD_BAR + ":0" + "#8",
                ItemID.GOLD_BOOTS + ":0" + "#32",
                ItemID.GOLD_CHEST + ":0" + "#64",
                ItemID.GOLD_HELMET + ":0" + "#40",
                ItemID.GOLD_HOE + ":0" + "#16",
                ItemID.GOLD_PANTS + ":0" + "#56",
                ItemID.GOLD_PICKAXE + ":0" + "#24",
                ItemID.GOLD_SHOVEL + ":0" + "#8",
                ItemID.GOLD_SWORD + ":0" + "#16",
                ItemID.SPAWN_EGG + ":*" + "#3",
                BlockID.ENCHANTMENT_TABLE + ":0" + "#100",
                BlockID.ENDER_CHEST + ":0" + "#100",
                ItemID.CHAINMAIL_BOOTS + ":0" + "#32",
                ItemID.CHAINMAIL_PANTS + ":0" + "#32",
                ItemID.CHAINMAIL_CHEST + ":0" + "#32",
                ItemID.CHAINMAIL_HELMET + ":0" + "#32",
                ItemID.DIAMOND + ":0" + "#9",
                ItemID.DIAMOND_AXE + ":0" + "#27",
                ItemID.DIAMOND_BOOTS + ":0" + "#36",
                ItemID.DIAMOND_PANTS + ":0" + "#63",
                ItemID.DIAMOND_CHEST + ":0" + "#72",
                ItemID.DIAMOND_HELMET + ":0" + "#45",
                ItemID.DIAMOND_HOE + ":0" + "#18",
                ItemID.DIAMOND_PICKAXE + ":0" + "#27",
                ItemID.DIAMOND_SHOVEL + ":0" + "#9",
                ItemID.DIAMOND_SWORD + ":0" + "#18",
                ItemID.DISC_11 + ":0" + "#12000",
                ItemID.DISC_13 + ":0" + "#12000",
                ItemID.DISC_BLOCKS + ":0" + "#12000",
                ItemID.DISC_CAT + ":0" + "#12000",
                ItemID.DISC_CHIRP + ":0" + "#12000",
                ItemID.DISC_FAR + ":0" + "#12000",
                ItemID.DISC_MALL + ":0" + "#12000",
                ItemID.DISC_MELLOHI + ":0" + "#12000",
                ItemID.DISC_STAL + ":0" + "#12000",
                ItemID.DISC_STRAD + ":0" + "#12000",
                ItemID.DISC_WARD + ":0" + "#12000",
                ItemID.BLAZE_POWDER + ":0" + "#3",
                ItemID.BLAZE_ROD + ":0" + "#18",
                ItemID.GHAST_TEAR + ":0" + "#18",
                ItemID.BOTTLE_O_ENCHANTING + ":0" + "#3",
                ItemID.FERMENTED_SPIDER_EYE + ":0" + "#4",
                ItemID.GLISTERING_MELON + ":0" + "#4",
                ItemID.ENDER_PEARL + ":0" + "#3",
                ItemID.EYE_OF_ENDER + ":0" + "#32",
                ItemID.LIGHTSTONE_DUST + ":0" + "#4",
                ItemID.MAGMA_CREAM + ":0" + "#32",
                ItemID.POTION + ":*" + "#2",
                ItemID.PAINTING + ":0" + "#5",
                ItemID.EMERALD + ":0" + "#3",
                BlockID.LIGHTSTONE + ":0" + "#7",
                BlockID.TNT + ":0" + "#5",
                BlockID.END_STONE + ":0" + "#2",
                ItemID.NETHER_STAR + ":0" + "#81"));
        @Setting("sacrificial-block")
        private String sacrificialBlockString = BlockID.STONE_BRICK + ":3";

        public int sacrificialBlockId = getSacrificialBlockId();

        public byte sacrificialBlockData = getSacrificialBlockData();

        private int getSacrificialBlockId() {

            try {
                String[] blockData = sacrificialBlockString.trim().split(":");
                return Integer.parseInt(blockData[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return 98;
            }
        }

        private byte getSacrificialBlockData() {

            try {
                String[] blockData = sacrificialBlockString.trim().split(":");
                return (byte) Integer.parseInt(blockData[1]);
            } catch (Exception e) {
                e.printStackTrace();
                return 3;
            }
        }
    }

    private void addEntityTaskId(Entity entity, int taskId) {

        entityTaskId.put(entity.getEntityId(), taskId);
    }

    private void removeEntity(Entity entity) {

        if (entityTaskId.containsKey(entity.getEntityId())) {
            entityTaskId.remove(entity.getEntityId());
        }

    }

    private int getEntityTaskId(Entity entity) {

        int getEntityTaskId = 0;
        if (entityTaskId.containsKey(entity.getEntityId())) {
            getEntityTaskId = entityTaskId.get(entity.getEntityId());
        }
        return getEntityTaskId;
    }

    private int calculateValue(ItemStack itemStack) {

        if (itemStack == null || itemStack.getTypeId() == 0) return -1;

        int itemStackId = itemStack.getTypeId();
        int itemStackData = itemStack.getData().getData();
        int itemStackSize = itemStack.getAmount();

        for (String sacrifice : config.goodSacrifices) {
            String[] itemValue = sacrifice.split("#");
            String[] itemData = itemValue[0].split(":");

            int id;
            int data;
            int value;

            try {
                id = Integer.parseInt(itemData[0]);
                data = Integer.parseInt(itemData[1]);
                value = Integer.parseInt(itemValue[1]);

                if (itemStackId == id && itemStackData == data) return (value * itemStackSize) + 64;
            } catch (Exception a) {

                try {

                    id = Integer.parseInt(itemData[0]);
                    value = Integer.parseInt(itemValue[1]);

                    if (itemStackId == id) return (value * itemStackSize) + 64;

                } catch (Exception b) {

                    log.warning("Incorrect configuration for the component: "
                            + this.getInformation().friendlyName() + ".");
                }
            }
        }

        return itemStackSize;
    }

    private static int calculateModifier(int value) {

        return (value * 12) / 335;
    }

    /**
     * This method is used to get the ItemStacks to drop based on the ItemStack sacrificed.
     *
     * @param sender - The triggering sender
     * @param items  - One or more items that were sacrificed
     * @return - The ItemStacks that should be received
     */
    public List<ItemStack> getCalculatedLoot(CommandSender sender, ItemStack... items) {

        // Calculate the amt & value
        int amt = 0;
        int value = 0;
        for (ItemStack item : items) {
            amt += item.getAmount();
            value += calculateValue(item);
        }

        return getCalculatedLoot(sender, amt, value);
    }

    public static List<ItemStack> getCalculatedLoot(CommandSender sender, int amt, int value) {

        List<ItemStack> loot = new ArrayList<>();

        // Calculate the modifier
        int modifier = calculateModifier(value);

        if (value > 64) {
            for (int i = 0; i < amt; i++) {

                ItemStack itemStack = null;
                ItemMeta itemMeta = null;

                if (inst.hasPermission(sender, "aurora.sacrifice.efficiency")) {
                    if (!ChanceUtil.getChance(Math.max(1, 100 - modifier))) continue;
                } else {
                    if (!ChanceUtil.getChance(Math.max(1, 125 - modifier))) continue;
                }
                switch (ChanceUtil.getRandom(25)) {
                    case 1:
                        itemStack = new ItemStack(ItemID.DIAMOND_SWORD);
                        if (Util.getChance(sender, modifier, 1.2)) {
                            itemStack.addEnchantment(Enchantment.DAMAGE_ALL, 5);
                            itemStack.addEnchantment(Enchantment.DAMAGE_ARTHROPODS, 5);
                            itemStack.addEnchantment(Enchantment.DAMAGE_UNDEAD, 5);
                            itemStack.addEnchantment(Enchantment.FIRE_ASPECT, 2);
                            itemStack.addEnchantment(Enchantment.KNOCKBACK, 2);
                            itemStack.addEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.RED + "God Sword");
                        }
                        break;
                    case 2:
                        itemStack = new ItemStack(ItemID.BOW);
                        if (Util.getChance(sender, modifier, 1.2)) {
                            itemStack.addEnchantment(Enchantment.ARROW_DAMAGE, 5);
                            itemStack.addEnchantment(Enchantment.ARROW_FIRE, 1);
                            itemStack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
                            itemStack.addEnchantment(Enchantment.ARROW_KNOCKBACK, 2);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.RED + "God Bow");
                        } else {
                            itemStack.addEnchantment(Enchantment.ARROW_DAMAGE, 2);
                            itemStack.addEnchantment(Enchantment.ARROW_FIRE, 1);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.RED + "Overseer's Bow");
                        }
                        break;
                    case 3:
                        itemStack = new ItemStack(ItemID.DIAMOND_PICKAXE);
                        if (Util.getChance(sender, modifier, 2)) {
                            itemStack.addEnchantment(Enchantment.DIG_SPEED, 5);
                            itemStack.addEnchantment(Enchantment.DURABILITY, 3);
                            itemStack.addEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 3);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GREEN + "Legendary God Pickaxe");
                        } else if (Util.getChance(sender, modifier, .37)) {
                            itemStack.addEnchantment(Enchantment.DIG_SPEED, 4);
                            itemStack.addEnchantment(Enchantment.SILK_TOUCH, 1);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GREEN + "God Pickaxe");
                        }
                        break;
                    case 4:
                        itemStack = new ItemStack(ItemID.GOLD_PICKAXE);
                        itemMeta = itemStack.getItemMeta();
                        List<String> lore = new ArrayList<>();
                        lore.add("A very fast golden pickaxe.");
                        itemMeta.setLore(lore);
                        itemMeta.addEnchant(Enchantment.DURABILITY, 4, true);
                        itemMeta.addEnchant(Enchantment.DIG_SPEED, 4, true);
                        break;
                    case 5:
                        itemStack = new ItemStack(ItemID.BOTTLE_O_ENCHANTING, ChanceUtil.getRangedRandom(40, 64));
                        break;
                    case 6:
                        itemStack = new ItemStack(BlockID.ENDER_CHEST);
                        break;
                    case 7:
                        itemStack = new ItemStack(ItemID.DIAMOND_CHEST);
                        if (Util.getChance(sender, modifier, .8)) {
                            itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.BLUE + "God Chestplate");
                        }
                        break;
                    case 8:
                        itemStack = new ItemStack(ItemID.DIAMOND_PANTS);
                        if (Util.getChance(sender, modifier, .8)) {
                            itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.BLUE + "God Leggings");
                        }
                        break;
                    case 9:
                        itemStack = new ItemStack(ItemID.PAINTING, ChanceUtil.getRangedRandom(50, 64));
                        break;
                    case 10:
                        itemStack = new ItemStack(BlockID.SPONGE, ChanceUtil.getRandom(64));
                        break;
                    case 11:
                        itemStack = new ItemStack(ItemID.BOOK, ChanceUtil.getRangedRandom(50, 64));
                        break;
                    case 12:
                        itemStack = new ItemStack(ItemID.BLAZE_ROD, ChanceUtil.getRangedRandom(20, 32));
                        break;
                    case 13:
                        itemStack = new ItemStack(ItemID.GLISTERING_MELON, ChanceUtil.getRangedRandom(20, 32));
                        break;
                    case 14:
                        itemStack = new ItemStack(ItemID.SLIME_BALL, ChanceUtil.getRangedRandom(20, 32));
                        break;
                    case 15:
                        itemStack = new ItemStack(ItemID.FERMENTED_SPIDER_EYE, ChanceUtil.getRangedRandom(20, 32));
                        break;
                    case 16:
                        if (Util.getChance(sender, modifier, 2.75)) {
                            itemStack = new ItemStack(ItemID.CHAINMAIL_BOOTS);
                            itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FALL, 4);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GOLD + "Ancient Boots");
                        }
                        break;
                    case 17:
                        if (Util.getChance(sender, modifier, 2.75)) {
                            itemStack = new ItemStack(ItemID.CHAINMAIL_PANTS);
                            itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GOLD + "Ancient Leggings");
                        }
                        break;
                    case 18:
                        if (Util.getChance(sender, modifier, 2.75)) {
                            itemStack = new ItemStack(ItemID.CHAINMAIL_CHEST);
                            itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GOLD + "Ancient Chestplate");
                        }
                        break;
                    case 19:
                        if (Util.getChance(sender, modifier, 2.75)) {
                            itemStack = new ItemStack(ItemID.CHAINMAIL_HELMET);
                            itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                            itemStack.addEnchantment(Enchantment.OXYGEN, 3);
                            itemStack.addEnchantment(Enchantment.WATER_WORKER, 1);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GOLD + "Ancient Helmet");
                        }
                        break;
                    case 20:
                        itemStack = new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRandom(9));
                        break;
                    case 21:
                        itemStack = new ItemStack(ItemID.DIAMOND_HELMET);
                        if (Util.getChance(sender, modifier, .8)) {
                            itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                            itemStack.addEnchantment(Enchantment.OXYGEN, 3);
                            itemStack.addEnchantment(Enchantment.WATER_WORKER, 1);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.BLUE + "God Helmet");
                        }
                        break;
                    case 22:
                        itemStack = new ItemStack(ItemID.DIAMOND_BOOTS);
                        if (Util.getChance(sender, modifier, .8)) {
                            itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                            itemStack.addEnchantment(Enchantment.PROTECTION_FALL, 4);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.BLUE + "God Boots");
                        }
                        break;
                    case 23:
                        itemStack = new Potion(PotionType.INSTANT_DAMAGE).toItemStack(1);
                        PotionMeta pMeta = (PotionMeta) itemStack.getItemMeta();

                        if (Util.getChance(sender, modifier, 5)) {
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 5), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 5), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 5), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 5), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 5), false);
                            pMeta.setDisplayName(ChatColor.WHITE + "Divine Combat Potion");
                        } else if (Util.getChance(sender, modifier, 2)) {
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 45, 5), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.REGENERATION, 20 * 45, 5), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 45, 5), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 45, 5), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 45, 5), false);
                            pMeta.setDisplayName(ChatColor.WHITE + "Holy Combat Potion");
                        } else {
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 2), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 2), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 2), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 2), false);
                            pMeta.addCustomEffect(
                                    new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2), false);
                            pMeta.setDisplayName(ChatColor.WHITE + "Extreme Combat Potion");
                        }
                        itemMeta = pMeta;
                        break;
                    case 24:
                        itemStack = new ItemStack(ItemID.DIAMOND, ChanceUtil.getRandom(3));
                        break;
                    case 25:
                        itemStack = new ItemStack(ItemID.EMERALD, ChanceUtil.getRandom(3));
                        break;
                }

                if (itemMeta != null) {
                    if (itemMeta instanceof Repairable && itemMeta.hasEnchants()) {
                        ((Repairable) itemMeta).setRepairCost(400);
                    }
                    itemStack.setItemMeta(itemMeta);
                }
                if (itemStack != null) loot.add(itemStack);
            }
        }

        if (loot.size() < amt / 2 || loot.size() == 0) {
            for (int i = 0; i < (amt - loot.size()); i++) {

                ItemStack itemStack = null;

                if (!ChanceUtil.getChance(2, 3)) continue;
                switch (ChanceUtil.getRandom(21)) {
                    case 1:
                        itemStack = new ItemStack(BlockID.DIRT);
                        break;
                    case 2:
                        itemStack = new ItemStack(BlockID.STONE);
                        break;
                    case 3:
                        itemStack = new ItemStack(BlockID.RED_FLOWER);
                        break;
                    case 4:
                        itemStack = new ItemStack(BlockID.YELLOW_FLOWER);
                        break;
                    case 5:
                        itemStack = new ItemStack(ItemID.INK_SACK, 1, (short) (ChanceUtil.getRandom(16) - 1));
                        break;
                    case 6:
                        itemStack = new ItemStack(ItemID.SEEDS);
                        break;
                    case 7:
                        itemStack = new ItemStack(ItemID.WHEAT);
                        break;
                    case 8:
                        itemStack = new ItemStack(BlockID.WOOD);
                        break;
                    case 9:
                        itemStack = new ItemStack(ItemID.FEATHER);
                        break;
                    case 10:
                        if (Util.getChance(sender, modifier, .2)) {
                            itemStack = new ItemStack(ItemID.GOLD_NUGGET, ChanceUtil.getRandom(64));
                        }
                        break;
                    case 11:
                        itemStack = new ItemStack(ItemID.ARROW);
                        break;
                    case 12:
                        itemStack = new ItemStack(ItemID.BOWL);
                        break;
                    case 13:
                        itemStack = new ItemStack(ItemID.BONE);
                        break;
                    case 14:
                        itemStack = new ItemStack(ItemID.SNOWBALL);
                        break;
                    case 15:
                        itemStack = new ItemStack(ItemID.FLINT);
                        break;
                    case 16:
                        if (Util.getChance(sender, modifier, .8)) {
                            itemStack = new ItemStack(ItemID.CLAY_BALL, ChanceUtil.getRandom(8));
                        }
                        break;
                    case 17:
                        if (Util.getChance(sender, modifier, .9)) {
                            itemStack = new ItemStack(ItemID.BOW);
                            itemStack.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        }
                        break;
                    case 18:
                        itemStack = new ItemStack(ItemID.RED_APPLE, ChanceUtil.getRandom(6));
                        break;
                    case 19:
                        if (Util.getChance(sender, modifier, .8)) {
                            itemStack = new ItemStack(ItemID.GOLD_APPLE, ChanceUtil.getRandom(8));
                        }
                        break;
                    case 20:
                        itemStack = new ItemStack(ItemID.ENDER_PEARL, ChanceUtil.getRandom(6));
                        break;
                    case 21:
                        itemStack = new ItemStack(ItemID.COOKIE, ChanceUtil.getRangedRandom(8, 16));
                        break;
                }

                if (itemStack != null) loot.add(itemStack);
            }
        }
        return loot;
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {

        if (entityTaskId.containsKey(event.getEntity().getEntityId())) event.setCancelled(true);
    }

    private static Set<BlockFace> surrounding = new HashSet<>();

    static {
        surrounding.add(BlockFace.SELF);
        surrounding.add(BlockFace.NORTH);
        surrounding.add(BlockFace.EAST);
        surrounding.add(BlockFace.SOUTH);
        surrounding.add(BlockFace.WEST);
    }

    public void createFire(Block origin) {

        if (origin.getTypeId() != config.getSacrificialBlockId()
                || origin.getData() != config.getSacrificialBlockData()) return;

        final Block above = origin.getRelative(BlockFace.UP);
        if (above.getTypeId() == BlockID.AIR) {
            above.setTypeId(BlockID.FIRE);
            server.getScheduler().runTaskLater(inst, new Runnable() {

                @Override
                public void run() {

                    above.setTypeId(BlockID.AIR);
                }
            }, 20 * 4);

            for (BlockFace face : surrounding) {
                createFire(origin.getRelative(face));
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        final Player player = event.getPlayer();
        final Item item = event.getItemDrop();
        if (!inst.hasPermission(player, "aurora.sacrifice")) return;

        int taskId = server.getScheduler().scheduleSyncRepeatingTask(inst, new Runnable() {

            @Override
            public void run() {

                int id = getEntityTaskId(item);

                if (item.isDead() || item.getTicksLived() > 40) {

                    if (id != -1) {
                        removeEntity(item);
                        server.getScheduler().cancelTask(id);
                    }
                } else {
                    final Block searchBlock = item.getLocation().getBlock().getRelative(BlockFace.DOWN);

                    int blockTypeId = searchBlock.getTypeId();
                    int blockData = searchBlock.getData();
                    if ((blockTypeId == config.sacrificialBlockId) && (blockData == config.sacrificialBlockData)) {
                        try {
                            // Create the event here
                            PlayerSacrificeItemEvent sacrificeItemEvent =
                                    new PlayerSacrificeItemEvent(player, searchBlock, item.getItemStack());
                            server.getPluginManager().callEvent(sacrificeItemEvent);
                            if (sacrificeItemEvent.isCancelled()) return;
                            createFire(searchBlock);
                            sacrifice(player, sacrificeItemEvent.getItemStack());
                            removeEntity(item);
                            item.remove();
                            server.getScheduler().cancelTask(id);
                            player.sendMessage(ChatColor.GOLD + "An ancient fire ignites.");
                        } catch (Exception e) {
                            log.warning("The: "
                                    + SacrificeComponent.this.getInformation().friendlyName()
                                    + " component could not contact Pitfall.");
                        }
                    }
                }
            }

        }, 0, 1); // Start at 0 ticks and repeat every 1 ticks
        addEntityTaskId(item, taskId);
    }

    @Override
    public void run() {

        for (Player player : server.getOnlinePlayers()) {
            Location playerLoc = player.getLocation();
            Block searchBlock = playerLoc.getBlock().getRelative(BlockFace.DOWN, 3);
            if (searchBlock.getTypeId() == config.sacrificialBlockId
                    && searchBlock.getData() == config.sacrificialBlockData) {
                Location airLoc = LocationUtil.findRandomLoc(searchBlock, 3);

                try {
                    Location newLoc = new Location(player.getWorld(), airLoc.getX(), airLoc.getY(),
                            airLoc.getZ(), playerLoc.getYaw(), playerLoc.getPitch());

                    Location[] smokeLocation = new Location[4];
                    smokeLocation[0] = player.getLocation();
                    smokeLocation[1] = player.getEyeLocation();
                    smokeLocation[2] = newLoc;
                    smokeLocation[3] = newLoc.getBlock().getRelative(BlockFace.UP).getLocation();

                    Entity vehicle = player.getVehicle();
                    if (vehicle != null) {
                        vehicle.eject();
                    }
                    player.teleport(newLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    if (vehicle != null) {
                        vehicle.teleport(player, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        vehicle.setPassenger(player);
                    }
                    EnvironmentUtil.generateRadialEffect(smokeLocation, Effect.SMOKE);
                } catch (Exception e) {
                    log.warning("Could not find a location to teleport the player: " + player.getName() + " to.");
                }
            }
        }
    }

    private void sacrifice(Player player, ItemStack item) {

        if (item.getTypeId() == 0) return;

        PlayerInventory pInventory = player.getInventory();

        if (ItemUtil.isMasterSword(item)) {
            pInventory.addItem(ItemUtil.Master.makeSword());
            return;
        } else if (ItemUtil.isMasterBow(item)) {
            pInventory.addItem(ItemUtil.Master.makeBow());
            return;
        }

        for (ItemStack aItemStack : getCalculatedLoot(player, item)) {
            pInventory.addItem(aItemStack);
        }

        if (ChanceUtil.getChance(5) && calculateValue(item) >= 200) {

            PrayerType prayerType;
            switch (ChanceUtil.getRandom(7)) {
                case 1:
                    prayerType = PrayerType.DIGGYDIGGY;
                    break;
                case 2:
                    prayerType = PrayerType.HEALTH;
                    break;
                case 3:
                    prayerType = PrayerType.POWER;
                    break;
                case 4:
                    prayerType = PrayerType.SPEED;
                    break;
                case 5:
                    prayerType = PrayerType.ANTIFIRE;
                    break;
                case 6:
                    prayerType = PrayerType.NIGHTVISION;
                    break;
                case 7:
                    prayerType = PrayerType.DEADLYDEFENSE;
                    break;
                default:
                    prayerType = PrayerType.SMOKE;
            }
            try {
                Prayer givenPrayer = prayer.constructPrayer(player, prayerType, TimeUnit.MINUTES.toMillis(60));
                if (prayer.influencePlayer(player, givenPrayer)) {
                    ChatUtil.sendNotice(player, "You feel as though you have been blessed with "
                            + prayerType.toString().toLowerCase() + ".");
                }
            } catch (UnsupportedPrayerException e) {
                e.printStackTrace();
            }
        }
    }

    public class Commands {

        @Command(aliases = {"sacrifice"}, desc = "Permissions Commands")
        @NestedCommand({SacrificeCommands.class})
        public void sacrificeCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class SacrificeCommands {

        @Command(aliases = {"value"}, desc = "Value an item", flags = "", min = 0, max = 0)
        public void userGroupSetCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            int value = calculateValue(((Player) sender).getInventory().getItemInHand());
            if (value == -1) throw new CommandException("You can't sacrifice that!");
            ChatUtil.sendNotice(sender, "That item has a value of: " + value + " in the sacrificial pit.");
        }
    }

    private static class Util {

        public static boolean getChance(CommandSender sender, int modifier, double rarityL) {

            boolean hasEfficiency = inst.hasPermission(sender, "aurora.sacrifice.efficiency");
            int baseChance = (int) (hasEfficiency ? rarityL * 100 : rarityL * 200);

            return ChanceUtil.getChance(Math.max(1, baseChance - modifier));
        }
    }
}