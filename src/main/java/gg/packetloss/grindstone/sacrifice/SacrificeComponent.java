/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.PersistentSession;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.ConfigurationNode;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import gg.packetloss.grindstone.exceptions.UnsupportedPrayerException;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.prayer.Prayer;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.*;
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

import static gg.packetloss.grindstone.sacrifice.SacrificeCommonality.*;

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

    private static SacrificialRegistry registry = new SacrificialRegistry();

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());

        populateRegistry();

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
        private Material sacrificialBlockMaterial = null;

        @Override
        public void load(ConfigurationNode node) {
            super.load(node);
            sacrificialBlockMaterial = null;
        }

        @Setting("sacrificial-block")
        private String sacrificialBlockString = Material.CHISELED_STONE_BRICKS.getKey().toString();

        public Material getSacrificialBlock() {
            if (sacrificialBlockMaterial == null) {
                sacrificialBlockMaterial = Material.matchMaterial(sacrificialBlockString);
            }

            return sacrificialBlockMaterial;
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

    private void populateRegistry() {
        registry.registerItem(() -> new ItemStack(Material.DIRT), JUNK);
        registry.registerItem(() -> new ItemStack(Material.STONE), JUNK);
        registry.registerItem(() -> new ItemStack(Material.ROSE_BUSH), JUNK);
        registry.registerItem(() -> new ItemStack(Material.DANDELION), JUNK);
        registry.registerItem(() -> new ItemStack(Material.PUMPKIN_PIE), JUNK);
        registry.registerItem(() -> new ItemStack(Material.WHEAT_SEEDS), JUNK);
        registry.registerItem(() -> new ItemStack(Material.WHEAT), JUNK);
        registry.registerItem(() -> new ItemStack(Material.OAK_PLANKS), JUNK);
        registry.registerItem(() -> new ItemStack(Material.FEATHER), JUNK);
        registry.registerItem(() -> new ItemStack(Material.GOLD_NUGGET, ChanceUtil.getRandom(9)), JUNK);
        registry.registerItem(() -> new ItemStack(Material.ARROW), JUNK);
        registry.registerItem(() -> new ItemStack(Material.BOWL), JUNK);
        registry.registerItem(() -> new ItemStack(Material.BONE), JUNK);
        registry.registerItem(() -> new ItemStack(Material.SNOWBALL), JUNK);
        registry.registerItem(() -> new ItemStack(Material.FLINT), JUNK);
        registry.registerItem(() -> new ItemStack(Material.CLAY_BALL, ChanceUtil.getRandom(8)), JUNK);
        registry.registerItem(() -> new ItemStack(Material.EGG), JUNK);
        registry.registerItem(() -> new ItemStack(Material.APPLE, ChanceUtil.getRandom(6)), JUNK);
        registry.registerItem(() -> new ItemStack(Material.GOLDEN_APPLE, ChanceUtil.getRandom(8)), JUNK);
        registry.registerItem(() -> new ItemStack(Material.ENDER_PEARL, ChanceUtil.getRandom(6)), JUNK);
        registry.registerItem(() -> new ItemStack(Material.COOKIE, ChanceUtil.getRangedRandom(8, 16)), JUNK);
        registry.registerItem(() -> new ItemStack(Material.DIAMOND), JUNK);

        registry.registerItem(() -> new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(3)), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.EMERALD, ChanceUtil.getRandom(3)), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.DIAMOND_SWORD), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.DIAMOND_HELMET), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.DIAMOND_CHESTPLATE), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.DIAMOND_LEGGINGS), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.DIAMOND_BOOTS), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.DIAMOND_PICKAXE), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.DIAMOND_AXE), NORMAL);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_FISH), NORMAL);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.OVERSEER_BOW), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.EXPERIENCE_BOTTLE, ChanceUtil.getRangedRandom(40, 64)), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.BLAZE_ROD, ChanceUtil.getRangedRandom(20, 32)), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.GLISTERING_MELON_SLICE, ChanceUtil.getRangedRandom(20, 32)), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.FERMENTED_SPIDER_EYE, ChanceUtil.getRangedRandom(20, 32)), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.GHAST_TEAR, ChanceUtil.getRangedRandom(20, 32)), NORMAL);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.PIXIE_DUST, ChanceUtil.getRangedRandom(3, 6)), NORMAL);
        registry.registerItem(() -> new ItemStack(Material.NAME_TAG), NORMAL);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_HELMET), RARE_1);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_CHESTPLATE), RARE_1);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_LEGGINGS), RARE_1);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_BOOTS), RARE_1);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_BOW), RARE_2);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_SWORD), RARE_2);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_SHORT_SWORD), RARE_2);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_AXE), RARE_2);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_PICKAXE), RARE_2);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.LEGENDARY_GOD_AXE), RARE_3);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.LEGENDARY_GOD_PICKAXE), RARE_3);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.EXTREME_COMBAT_POTION), RARE_3);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_HELMET), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_CHESTPLATE), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_LEGGINGS), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_BOOTS), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.HOLY_COMBAT_POTION), RARE_4);

        registry.registerItem(() -> {
            if (server.getOfflinePlayers().length < 1) {
                return new ItemStack(Material.PLAYER_HEAD);
            }

            return ItemUtil.makeSkull(CollectionUtil.getElement(server.getOfflinePlayers()));
        }, RARE_5);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.DIVINE_COMBAT_POTION), RARE_6);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.NINJA_OATH), RARE_7);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ROGUE_OATH), RARE_7);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.PHANTOM_CLOCK), UBER_RARE);
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
        return registry.getCalculatedLoot(sender, max, value);
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
        return block.getType() == config.getSacrificialBlock();
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
        if (!isSacrificeBlock(origin)) {
            return;
        }

        while (isSacrificeBlock(origin.getRelative(BlockFace.UP))) {
            origin = origin.getRelative(BlockFace.UP);
        }

        final Block above = origin.getRelative(BlockFace.UP);
        if (above.getType() == Material.AIR) {
            above.setType(Material.FIRE);
            server.getScheduler().runTaskLater(inst, () -> above.setType(Material.AIR), 20 * 4);

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
                Block searchBlock = item.getLocation().getBlock();

                while (isSacrificeBlock(searchBlock.getRelative(BlockFace.DOWN))) {
                    searchBlock = searchBlock.getRelative(BlockFace.DOWN);
                }

                if (isSacrificeBlock(searchBlock)) {
                    PlayerSacrificeItemEvent sacrificeItemEvent = new PlayerSacrificeItemEvent(player, searchBlock, item.getItemStack());
                    server.getPluginManager().callEvent(sacrificeItemEvent);
                    if (sacrificeItemEvent.isCancelled()) return;
                    sacrifice(player, sacrificeItemEvent.getItemStack());

                    createFire(searchBlock);
                    removeEntity(item);
                    item.remove();
                    server.getScheduler().cancelTask(id);
                    player.sendMessage(ChatColor.GOLD + "An ancient fire ignites.");
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

    private void sacrifice(Player player, ItemStack item) {
        if (item.getType() == Material.AIR) return;

        PlayerInventory pInventory = player.getInventory();

        final double value = registry.getValue(item);
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
            double value = registry.getValue(questioned);
            if (value == 0) {
                throw new CommandException("You can't sacrifice that!");
            }

            // Mask the value so it doesn't just show the market price and print it
            int shownValue = (int) Math.round(value * 60.243);
            ChatUtil.sendNotice(player, "That item has a value of: " +
                    ChatUtil.WHOLE_NUMBER_FORMATTER.format(shownValue) +
                    " in the sacrificial pit.");
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