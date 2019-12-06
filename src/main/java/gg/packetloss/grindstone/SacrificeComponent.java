/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import gg.packetloss.grindstone.exceptions.UnsupportedPrayerException;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.prayer.Prayer;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Sacrifice", desc = "Sacrifice! Sacrifice! Sacrifice!")
@Depend(components = {SessionComponent.class, PrayerComponent.class, AdminComponent.class, HighScoresComponent.class})
public class SacrificeComponent extends BukkitComponent implements Listener, Runnable {

    private static final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private PrayerComponent prayer;
    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private HighScoresComponent highScores;

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

        entityTaskId.remove(entity.getEntityId());

    }

    private int getEntityTaskId(Entity entity) {

        int getEntityTaskId = 0;
        if (entityTaskId.containsKey(entity.getEntityId())) {
            getEntityTaskId = entityTaskId.get(entity.getEntityId());
        }
        return getEntityTaskId;
    }

    private static int calculateModifier(double value) {

        return (int) (Math.sqrt(value) * 1.5);
    }

    /**
     * This method is used to get the ItemStacks to drop based
     * on a numerical value and quantity.
     *
     * @param sender - The triggering sender
     * @param max    - The maximum amount of items to return
     * @param value  - The value put towards the items returned
     * @return - The ItemStacks that should be received
     */
    public static List<ItemStack> getCalculatedLoot(CommandSender sender, int max, double value) {

        List<ItemStack> loot = new ArrayList<>();

        // Calculate the modifier
        int baseChance = sender instanceof Player && inst.hasPermission(sender, "aurora.tome.sacrifice") ? 100 : 125;
        int modifier = calculateModifier(value);

        value *= .9;

        while (value > 0 && (max == -1 || max > 0)) {

            ItemStack itemStack;
            boolean wasJunk = false;

            if (ChanceUtil.getChance(Math.max(1, baseChance - modifier))) {
                itemStack = getValuableItem(sender, modifier);
            } else {
                wasJunk = true;
                itemStack = getCommonItemStack(sender, modifier);
            }

            if (itemStack != null) {
                value -= Math.max(9, MarketComponent.priceCheck(itemStack));
                if (!wasJunk || !(sender instanceof Player && sender.hasPermission("aurora.tome.cleanly"))) {
                    loot.add(itemStack);
                }
            }

            if (max != -1) {
                max--;
            }
        }
        return loot;
    }

    private static ItemStack getValuableItem(CommandSender sender, int modifier) {

        ItemStack itemStack = null;

        switch (ChanceUtil.getRandom(23)) {
            case 1:
                if (Util.getChance(sender, modifier, 1.2)) {
                    if (ChanceUtil.getChance(2)) {
                        itemStack = CustomItemCenter.build(CustomItems.GOD_SWORD);
                    } else {
                        itemStack = CustomItemCenter.build(CustomItems.GOD_SHORT_SWORD);
                    }
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_SWORD);
                }
                break;
            case 2:
                if (Util.getChance(sender, modifier, 1.2)) {
                    itemStack = CustomItemCenter.build(CustomItems.GOD_BOW);
                } else {
                    itemStack = CustomItemCenter.build(CustomItems.OVERSEER_BOW);
                }
                break;
            case 3:
                if (Util.getChance(sender, modifier, 2)) {
                    itemStack = CustomItemCenter.build(CustomItems.LEGENDARY_GOD_PICKAXE);
                } else if (Util.getChance(sender, modifier, .37)) {
                    itemStack = CustomItemCenter.build(CustomItems.GOD_PICKAXE);
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_PICKAXE);
                }
                break;
            case 4:
                if (ChanceUtil.getChance(10000)) {
                    itemStack = CustomItemCenter.build(CustomItems.PHANTOM_CLOCK);
                } else {
                    itemStack = CustomItemCenter.build(CustomItems.GOD_FISH);
                }
                break;
            case 5:
                itemStack = new ItemStack(ItemID.BOTTLE_O_ENCHANTING, ChanceUtil.getRangedRandom(40, 64));
                break;
            case 6:
                if (sender instanceof Player && Util.getChance(sender, modifier, 3)) {
                    itemStack = ItemUtil.makeSkull(sender.getName());
                } else {
                    itemStack = CustomItemCenter.build(CustomItems.PIXIE_DUST, ChanceUtil.getRangedRandom(3, 6));
                }
                break;
            case 7:
                if (Util.getChance(sender, modifier, .8)) {
                    itemStack = CustomItemCenter.build(CustomItems.GOD_CHESTPLATE);
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_CHEST);
                }
                break;
            case 8:
                if (Util.getChance(sender, modifier, .8)) {
                    itemStack = CustomItemCenter.build(CustomItems.GOD_LEGGINGS);
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_PANTS);
                }
                break;
            case 9:
                itemStack = new ItemStack(ItemID.BLAZE_ROD, ChanceUtil.getRangedRandom(20, 32));
                break;
            case 10:
                itemStack = new ItemStack(ItemID.GLISTERING_MELON, ChanceUtil.getRangedRandom(20, 32));
                break;
            case 11:
                itemStack = new ItemStack(ItemID.FERMENTED_SPIDER_EYE, ChanceUtil.getRangedRandom(20, 32));
                break;
            case 12:
                itemStack = new ItemStack(ItemID.GHAST_TEAR, ChanceUtil.getRangedRandom(20, 32));
                break;
            case 13:
                if (Util.getChance(sender, modifier, 2.75)) {
                    itemStack = CustomItemCenter.build(CustomItems.ANCIENT_BOOTS);
                }
                break;
            case 14:
                if (Util.getChance(sender, modifier, 2.75)) {
                    itemStack = CustomItemCenter.build(CustomItems.ANCIENT_LEGGINGS);
                }
                break;
            case 15:
                if (Util.getChance(sender, modifier, 2.75)) {
                    itemStack = CustomItemCenter.build(CustomItems.ANCIENT_CHESTPLATE);
                }
                break;
            case 16:
                if (Util.getChance(sender, modifier, 2.75)) {
                    itemStack = CustomItemCenter.build(CustomItems.ANCIENT_HELMET);
                }
                break;
            case 17:
                if (Util.getChance(sender, modifier, .8)) {
                    itemStack = CustomItemCenter.build(CustomItems.GOD_HELMET);
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_HELMET);
                }
                break;
            case 18:
                if (Util.getChance(sender, modifier, .8)) {
                    itemStack = CustomItemCenter.build(CustomItems.GOD_BOOTS);
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_BOOTS);
                }
                break;
            case 19:
                if (Util.getChance(sender, modifier, 5)) {
                    itemStack = CustomItemCenter.build(CustomItems.DIVINE_COMBAT_POTION);
                } else if (Util.getChance(sender, modifier, 2)) {
                    itemStack = CustomItemCenter.build(CustomItems.HOLY_COMBAT_POTION);
                } else {
                    itemStack = CustomItemCenter.build(CustomItems.EXTREME_COMBAT_POTION);
                }
                break;
            case 20:
                itemStack = new ItemStack(ItemID.DIAMOND, ChanceUtil.getRandom(3));
                break;
            case 21:
                itemStack = new ItemStack(ItemID.EMERALD, ChanceUtil.getRandom(3));
                break;
            case 22:
                if (Util.getChance(sender, modifier, 2.5)) {
                    itemStack = CustomItemCenter.build(CustomItems.LEGENDARY_GOD_AXE);
                } else if (Util.getChance(sender, modifier, .37)) {
                    itemStack = CustomItemCenter.build(CustomItems.GOD_AXE);
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_AXE);
                }
                break;
            case 23:
                itemStack = new ItemStack(ItemID.NAME_TAG);
                break;
        }
        return itemStack;
    }

    private static ItemStack getCommonItemStack(CommandSender sender, int modifier) {

        ItemStack itemStack = null;

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
                itemStack = new ItemStack(ItemID.PUMPKIN_PIE);
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
                itemStack = new ItemStack(ItemID.EGG);
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

        return itemStack;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPunch(PlayerInteractEvent event) {

        if (!event.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;

        Player player = event.getPlayer();
        BlockState clicked = event.getClickedBlock().getState();
        SacrificeSession session = sessions.getSession(SacrificeSession.class, player);

        if (session.hasItems() && clicked instanceof Chest) {
            final int starting = session.remaining();
            Inventory inventory = ((Chest) clicked).getInventory();
            while (inventory.firstEmpty() != -1 && session.hasItems()) {
                inventory.addItem(session.pollItem());
            }
            final int ending = session.remaining();

            if (starting != ending) {
                ChatUtil.sendNotice(player, "Items deposited, " + ending + " items remaining!");
            }
        }
    }

    public boolean isSacrificeBlock(Block block) {
        return block.getTypeId() == config.getSacrificialBlockId() &&
                block.getData() == config.getSacrificialBlockData();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (isSacrificeBlock(block)) {
            event.setDropItems(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        if (isSacrificeBlock(block) && !admin.isAdmin(event.getPlayer())) {
            event.setCancelled(true);
        }
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
        if (!isSacrificeBlock(origin)) return;

        final Block above = origin.getRelative(BlockFace.UP);
        if (above.getTypeId() == BlockID.AIR) {
            above.setTypeId(BlockID.FIRE);
            server.getScheduler().runTaskLater(inst, () -> above.setTypeId(BlockID.AIR), 20 * 4);

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

        int taskId = server.getScheduler().scheduleSyncRepeatingTask(inst, () -> {
            int id = getEntityTaskId(item);

            if (item.isDead() || item.getTicksLived() > 40) {

                if (id != -1) {
                    removeEntity(item);
                    server.getScheduler().cancelTask(id);
                }
            } else {
                final Block searchBlock = item.getLocation().getBlock().getRelative(BlockFace.DOWN);

                if (isSacrificeBlock(searchBlock)) {
                    try {
                        // Create the event here
                        PlayerSacrificeItemEvent sacrificeItemEvent = new PlayerSacrificeItemEvent(player, searchBlock, item.getItemStack());
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
        }, 0, 1); // Start at 0 ticks and repeat every 1 ticks
        addEntityTaskId(item, taskId);
    }

    @Override
    public void run() {
        for (World world : server.getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
                Location entityLoc = entity.getLocation();
                Block searchBlock = entityLoc.getBlock().getRelative(BlockFace.DOWN, 3);
                if (isSacrificeBlock(searchBlock)) {
                    Location airLoc = LocationUtil.findRandomLoc(searchBlock, 3);

                    Location newLoc = new Location(entity.getWorld(), airLoc.getX(), airLoc.getY(),
                            airLoc.getZ(), entityLoc.getYaw(), entityLoc.getPitch());

                    Location[] smokeLocation = new Location[4];
                    smokeLocation[0] = entity.getLocation();
                    smokeLocation[1] = entity.getEyeLocation();
                    smokeLocation[2] = newLoc;
                    smokeLocation[3] = newLoc.getBlock().getRelative(BlockFace.UP).getLocation();

                    Entity vehicle = entity.getVehicle();
                    if (vehicle != null) {
                        vehicle.eject();
                    }
                    entity.teleport(newLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    if (vehicle != null) {
                        vehicle.teleport(entity, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        vehicle.setPassenger(entity);
                    }
                    EnvironmentUtil.generateRadialEffect(smokeLocation, Effect.SMOKE);
                }
            }
        }
    }

    private double getValue(ItemStack item) {
        // FIXME: Hard coded as a workaround for the market no longer working with spawn eggs
        if (item.getType() == Material.MONSTER_EGG) {
            return 12.5;
        }
        return MarketComponent.priceCheck(item);
    }

    private void sacrifice(Player player, ItemStack item) {

        if (item.getTypeId() == 0) return;

        PlayerInventory pInventory = player.getInventory();

        final double value = getValue(item);

        if (value < 0) {
            pInventory.addItem(item);
            ChatUtil.sendError(player, "The gods reject your offer.");
            return;
        }

        highScores.update(player, ScoreTypes.SACRIFICED_VALUE, (int) Math.ceil(value));

        SacrificeSession session = sessions.getSession(SacrificeSession.class, player);
        session.addItems(getCalculatedLoot(player, -1, value));

        while (pInventory.firstEmpty() != -1 && session.hasItems()) {
            pInventory.addItem(session.pollItem());
        }

        if (session.hasItems()) {
            ChatUtil.sendNotice(player, "The gods give you the divine touch!");
            ChatUtil.sendNotice(player, " - Punch a chest to fill it with items");
        }

        if (ChanceUtil.getChance(5) && value >= 500) {

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
                Prayer givenPrayer = PrayerComponent.constructPrayer(player, prayerType, TimeUnit.MINUTES.toMillis(60));
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

            Player player = PlayerUtil.checkPlayer(sender);

            ItemStack questioned = player.getInventory().getItemInHand();

            // Check value & validity
            double value = MarketComponent.priceCheck(questioned);
            if (value < 0) throw new CommandException("You can't sacrifice that!");

            // Mask the value so it doesn't just show the market price and print it
            int shownValue = (int) Math.round(value * 60.243);
            ChatUtil.sendNotice(player, "That item has a value of: " + shownValue + " in the sacrificial pit.");
        }
    }

    private static class Util {

        public static boolean getChance(CommandSender sender, int modifier, double rarityL) {

            boolean hasEfficiency = inst.hasPermission(sender, "aurora.sacrifice.efficiency");
            int baseChance = (int) (hasEfficiency ? rarityL * 100 : rarityL * 200);

            return ChanceUtil.getChance(Math.max(1, baseChance - modifier));
        }
    }

    // Sacrifice Session
    private static class SacrificeSession extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.MINUTES.toMillis(30);

        private Queue<ItemStack> queue = new ConcurrentLinkedQueue<>();

        protected SacrificeSession() {

            super(MAX_AGE);
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }

        public void addItems(List<ItemStack> itemStacks) {

            queue.addAll(itemStacks);
        }

        public ItemStack pollItem() {

            return queue.poll();
        }

        public boolean hasItems() {

            return !queue.isEmpty();
        }

        public int remaining() {

            return queue.size();
        }
    }
}