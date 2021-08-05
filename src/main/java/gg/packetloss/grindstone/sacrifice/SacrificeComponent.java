/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.worldedit.math.BlockVector2;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.ConfigurationNode;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.RandomizedSkullsComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import gg.packetloss.grindstone.events.PlayerSacrificeRewardEvent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.Prayers;
import gg.packetloss.grindstone.state.player.NativeSerializerComponent;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import gg.packetloss.grindstone.util.task.TaskBuilder;
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
import java.util.concurrent.TimeUnit;

import static gg.packetloss.grindstone.sacrifice.SacrificeCommonality.*;

@ComponentInformation(friendlyName = "Sacrifice", desc = "Sacrifice! Sacrifice! Sacrifice!")
@Depend(components = {
        SessionComponent.class, PrayerComponent.class, AdminComponent.class, HighScoresComponent.class,
        NativeSerializerComponent.class, RandomizedSkullsComponent.class
})
public class SacrificeComponent extends BukkitComponent implements Listener, Runnable {
    private static SacrificeComponent inst;

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private PrayerComponent prayer;
    @InjectComponent
    private AdminComponent admin;
    @InjectComponent
    private HighScoresComponent highScores;
    @InjectComponent
    private NativeSerializerComponent nativeSerializer;
    @InjectComponent
    private RandomizedSkullsComponent randomizedSkulls;

    private LocalConfiguration config;
    private final List<Item> protectedEntities = new ArrayList<>();

    private final SacrificialRegistry registry = new SacrificialRegistry();

    public SacrificeComponent() {
        inst = this;
    }

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());

        populateRegistry();

        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            registrar.registerAsSubCommand("sacrifice", "Sacrifice Commands", (sacrificeRegistrar) -> {
                sacrificeRegistrar.register(SacrificeCommandsRegistration.builder(), new SacrificeCommands(this));
            });
        });

        CommandBook.registerEvents(this);
        CommandBook.server().getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, 20 * 2, 20);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    protected static class LocalConfiguration extends ConfigurationBase {
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

        @Setting("sacrificial-debounce-seconds")
        public int debounceSeconds = 5;

        @Setting("sacrificial-value.min-multiplier")
        public double valueMinMultiplier = .9;

        @Setting("sacrificial-value.max-multiplier")
        public double valueMaxMultiplier = 1.1;

        @Setting("sacrificial-prayer-cost")
        public double costPerPrayer = 1200;
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

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.GOD_FISH), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_HELMET), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_CHESTPLATE), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_LEGGINGS), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_BOOTS), RARE_4);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.HOLY_COMBAT_POTION), RARE_4);

        registry.registerItem(() -> {
            return randomizedSkulls.getRandomSkull().orElse(new ItemStack(Material.PLAYER_HEAD));
        }, RARE_5);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.PEACEFUL_WARRIOR_HELMET), RARE_5);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.PEACEFUL_WARRIOR_CHESTPLATE), RARE_5);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.PEACEFUL_WARRIOR_LEGGINGS), RARE_5);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.PEACEFUL_WARRIOR_BOOTS), RARE_5);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.DIVINE_COMBAT_POTION), RARE_6);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.NINJA_OATH), RARE_7);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ROGUE_OATH), RARE_7);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_HELMET), RARE_8);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_CHESTPLATE), RARE_8);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_LEGGINGS), RARE_8);
        registry.registerItem(() -> CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_BOOTS), RARE_8);

        registry.registerItem(() -> CustomItemCenter.build(CustomItems.PHANTOM_CLOCK), UBER_RARE);
    }

    public double getValue(ItemStack itemStack) {
        return registry.getValue(itemStack);
    }

    protected LocalConfiguration getConfig() {
        return config;
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
    @Deprecated
    public static List<ItemStack> getCalculatedLoot(CommandSender sender, int max, double value) {
        return getCalculatedLoot(new SacrificeInformation(sender, max, value)).getItemStacks();
    }

    public static SacrificeResult getCalculatedLoot(SacrificeInformation sacrificeInformation) {
        return inst.registry.getCalculatedLoot(sacrificeInformation);
    }

    // For use by SacrificeSession
    protected static NativeSerializerComponent getNativeSerializer() {
        return inst.nativeSerializer;
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
            session.pollItems((itemStack -> {
                ItemStack remainder = inventory.addItem(itemStack).get(0);
                if (remainder != null) {
                    session.addItem(remainder);
                    return true;
                }

                return false;
            }));

            final int ending = session.remaining();

            if (starting != ending) {
                ChatUtil.sendNotice(player, "Items deposited, " + ending + " items remaining!");
            }
        }
    }

    public boolean isSacrificeBlock(Block block) {
        return block.getType() == config.getSacrificialBlock();
    }

    protected SacrificialPitChecker checkForPitAt(Location location) {
        return new SacrificialPitChecker(location, this::isSacrificeBlock);
    }

    public boolean isPointOfSacrifice(Location location) {
        return LocationUtil.isChunkLoadedAt(location) && checkForPitAt(location).isValid();
    }

    @EventHandler
    public void onEntityCombust(EntityCombustEvent event) {
        //noinspection SuspiciousMethodCalls
        if (event.getEntity() instanceof Item && protectedEntities.contains(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private final Map<UUID, DebounceHandle<List<ItemStack>>> playerToSacrificeHandle = new HashMap<>();

    private DebounceHandle<List<ItemStack>> createPlayerHandle(Player player) {
        player.sendMessage(ChatColor.GOLD + "An ancient fire ignites.");

        TaskBuilder.Debounce<List<ItemStack>> builder = TaskBuilder.debounce();
        builder.setWaitTime(20 * config.debounceSeconds);

        builder.setInitialValue(new ArrayList<>());
        builder.setUpdateFunction((oldList, newList) -> {
            oldList.addAll(newList);
            return oldList;
        });

        builder.setBounceAction((items) -> {
            // Clear out any items that were removed by the sacrifice item event
            items.removeIf((i) -> i.getType().isAir());
            if (!items.isEmpty()) {
                sacrifice(player, items);
            }

            playerToSacrificeHandle.remove(player.getUniqueId());
        });

        return builder.build();
    }

    private DebounceHandle<List<ItemStack>> getOrCreatePlayerHandle(Player player) {
        UUID playerID = player.getUniqueId();
        if (!playerToSacrificeHandle.containsKey(playerID)) {
            playerToSacrificeHandle.put(playerID, createPlayerHandle(player));
        }

        return playerToSacrificeHandle.get(playerID);
    }

    private final Map<BlockVector2, DebounceHandle<Integer>> pointToFireHandle = new HashMap<>();

    private DebounceHandle<Integer> createFireHandle(BlockVector2 point, SacrificialPitChecker checker) {
        TaskBuilder.Debounce<Integer> builder = TaskBuilder.debounce();
        builder.setWaitTime(20 * config.debounceSeconds);

        builder.setInitialValue(0);
        builder.setUpdateFunction(Integer::sum);

        builder.setBounceAction((numRelights) -> {
            checker.extinguish();
            pointToFireHandle.remove(point);
        });

        return builder.build();
    }

    private DebounceHandle<Integer> getOrCreateFireHandle(BlockVector2 point, SacrificialPitChecker checker) {
        if (!pointToFireHandle.containsKey(point)) {
            pointToFireHandle.put(point, createFireHandle(point, checker));
        }

        return pointToFireHandle.get(point);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        final Player player = event.getPlayer();
        final Item item = event.getItemDrop();
        if (!player.hasPermission("aurora.sacrifice")) return;

        protectedEntities.add(item);

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setAction((times) -> {
            SacrificialPitChecker checker = checkForPitAt(item.getLocation());
            if (checker.isValid()) {
                return true;
            }

            if (item.isDead() || item.getTicksLived() > 40) {
                return true;
            }

            return false;
        });

        taskBuilder.setFinishAction(() -> {
            Location itemLoc = item.getLocation();

            SacrificialPitChecker checker = checkForPitAt(itemLoc);
            if (checker.isValid()) {
                PlayerSacrificeItemEvent sacrificeItemEvent = new PlayerSacrificeItemEvent(player, itemLoc, item.getItemStack());
                CommandBook.callEvent(sacrificeItemEvent);
                if (sacrificeItemEvent.isCancelled()) return;

                checker.ignite();

                getOrCreateFireHandle(checker.getIdentifyingPoint(), checker).accept(1);
                getOrCreatePlayerHandle(player).accept(List.of(sacrificeItemEvent.getItemStack()));

                item.remove();
            }

            protectedEntities.remove(item);
        });

        taskBuilder.build();
    }

    @Override
    public void run() {
        for (World world : CommandBook.server().getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
                Location entityLoc = entity.getLocation();
                if (isPointOfSacrifice(entityLoc)) {
                    Location airLoc = LocationUtil.findRandomLoc(entityLoc, 3);

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

    private static final List<Prayers> SACRIFICE_PRAYERS = List.of(
        Prayers.DIGGYDIGGY,
        Prayers.HEALTH,
        Prayers.POWER,
        Prayers.SPEED,
        Prayers.ANTIFIRE,
        Prayers.NIGHT_VISION,
        Prayers.DEADLYDEFENSE
    );

    private Prayers getRandomSacrificePrayer() {
        return CollectionUtil.getElement(SACRIFICE_PRAYERS);
    }

    private void sacrifice(Player player, List<ItemStack> items) {
        double totalValue = 0;

        MarketItemLookupInstance lookupInst = MarketComponent.getLookupInstanceFromStacksImmediately(items);
        for (ItemStack itemStack : items) {
            double value = registry.getValue(lookupInst, itemStack);
            if (value <= 0) {
                GeneralPlayerUtil.giveItemToPlayer(player, itemStack);
                continue;
            }

            totalValue += value;
        }

        if (totalValue <= 0) {
            ChatUtil.sendError(player, "The gods reject your offer.");
            return;
        }

        totalValue *= ChanceUtil.getRangedRandom(config.valueMinMultiplier, config.valueMaxMultiplier);

        PlayerSacrificeRewardEvent event = new PlayerSacrificeRewardEvent(player, totalValue);
        CommandBook.callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        highScores.update(player, ScoreTypes.SACRIFICED_VALUE, Math.round(totalValue));

        SacrificeSession session = sessions.getSession(SacrificeSession.class, player);
        session.addItems(getCalculatedLoot(new SacrificeInformation(player, totalValue)));

        PlayerInventory pInventory = player.getInventory();
        session.pollItems((itemStack -> {
            ItemStack remainder = pInventory.addItem(itemStack).get(0);
            if (remainder != null) {
                session.addItem(remainder);
                return true;
            }

            return false;
        }));

        Set<Prayers> givenPrayers = new HashSet<>();
        for (double i = totalValue; i > 0; i -= config.costPerPrayer) {
            if (!ChanceUtil.getChance(5)) {
                continue;
            }

            Prayers prayer = getRandomSacrificePrayer();
            if (givenPrayers.contains(prayer)) {
                continue;
            }

            givenPrayers.add(prayer);
            PrayerComponent.constructPrayer(player, prayer, TimeUnit.MINUTES.toMillis(60));
        }

        if (!givenPrayers.isEmpty()) {
            ChatUtil.sendNotice(player, "You have been blessed with: ");
            for (Prayers prayerType : givenPrayers) {
                ChatUtil.sendNotice(player, " - " + prayerType.getChatColor() + prayerType.getFormattedName());
            }
        }

        if (session.hasItems()) {
            ChatUtil.sendNotice(player, "The gods give you the divine touch!");
            ChatUtil.sendNotice(player, " - Punch a chest to fill it with items");
        }
    }
}