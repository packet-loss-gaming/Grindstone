package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.economic.store.AdminStoreComponent;
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
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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

import static com.skelril.aurora.util.item.ItemUtil.CustomItems;

@ComponentInformation(friendlyName = "Sacrifice", desc = "Sacrifice! Sacrifice! Sacrifice!")
@Depend(components = {SessionComponent.class, PrayerComponent.class})
public class SacrificeComponent extends BukkitComponent implements Listener, Runnable {

    private static final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
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

    private static int calculateModifier(double value) {

        return (int) (Math.sqrt(value) * 1.5);
    }

    /**
     * This method is used to get the ItemStacks to drop based
     * on a numerical value and quantity.
     *
     * @param sender - The triggering sender
     * @param max - The maximum amount of items to return
     * @param value - The value put towards the items returned
     * @return - The ItemStacks that should be received
     */
    public static List<ItemStack> getCalculatedLoot(CommandSender sender, int max, double value) {

        List<ItemStack> loot = new ArrayList<>();

        // Calculate the modifier
        int k = inst.hasPermission(sender, "aurora.sacrifice.efficiency") ? 100 : 125;
        int modifier = calculateModifier(value);

        value *= .9;

        while (value > 0 && (max == -1 || max > 0)) {

            ItemStack itemStack;

            if (ChanceUtil.getChance(Math.max(1, k - modifier))) {
                itemStack = getValuableItem(sender, modifier);
            } else {
                itemStack = getCommonItemStack(sender, modifier);
            }

            if (itemStack != null) {
                value -= Math.max(9, AdminStoreComponent.priceCheck(itemStack));
                loot.add(itemStack);
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
                    itemStack = ItemUtil.God.makeSword();
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_SWORD);
                }
                break;
            case 2:
                if (Util.getChance(sender, modifier, 1.2)) {
                    itemStack = ItemUtil.God.makeBow();
                } else {
                    itemStack = ItemUtil.Misc.overseerBow();
                }
                break;
            case 3:
                if (Util.getChance(sender, modifier, 2)) {
                    itemStack = ItemUtil.God.makePickaxe(true);
                } else if (Util.getChance(sender, modifier, .37)) {
                    itemStack = ItemUtil.God.makePickaxe(false);
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_PICKAXE);
                }
                break;
            case 4:
                if (ChanceUtil.getChance(10000)) {
                    itemStack = ItemUtil.Misc.phantomClock(1);
                } else {
                    itemStack = ItemUtil.Misc.godFish(1);
                }
                break;
            case 5:
                itemStack = new ItemStack(ItemID.BOTTLE_O_ENCHANTING, ChanceUtil.getRangedRandom(40, 64));
                break;
            case 6:
                if (sender instanceof Player && Util.getChance(sender, modifier, 3)) {
                    itemStack = ItemUtil.makeSkull(sender.getName());
                } else {
                    itemStack = ItemUtil.Misc.pixieDust(ChanceUtil.getRangedRandom(3, 6));
                }
                break;
            case 7:
                if (Util.getChance(sender, modifier, .8)) {
                    itemStack = ItemUtil.God.makeChest();
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_CHEST);
                }
                break;
            case 8:
                if (Util.getChance(sender, modifier, .8)) {
                    itemStack = ItemUtil.God.makeLegs();
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
                    itemStack = ItemUtil.Ancient.makeBoots();
                }
                break;
            case 14:
                if (Util.getChance(sender, modifier, 2.75)) {
                    itemStack = ItemUtil.Ancient.makeLegs();
                }
                break;
            case 15:
                if (Util.getChance(sender, modifier, 2.75)) {
                    itemStack = ItemUtil.Ancient.makeChest();
                }
                break;
            case 16:
                if (Util.getChance(sender, modifier, 2.75)) {
                    itemStack = ItemUtil.Ancient.makeHelmet();
                }
                break;
            case 17:
                if (Util.getChance(sender, modifier, .8)) {
                    itemStack = ItemUtil.God.makeHelmet();
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_HELMET);
                }
                break;
            case 18:
                if (Util.getChance(sender, modifier, .8)) {
                    itemStack = ItemUtil.God.makeBoots();
                } else {
                    itemStack = new ItemStack(ItemID.DIAMOND_BOOTS);
                }
                break;
            case 19:
                if (Util.getChance(sender, modifier, 5)) {
                    itemStack = ItemUtil.CPotion.divineCombatPotion();
                } else if (Util.getChance(sender, modifier, 2)) {
                    itemStack = ItemUtil.CPotion.holyCombatPotion();
                } else {
                    itemStack = ItemUtil.CPotion.extremeCombatPotion();
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
                    itemStack = ItemUtil.God.makeAxe(true);
                } else if (Util.getChance(sender, modifier, .37)) {
                    itemStack = ItemUtil.God.makeAxe(false);
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

        final double value = AdminStoreComponent.priceCheck(item);

        if (ItemUtil.isItem(item, CustomItems.MASTER_SWORD)) {
            pInventory.addItem(ItemUtil.Master.makeSword());
            return;
        } else if (ItemUtil.isItem(item, CustomItems.MASTER_BOW)) {
            pInventory.addItem(ItemUtil.Master.makeBow());
            return;
        } else if (value < 0) {
            pInventory.addItem(item);
            ChatUtil.sendError(player, "The gods reject your offer.");
            return;
        }

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

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            ItemStack questioned = ((Player) sender).getInventory().getItemInHand();

            // Check value & validity
            double value = AdminStoreComponent.priceCheck(questioned);
            if (value < 0) throw new CommandException("You can't sacrifice that!");

            // Mask the value so it doesn't just show the market price and print it
            int shownValue = (int) Math.round(value * 60.243);
            ChatUtil.sendNotice(sender, "That item has a value of: " + shownValue + " in the sacrificial pit.");
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