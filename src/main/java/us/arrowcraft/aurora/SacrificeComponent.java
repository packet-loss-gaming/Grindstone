package us.arrowcraft.aurora;
import com.petrifiednightmares.pitfall.Pitfall;
import com.petrifiednightmares.pitfall.PitfallEvent;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.arrowcraft.aurora.exceptions.UnsupportedPrayerException;
import us.arrowcraft.aurora.prayer.Prayer;
import us.arrowcraft.aurora.prayer.PrayerComponent;
import us.arrowcraft.aurora.prayer.PrayerType;
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.EnvironmentUtil;
import us.arrowcraft.aurora.util.LocationUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Sacrifice", desc = "Sacrifice! Sacrifice! Sacrifice!")
@Depend(plugins = "Pitfall", components = PrayerComponent.class)
public class SacrificeComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
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
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 20);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private Pitfall getPitfall() {

        Plugin plugin = server.getPluginManager().getPlugin("Pitfall");

        // Pitfall may not be loaded
        if (plugin == null || !(plugin instanceof Pitfall)) {
            return null; // Maybe you want throw an exception instead
        }

        return (Pitfall) plugin;
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("good-sacrifices")
        public Set<String> goodSacrifices = new HashSet<>(Arrays.asList(
                BlockID.DIAMOND_BLOCK + ":0" + "#81",
                BlockID.EMERALD_BLOCK + ":0" + "#64",
                BlockID.GOLD_BLOCK + ":0" + "#64",
                ItemID.GOLD_APPLE + ":1" + "#64",
                ItemID.GOLD_APPLE + ":0" + "#8",
                ItemID.GOLD_AXE + ":0" + "#24",
                ItemID.GOLD_BAR + ":0" + "#8",
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
                BlockID.END_STONE + ":0" + "#2"));
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

    private int calculateModifier(int value) {

        return (value * 12) / 335;
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {

        if (entityTaskId.containsKey(event.getEntity().getEntityId())) event.setCancelled(true);
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
                            PitfallEvent pitfallEvent = new PitfallEvent(searchBlock, player,
                                    config.sacrificialBlockId, config.sacrificialBlockData,   // Old Block B
                                    config.sacrificialBlockId, config.sacrificialBlockData,   // New Block B
                                    BlockID.AIR, (byte) 0,                                    // Old Block H
                                    BlockID.FIRE, (byte) 0,                                   // New Block H
                                    20 * 4, false);
                            getPitfall().getPitfallEngine().createPitfallEffect(pitfallEvent);
                            if (pitfallEvent.isCancelled()) return;
                            sacrifice(player, item.getItemStack().clone());
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

                    player.teleport(newLoc);
                    EnvironmentUtil.generateRadialEffect(smokeLocation, Effect.SMOKE);
                } catch (Exception e) {
                    log.warning("Could not find a location to teleport the player: " + player.getName() + " to.");
                }
            }
        }
    }

    /**
     * This method is used to get the ItemStacks to drop based on the ItemStack sacrificed.
     *
     * @param sender - The triggering sender
     * @param items  - One or more items that were sacrificed
     *
     * @return - The ItemStacks that should be received
     */
    public List<ItemStack> getCalculatedLoot(CommandSender sender, ItemStack... items) {

        List<ItemStack> loot = new ArrayList<>();

        // Calculate the amt & value
        int amt = 0;
        int value = 0;
        for (ItemStack item : items) {
            amt += item.getAmount();
            value += calculateValue(item);
        }

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
                switch (ChanceUtil.getRandom(23)) {
                    case 1:
                        itemStack = new ItemStack(ItemID.DIAMOND_SWORD);
                        itemStack.addEnchantment(Enchantment.DAMAGE_ALL, 5);
                        itemStack.addEnchantment(Enchantment.DAMAGE_ARTHROPODS, 5);
                        itemStack.addEnchantment(Enchantment.DAMAGE_UNDEAD, 5);
                        itemStack.addEnchantment(Enchantment.FIRE_ASPECT, 2);
                        itemStack.addEnchantment(Enchantment.KNOCKBACK, 2);
                        itemStack.addEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
                        itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.RED + "God Sword");
                        break;
                    case 2:
                        itemStack = new ItemStack(ItemID.BOW);
                        itemStack.addEnchantment(Enchantment.ARROW_DAMAGE, 5);
                        itemStack.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        itemStack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
                        itemStack.addEnchantment(Enchantment.ARROW_KNOCKBACK, 2);
                        itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.RED + "God Bow");
                        break;
                    case 3:
                        itemStack = new ItemStack(ItemID.DIAMOND_PICKAXE);
                        if (ChanceUtil.getChance(Math.max(1, 200 - modifier))
                                || ChanceUtil.getChance(Math.max(1, 100 - modifier))
                                && inst.hasPermission(sender, "aurora.sacrifice.efficiency")) {
                            itemStack.addEnchantment(Enchantment.DIG_SPEED, 5);
                            itemStack.addEnchantment(Enchantment.DURABILITY, 3);
                            itemStack.addEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 3);
                            itemMeta = itemStack.getItemMeta();
                            itemMeta.setDisplayName(ChatColor.GREEN + "Legendary God Pickaxe");
                        } else {
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
                        itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                        itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.BLUE + "God Chestplate");
                        break;
                    case 8:
                        itemStack = new ItemStack(ItemID.DIAMOND_PANTS);
                        itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                        itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.BLUE + "God Leggings");
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
                        if (ChanceUtil.getChance(Math.max(1, 100 - modifier))) {
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
                        if (ChanceUtil.getChance(Math.max(1, 100 - modifier))) {
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
                        if (ChanceUtil.getChance(Math.max(1, 100 - modifier))) {
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
                        if (ChanceUtil.getChance(Math.max(1, 100 - modifier))) {
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
                        itemStack = new ItemStack(ItemID.GOLD_APPLE, 1, (short) 1);
                        break;
                    case 21:
                        itemStack = new ItemStack(ItemID.DIAMOND_HELMET);
                        itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                        itemStack.addEnchantment(Enchantment.OXYGEN, 3);
                        itemStack.addEnchantment(Enchantment.WATER_WORKER, 1);
                        itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.BLUE + "God Helmet");
                        break;
                    case 22:
                        itemStack = new ItemStack(ItemID.DIAMOND_BOOTS);
                        itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                        itemStack.addEnchantment(Enchantment.PROTECTION_FALL, 4);
                        itemMeta = itemStack.getItemMeta();
                        itemMeta.setDisplayName(ChatColor.BLUE + "God Boots");
                        break;
                    case 23:
                        itemStack = new ItemStack(Material.POTION);
                        PotionMeta pMeta = (PotionMeta) itemStack.getItemMeta();

                        if (ChanceUtil.getChance(Math.max(1, 500 - modifier))) {
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

                            // Lore
                            List<String> divineLore = new ArrayList<>();
                            divineLore.add("You can almost smell the ultimate power");
                            divineLore.add("in this potion.");
                            pMeta.setLore(divineLore);
                        } else if (ChanceUtil.getChance(Math.max(1, 200 - modifier))) {
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

                            // Lore
                            List<String> holyLore = new ArrayList<>();
                            holyLore.add("A liquid so sweet it gives you the power");
                            holyLore.add("of the gods for a short time.");
                            pMeta.setLore(holyLore);
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

                            // Lore
                            List<String> extremeLore = new ArrayList<>();
                            extremeLore.add("A very powerful and devastating potion.");
                            pMeta.setLore(extremeLore);
                        }
                        itemMeta = pMeta;
                        break;
                }

                if (itemMeta != null) {
                    if (itemMeta instanceof Repairable) ((Repairable) itemMeta).setRepairCost(400);
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
                        if (inst.hasPermission(sender, "aurora.sacrifice.efficiency") && ChanceUtil.getChance(70)
                                || ChanceUtil.getChance(95)) {
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
                        if (ChanceUtil.getChance(80)) {
                            itemStack = new ItemStack(ItemID.CLAY_BALL, ChanceUtil.getRandom(8));
                        }
                        break;
                    case 17:
                        if (ChanceUtil.getChance(90)) {
                            itemStack = new ItemStack(ItemID.BOW);
                            itemStack.addEnchantment(Enchantment.ARROW_FIRE, 1);
                        }
                        break;
                    case 18:
                        itemStack = new ItemStack(ItemID.RED_APPLE, ChanceUtil.getRandom(6));
                        break;
                    case 19:
                        if (ChanceUtil.getChance(80)) {
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

    private void sacrifice(Player player, ItemStack item) {

        PlayerInventory pInventory = player.getInventory();

        for (ItemStack aItemStack : getCalculatedLoot(player, item)) {
            pInventory.addItem(aItemStack);
        }

        if (ChanceUtil.getChance(5) && calculateValue(item) >= 60 && !prayer.isInfluenced(player)) {

            PrayerType prayerType;
            switch (ChanceUtil.getRandom(7)) {
                case 1:
                    prayerType = PrayerType.FLASH;
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
}