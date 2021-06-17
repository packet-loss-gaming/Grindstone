/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.CursedMine;

import com.google.common.collect.ImmutableList;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.Prayers;
import gg.packetloss.grindstone.prayer.effect.passive.InventoryEffect;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.block.BlockStateComponent;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.listener.InventorySlotBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.restoration.RestorationUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;

@ComponentInformation(friendlyName = "Cursed Mine", desc = "Dave says hi")
@Depend(plugins = {"WorldGuard"}, components = {
        AdminComponent.class, BlockStateComponent.class, HighScoresComponent.class,
        PrayerComponent.class, RestorationUtil.class, SpectatorComponent.class
})
public class CursedMineArea extends AreaComponent<CursedMineConfig> {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected BlockStateComponent blockState;
    @InjectComponent
    protected HighScoresComponent highScores;
    @InjectComponent
    protected PrayerComponent prayer;
    @InjectComponent
    protected RestorationUtil restorationUtil;
    @InjectComponent
    protected SpectatorComponent spectator;

    protected ProtectedRegion floodGate_RG;
    protected ProtectedRegion skullsEast_RG;
    protected ProtectedRegion skullsNorth_RG;
    protected ProtectedRegion skullsWest_RG;

    protected final long lastActivationTime = 18000;
    protected long lastActivation = 0;
    protected Map<UUID, Long> daveHitList = new HashMap<>();

    protected static Set<Material> AFFECTED_MATERIALS = new HashSet<>();

    static {
        // Automatically compute the list of affected materials based on drain rate
        for (Material type : Material.values()) {
            if (type.isLegacy()) {
                continue;
            }

            if (getMaxStackDrain(type) > 0) {
                AFFECTED_MATERIALS.add(type);
            }
        }
    }

    @Override
    public void setUp() {
        spectator.registerSpectatorKind(PlayerStateKind.CURSED_MINE_SPECTATOR);

        world = server.getWorlds().get(0);

        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        String base = "oblitus-district-cursed-mine";
        region = manager.getRegion(base);
        floodGate_RG = manager.getRegion(base + "-flood-gate");
        skullsEast_RG = manager.getRegion(base + "-deaths-east");
        skullsNorth_RG = manager.getRegion(base + "-deaths-north");
        skullsWest_RG = manager.getRegion(base + "-deaths-west");
        tick = 4 * 20;
        listener = new CursedMineListener(this);
        config = new CursedMineConfig();

        setupGhosts();

        CommandBook.registerEvents(new FlightBlockingListener(admin, this::contains));
        CommandBook.registerEvents(new InventorySlotBlockingListener(
                this::isParticipant,
                (st) -> st == InventoryType.SlotType.CRAFTING
        ));

        spectator.registerSpectatedRegion(PlayerStateKind.CURSED_MINE_SPECTATOR, region);
        spectator.registerSpectatorSkull(
                PlayerStateKind.CURSED_MINE_SPECTATOR,
                new Location(world, 353, 66, -529),
                () -> getContainedParticipants().size() > 1
        );
    }

    @Override
    public void run() {
        removeExpiredHitlistEntries();

        drain();
        trySpawnBlazes();
        restoreReadyBlocks();

        if (!isEmpty()) {
            changeWater();
        }
    }

    private void removeExpiredHitlistEntries() {
        daveHitList.entrySet().removeIf(stringLongEntry -> stringLongEntry.getValue() >= System.currentTimeMillis());
    }

    public void addToHitList(Player player) {
        daveHitList.put(player.getUniqueId(), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
    }

    public void removeFromHitList(Player player) {
        daveHitList.remove(player.getUniqueId());
    }

    public boolean isOnHitList(Player player) {
        return daveHitList.containsKey(player.getUniqueId());
    }

    public void addSkull(Player player) {
        ProtectedRegion r;
        BlockFace direction;

        switch (ChanceUtil.getRandom(3)) {
            case 1:
                r = skullsEast_RG;
                direction = BlockFace.WEST;
                break;
            case 2:
                r = skullsNorth_RG;
                direction = BlockFace.SOUTH;
                break;
            case 3:
                r = skullsWest_RG;
                direction = BlockFace.EAST;
                break;
            default:
                return;
        }

        if (r != null) {
            Location l = LocationUtil.pickLocation(getWorld(), r.getMinimumPoint().getY(), r.getMinimumPoint(), r.getMaximumPoint());
            SkullPlacer.placePlayerSkullOnGround(l, direction, player);
        }
    }

    private static int getMaxStackDrain(Material type) {
        switch (type) { // grouped by drain category, ordered by mine depth
            case IRON_BLOCK:
            case GOLD_BLOCK:
            case REDSTONE_BLOCK:
            case LAPIS_BLOCK:
            case DIAMOND_BLOCK:
            case EMERALD_BLOCK:
                return 2;

            case IRON_ORE:
            case GOLD_ORE:
            case REDSTONE_ORE:
            case LAPIS_ORE:
            case DIAMOND_ORE:
            case EMERALD_ORE:
                return 4;

            case IRON_INGOT:
            case GOLD_INGOT:
            case DIAMOND:
            case EMERALD:
                return 8;

            case REDSTONE:
            case LAPIS_LAZULI:
                return 34;

            case GOLD_NUGGET:
            case IRON_NUGGET:
                return 80;

            default:
                return 0;
        }
    }

    public int getMaxStackDrain(ItemStack stack) {
        if (stack == null || ItemUtil.isNamed(stack)) {
            return 0;
        }

        return getMaxStackDrain(stack.getType());
    }

    private boolean checkInventory(Player player, ItemStack[] itemStacks) {
        if (!inst.hasPermission(player, "aurora.tome.divinity")) return false;

        for (ItemStack stack : itemStacks) {
            if (getMaxStackDrain(stack) > 0) {
                return true;
            }
        }

        return false;
    }

    private void drainPlayers() {
        for (Entity e : getContained(InventoryHolder.class)) {
            Inventory eInventory = ((InventoryHolder) e).getInventory();

            if (e instanceof Player) {
                if (!isParticipant((Player) e)) {
                    continue;
                }

                boolean hasItems = checkInventory((Player) e, eInventory.getContents());
                if (hasItems && ChanceUtil.getChance(15)) {
                    ChatUtil.sendNotice(e, "Divine intervention protects some of your items.");
                    continue;
                }
            }

            for (int i = 0; i < eInventory.getSize(); ++i) {
                ItemStack stack = eInventory.getItem(i);

                int maxDrainAmount = getMaxStackDrain(stack);
                if (maxDrainAmount == 0) {
                    continue;
                }

                assert stack != null;

                int drainAmount = ChanceUtil.getRandom(maxDrainAmount);
                int newAmt = stack.getAmount() - drainAmount;
                if (newAmt < 1) {
                    eInventory.setItem(i, null);
                } else {
                    stack.setAmount(newAmt);
                }
            }
        }
    }

    private void drainFloor() {
        for (Item item : getContained(Item.class)) {
            if (!contains(item)) continue;

            ItemStack stack = item.getItemStack();

            int maxDrainAmount = getMaxStackDrain(stack);
            if (maxDrainAmount == 0) {
                continue;
            }

            int drainAmount = ChanceUtil.getRandom(maxDrainAmount);
            int newAmt = stack.getAmount() - drainAmount;
            if (newAmt < 1) {
                item.remove();
            } else {
                item.getItemStack().setAmount(newAmt);
            }
        }
    }

    public void drain() {
        drainPlayers();
        drainFloor();
    }

    private boolean isInEmeraldMine(Entity entity) {
        return entity.getLocation().getY() < 30;
    }

    private boolean shouldSpawnBlazes() {
        long diff = System.currentTimeMillis() - lastActivation;
        return lastActivation == 0 || diff <= lastActivationTime * .35 || diff >= lastActivationTime * 5;
    }

    private void trySpawnBlazes() {
        if (!shouldSpawnBlazes()) {
            return;
        }

        for (Player player : getContainedParticipants()) {
            if (!isInEmeraldMine(player)) {
                continue;
            }

            Location playerLoc = player.getLocation();
            for (int i = 0; i < ChanceUtil.getRangedRandom(2, 5); i++) {
                Location spawnLoc = LocationUtil.findRandomLoc(playerLoc, 5, true, false);

                if (spawnLoc.getBlock().getType() != Material.AIR) {
                    spawnLoc = playerLoc;
                }

                getWorld().spawn(spawnLoc, Blaze.class);
            }
        }
    }

    public void restoreReadyBlocks() {
        blockState.popBlocksOlderThan(
                BlockStateKind.CURSED_MINE,
                TimeUnit.SECONDS.toMillis(30)
        );
    }

    public void revertPlayerBlocks(Player player) {
        blockState.popBlocksCreatedBy(BlockStateKind.CURSED_MINE, player);
    }

    private static final Set<Material> REPLACEABLE_TYPES = Set.of(
            Material.WATER, Material.OAK_PLANKS, Material.AIR
    );

    private Material getNewFloodGateType() {
        if (lastActivation == 0 || System.currentTimeMillis() - lastActivation >= lastActivationTime) {
            return Material.OAK_PLANKS;
        } else {
            return Material.AIR;
        }
    }

    private void changeWater() {
        Material newType = getNewFloodGateType();

        RegionWalker.walk(floodGate_RG, (x, y, z) -> {
            Block block = getWorld().getBlockAt(x, y, z);
            if (REPLACEABLE_TYPES.contains(block.getType())) {
                block.setType(newType);
            }
        });
    }

    protected void eatFood(Player player) {
        if (!ChanceUtil.getChance(10)) {
            return;
        }

        if (player.getSaturation() - 1 >= 0) {
            player.setSaturation(player.getSaturation() - 1);
        } else if (player.getFoodLevel() - 1 >= 0) {
            player.setFoodLevel(player.getFoodLevel() - 1);
        } else if (player.getHealth() - 1 >= 0) {
            player.setHealth(player.getHealth() - 1);
        }
    }

    protected void poison(Player player, int duration) {
        if (ChanceUtil.getChance(player.getLocation().getBlockY() / 2)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * duration, 2));
            ChatUtil.sendWarning(player, "The ore releases a toxic gas poisoning you!");
        }
    }

    protected static final Predicate<SummoningContext> GOOD_GHOST = (ctx) -> !ctx.isEvil();
    protected static final Predicate<SummoningContext> EVIL_GHOST = SummoningContext::isEvil;

    protected final Ghost CASPHER = new Ghost("Caspher", GOOD_GHOST);
    protected final Ghost COOKIE = new Ghost("COOKIE", GOOD_GHOST);
    protected final Ghost JOHN = new Ghost("John", GOOD_GHOST);
    protected final Ghost TIM = new Ghost("Tim", GOOD_GHOST);
    protected final Ghost DAN = new Ghost("Dan", GOOD_GHOST);

    protected final Ghost PIRATE_DAVE = new Ghost("Capn' Dave", (ctx) -> {
        if (!ctx.isEvil()) {
            return false;
        }

        if (!ChanceUtil.getChance(4)) {
            return false;
        }

        return true;
    });
    protected final Ghost PYRO_DAVE = new Ghost("Pyro Dave", (ctx) -> {
        if (!ctx.isEvil()) {
            return false;
        }

        if (ctx.getBrokenBlock() != Material.DIAMOND_ORE) {
            return false;
        }

        if (!ChanceUtil.getChance(4)) {
            return false;
        }

        return true;
    });
    protected final Ghost ALCHEMY_DAVE = new Ghost("Alchemy Dave", (ctx) -> {
        if (!ctx.isEvil()) {
            return false;
        }

        if (ctx.getBrokenBlock() != Material.EMERALD_ORE) {
            return false;
        }

        return true;
    });
    protected final Ghost PARA_DAVE = new Ghost("Para Dave", EVIL_GHOST);
    protected final Ghost FAMISHED_DAVE = new Ghost("Famished Dave", EVIL_GHOST);
    protected final Ghost FRIENDLY_DAVE = new Ghost("Friendly Dave", EVIL_GHOST);
    protected final Ghost GEORGE = new Ghost("George", EVIL_GHOST);
    protected final Ghost SIMON = new Ghost("Simon", EVIL_GHOST);
    protected final Ghost BEN = new Ghost("Ben", EVIL_GHOST);
    protected final Ghost MERLIN = new Ghost("Merlin", EVIL_GHOST);
    protected final Ghost HALLOW = new Ghost("Hallow", EVIL_GHOST);
    protected final Ghost LEGIONNAIR = new Ghost("Legionnair", EVIL_GHOST);

    protected final List<Haunting> HAUNTINGS = List.of(
        // Begin Good Ghost Actions
        new Haunting(CASPHER, (player) -> {
            ChatUtil.sendNotice(player, "Caspher the friendly ghost drops some bread.");
            player.getWorld().dropItemNaturally(player.getLocation(),
                new ItemStack(Material.BREAD, ChanceUtil.getRandom(16)));
        }),
        new Haunting(CASPHER, (player) -> {
            ChatUtil.sendNotice(player, "Caspher the friendly ghost appears.");
            for (int i = 0; i < 8; i++) {
                player.getWorld().dropItemNaturally(player.getLocation(),
                    new ItemStack(Material.IRON_INGOT, ChanceUtil.getRandom(64)));
                player.getWorld().dropItemNaturally(player.getLocation(),
                    new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandom(64)));
                player.getWorld().dropItemNaturally(player.getLocation(),
                    new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(64)));
            }
        }),
        new Haunting(COOKIE, (player) -> {
            ChatUtil.sendNotice(player, "COOKIE gives you a cookie.");
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.COOKIE));
        }),
        new Haunting(JOHN, (player) -> {
            ChatUtil.sendNotice(player, "John gives you a new jacket.");
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.LEATHER_CHESTPLATE));
        }),
        new Haunting(TIM, (player) -> {
            ChatUtil.sendNotice(player, "Tim teleports items to you.");
            getContained(Item.class).forEach(i -> i.teleport(player));

            // Add in some extra drops just in case the loot wasn't very juicy
            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.IRON_INGOT, ChanceUtil.getRandom(64)));
            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandom(64)));
            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(64)));
        }),
        new Haunting(DAN, (player) -> {
            ChatUtil.sendNotice(player, "Dan gives you a sparkling touch.");

            Material type = ChanceUtil.supplyRandom(
                () -> Material.IRON_INGOT,
                () -> Material.GOLD_INGOT,
                () -> Material.DIAMOND
            );

            PrayerComponent.constructPrayer(player, true, ImmutableList.of(new InventoryEffect(type, 64)), 5000);
        }),

        // Begin Evil Ghost Actions
        new Haunting(PIRATE_DAVE, (player) -> {
            addToHitList(player);
            ChatUtil.sendWarning(player, "Capn' Dave yells: FIRE!");
            player.chat("Who's a good ghost?!?!");
            server.getScheduler().runTaskLater(inst, () -> {
                player.chat("Don't hurt me!!!");
                server.getScheduler().runTaskLater(inst, () -> {
                    player.chat("Nooooooooooo!!!");

                    PrayerComponent.constructPrayer(player, Prayers.CANNON, TimeUnit.MINUTES.toMillis(2));
                }, 20);
            }, 20);
        }),
        new Haunting(PYRO_DAVE, (player) -> {
            addToHitList(player);
            ChatUtil.sendWarning(player, "Pyro Dave lights a match! It's super effective!");
            EditSession ess = WorldEditBridge.getSystemEditSessionFor(getWorld());
            try {
                ess.fillXZ(toBlockVec3(player.getLocation()), BlockTypes.FIRE.getDefaultState(), 20, 20, true);
                ess.flushSession();
            } catch (MaxChangedBlocksException ignored) {

            }
            for (int i = ChanceUtil.getRandom(24) + 20; i > 0; --i) {
                final boolean untele = i == 11;
                server.getScheduler().runTaskLater(inst, () -> {
                    if (untele) {
                        revertPlayerBlocks(player);
                        removeFromHitList(player);
                    }

                    if (!contains(player)) return;

                    Location l = LocationUtil.findRandomLoc(player.getLocation().getBlock(), 3, true, false);
                    ExplosionStateFactory.createExplosion(l, 3, true, false);
                }, 12 * i);
            }
        }),
        new Haunting(ALCHEMY_DAVE, (player) -> {
            ChatUtil.sendNotice(player, "Dave got a chemistry set!");
            addToHitList(player);
            PrayerComponent.constructPrayer(player, Prayers.DEADLY_POTION, TimeUnit.MINUTES.toMillis(30));
        }),
        new Haunting(PARA_DAVE, (player) -> {
            ChatUtil.sendWarning(player, "You find yourself falling from the sky...");
            addToHitList(player);

            Location playerLoc = player.getLocation();
            playerLoc.setY(350);
            player.teleport(playerLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        }),
        new Haunting(FAMISHED_DAVE, (player) -> {
            ChatUtil.sendWarning(player, "Dave likes your food.");
            addToHitList(player);
            PrayerComponent.constructPrayer(player, Prayers.STARVATION, TimeUnit.MINUTES.toMillis(15));
        }),
        new Haunting(FRIENDLY_DAVE, (player) -> {
            ChatUtil.sendWarning(player, "Dave says hi, that's not good.");
            addToHitList(player);
            PrayerComponent.constructPrayer(player, Prayers.SLAP, TimeUnit.MINUTES.toMillis(30));
            PrayerComponent.constructPrayer(player, Prayers.BUTTER_FINGERS, TimeUnit.MINUTES.toMillis(30));
            PrayerComponent.constructPrayer(player, Prayers.FIRE, TimeUnit.MINUTES.toMillis(30));
        }),
        new Haunting(GEORGE, (player) -> {
            ChatUtil.sendWarning(player, "George plays with fire, sadly too close to you.");
            PrayerComponent.constructPrayer(player, Prayers.FIRE, TimeUnit.SECONDS.toMillis(45));
        }),
        new Haunting(SIMON, (player) -> {
            ChatUtil.sendWarning(player, "Simon says pick up sticks.");
            for (int i = 0; i < player.getInventory().getContents().length * 1.5; i++) {
                player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.STICK, 64));
            }
        }),
        new Haunting(BEN, (player) -> {
            ChatUtil.sendWarning(player, "Ben dumps out your backpack.");
            PrayerComponent.constructPrayer(player, Prayers.BUTTER_FINGERS, TimeUnit.SECONDS.toMillis(10));
        }),
        new Haunting(MERLIN, (player) -> {
            ChatUtil.sendWarning(player, "Merlin attacks with a mighty rage!");
            PrayerComponent.constructPrayer(player, Prayers.MERLIN, TimeUnit.SECONDS.toMillis(20));
        }),
        new Haunting(HALLOW, (player) -> {
            ChatUtil.sendWarning(player, "Hallow declares war on YOU!");
            for (int i = 0; i < ChanceUtil.getRangedRandom(10, 30); i++) {
                Blaze blaze = getWorld().spawn(player.getLocation(), Blaze.class);
                blaze.setTarget(player);
                blaze.setRemoveWhenFarAway(true);
            }
        }),
        new Haunting(LEGIONNAIR, (player) -> {
            ChatUtil.sendWarning(player, "A legion of hell hounds appears!");
            for (int i = 0; i < ChanceUtil.getRangedRandom(10, 30); i++) {
                Wolf wolf = getWorld().spawn(player.getLocation(), Wolf.class);
                wolf.setTarget(player);
                wolf.setRemoveWhenFarAway(true);
            }
        })
    );

    protected final List<Ghost> GHOST_DEFINITIONS = new ArrayList<>();

    protected final Map<String, List<Consumer<Player>>> HAUNTING_LOOKUP = new HashMap<>();

    private void setupGhosts() {
        for (Haunting haunting : HAUNTINGS) {
            Ghost ghost = haunting.getGhost();
            if (!GHOST_DEFINITIONS.contains(ghost)) {
                GHOST_DEFINITIONS.add(ghost);
                HAUNTING_LOOKUP.put(ghost.getName(), new ArrayList<>());
            }

            List<Consumer<Player>> registeredHauntings = HAUNTING_LOOKUP.get(ghost.getName());
            registeredHauntings.add(haunting.getAction());
        }
    }

    protected boolean isGhost(Zombie zombie) {
        String customName = zombie.getCustomName();
        if (customName == null) {
            return false;
        }

        if (!contains(zombie)) {
            return false;
        }

        return HAUNTING_LOOKUP.containsKey(customName);
    }

    protected boolean tryTriggerHaunting(Zombie zombie, Player target) {
        if (!contains(zombie)) {
            return false;
        }

        String customName = zombie.getCustomName();
        if (customName == null) {
            return false;
        }

        List<Consumer<Player>> hauntings = HAUNTING_LOOKUP.get(customName);
        if (hauntings == null) {
            return false;
        }

        // There was a haunting, trigger it
        Consumer<Player> haunting = CollectionUtil.getElement(hauntings);
        haunting.accept(target);

        // Remove the ghost
        zombie.remove();

        return true;
    }

    private Location getGhostSpawnLocation(Player target) {
        Location targetLoc = target.getLocation();
        return new BlockWanderer(target.getLocation(), (newBlock, bestBlock) -> {
            double newDist = LocationUtil.distanceSquared2D(newBlock.getLocation(), targetLoc);
            double oldDist = LocationUtil.distanceSquared2D(bestBlock.getLocation(), targetLoc);

            if (newDist <= oldDist) {
                return false;
            }

            if (EnvironmentUtil.isLiquid(newBlock)) {
                return false;
            }

            if (newBlock.getRelative(BlockFace.UP).getType().isSolid()) {
                return false;
            }

            return true;
        }).walk().add(0.5, 0, .5);
    }

    private void spawnGhost(Ghost ghost, Player target) {
        Zombie ghostEntity = getWorld().spawn(
            getGhostSpawnLocation(target),
            Zombie.class,
            (e) -> e.getEquipment().clear()
        );

        // Setup ghost name for the haunting system
        ghostEntity.setCustomName(ghost.getName());
        ghostEntity.setCustomNameVisible(false);

        // Target the player, and also ensure the ghost doesn't stick around longer than it needs to
        ghostEntity.setTarget(target);
        ghostEntity.setRemoveWhenFarAway(true);

        // Make the ghost very fragile
        ghostEntity.setMaxHealth(1);
        ghostEntity.setHealth(1);

        // Make sure the ghost can't pickup items
        ghostEntity.setCanPickupItems(false);

        // Modify the ghost to seek further, and chase faster than a normal zombie
        try {
            AttributeBook.setAttribute(ghostEntity, AttributeBook.Attribute.MOVEMENT_SPEED, 0.35);
            AttributeBook.setAttribute(ghostEntity, AttributeBook.Attribute.FOLLOW_RANGE, 100);
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }

        // Make a sound and warn the player
        target.playSound(ghostEntity.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1, 1);
        ChatUtil.sendWarning(target, "You sense a disturbance...");
    }

    protected void ghost(Player player, Material blockType) {
        if (!ChanceUtil.getChance(player.getLocation().getBlockY())) {
            return;
        }

        boolean isEvil = ChanceUtil.getChance(2);
        if (isEvil) {
            if (ItemUtil.hasAncientArmour(player) && ChanceUtil.getChance(2)) {
                ChatUtil.sendNotice(player, ChatColor.AQUA, "Your armour blocks an incoming ghost attack.");
                return;
            }
        }

        SummoningContext summonContext = new SummoningContext(blockType, isEvil);

        // Figure out which ghost we're going to spawn
        Ghost ghost;
        do {
            ghost = CollectionUtil.getElement(GHOST_DEFINITIONS);
        } while (!ghost.shouldBeSummoned(summonContext));
        spawnGhost(ghost, player);
    }
}
