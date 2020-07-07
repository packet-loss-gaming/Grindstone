/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.GraveYard;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.block.BlockStateComponent;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Lever;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.bukkit.block.data.type.Chest.Type;

@ComponentInformation(friendlyName = "Grave Yard", desc = "The home of the undead")
@Depend(components = {
        AdminComponent.class, PlayerStateComponent.class,
        SpectatorComponent.class, BlockStateComponent.class},
        plugins = {"WorldGuard"})
public class GraveYardArea extends AreaComponent<GraveYardConfig> {

    protected static List<PlayerStateKind> GRAVE_YARD_SPECTATOR_KINDS = List.of(
            PlayerStateKind.GRAVE_YARD_SPECTATOR,
            PlayerStateKind.GRAVE_YARD_NORTH_SPECTATOR,
            PlayerStateKind.GRAVE_YARD_SOUTH_SPECTATOR
    );

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected PlayerStateComponent playerState;
    @InjectComponent
    protected SpectatorComponent spectator;
    @InjectComponent
    protected BlockStateComponent blockState;

    // Other
    protected Economy economy;

    // Temple regions
    protected ProtectedRegion temple, pressurePlateLockArea, creepers, parkour, rewards;

    // Block information
    protected static final Set<Material> BREAKABLE = Set.of(
            Material.TALL_GRASS,
            Material.ROSE_BUSH,
            Material.DANDELION,
            Material.DIRT,
            Material.TORCH,
            Material.CRACKED_STONE_BRICKS,
            Material.COBWEB,
            Material.OAK_LEAVES
    );

    protected static final Set<Material> AUTO_BREAKABLE =  Set.of(
            Material.OAK_SLAB,
            Material.STONE_BRICK_SLAB,
            Material.CRACKED_STONE_BRICKS
    );

    // Ticks of active grave yard rewards room
    protected int rewardsRoomOccupiedTicks = 0;

    // Head Stones
    protected List<Location> headStones = new ArrayList<>();

    // Reward Chest
    protected List<Location> rewardChest = new ArrayList<>();

    // Pressure Plate Lock
    // Use a boolean to store the check value instead of checking for every step
    protected boolean isPressurePlateLocked = true;
    protected ConcurrentHashMap<Location, Boolean> pressurePlateLocks = new ConcurrentHashMap<>();

    @Override
    public void setUp() {
        world = server.getWorlds().get(0);

        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        String base = "carpe-diem-district-grave-yard";
        region = manager.getRegion(base);
        temple = manager.getRegion(base + "-temple");
        pressurePlateLockArea = manager.getRegion(base + "-temple-puzzle-one");
        creepers = manager.getRegion(base + "-creepers");
        parkour = manager.getRegion(base + "-parkour");
        rewards = manager.getRegion(base + "-temple-rewards");

        tick = 4 * 20;
        listener = new GraveYardListener(this);
        config = new GraveYardConfig();

        CommandBook.registerEvents(new FlightBlockingListener(admin, this::contains));

        findHeadStones();
        findPressurePlateLockLevers();
        findRewardChest();

        setupEconomy();

        spawnBlockBreakerTask();

        for (PlayerStateKind kind : GRAVE_YARD_SPECTATOR_KINDS) {
            spectator.registerSpectatedRegion(kind, region);
        }

        spectator.registerSpectatorSkull(
                PlayerStateKind.GRAVE_YARD_SPECTATOR,
                new Location(world, -169, 94, -686),
                () -> getTempleContained(Player.class).stream().anyMatch(this::isParticipant)
        );
        spectator.registerSpectatorSkull(
                PlayerStateKind.GRAVE_YARD_NORTH_SPECTATOR,
                new Location(world, -161, 82, -767),
                () -> !isEmpty()
        );
        spectator.registerSpectatorSkull(
                PlayerStateKind.GRAVE_YARD_SOUTH_SPECTATOR,
                new Location(world, -144, 82, -578),
                () -> !isEmpty()
        );
    }

    @Override
    public void enable() {
        for (PlayerStateKind kind : GRAVE_YARD_SPECTATOR_KINDS) {
            spectator.registerSpectatorKind(kind);
        }

        server.getScheduler().runTaskLater(inst, super::enable, 1);
    }

    private void handleEmptyRewardsRoom() {
        if (rewardsRoomOccupiedTicks == 0) {
            return;
        }

        rewardsRoomOccupiedTicks = 0;

        getContained(rewards, Projectile.class, Item.class, ExperienceOrb.class, Zombie.class).forEach(Entity::remove);

        resetPressurePlateLock();
        isPressurePlateLocked = !checkPressurePlateLock();
        resetRewardChest();
    }

    @Override
    public void run() {
        restoreBlocks();
        if (isEmpty()) {
            handleEmptyRewardsRoom();
            return;
        }

        boolean playerInRewardsRoom = false;

        for (LivingEntity entity : getContained(LivingEntity.class)) {
            if (!entity.isValid()) continue;

            // Cave Spider killer
            if (entity instanceof CaveSpider && entity.getLocation().getBlock().getLightFromSky() >= 10) {
                for (int i = 0; i < 20; ++i) getWorld().playEffect(entity.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                entity.remove();
                continue;
            }
        }

        for (Player player : getContainedParticipants()) {
            if (isEvilMode(player.getEyeLocation().getBlock())) {
                fogPlayer(player);
                localSpawn(player);

                if (!playerInRewardsRoom && contains(rewards, player)) {
                    playerInRewardsRoom = true;
                }
            }
        }

        if (playerInRewardsRoom) {
            ++rewardsRoomOccupiedTicks;
        } else {
            handleEmptyRewardsRoom();
        }
    }

    private void spawnBlockBreakerTask() {
        server.getScheduler().runTaskTimer(inst, () -> {
            if (cachedEmpty()) return;
            for (LivingEntity e : getContained(LivingEntity.class)) {
                // Auto break stuff
                Location belowLoc = e.getLocation().add(0, -1, 0);
                if (!(e instanceof Player) || (isHostileTempleArea(belowLoc) && isParticipant((Player) e))) {
                    breakBlock(e, belowLoc);
                    breakBlock(e, belowLoc.add(0, -1, 0));
                    breakBlock(e, belowLoc.add(0, -1, 0));
                }
            }
        }, 0, 5);
    }

    public <T extends Entity> Collection<T> getTempleContained(Class<T> clazz) {
        return world.getEntitiesByClass(clazz).stream()
                .filter(entity -> entity.isValid() && isHostileTempleArea(entity.getLocation()))
                .collect(Collectors.toList());
    }

    private boolean isEvilSurface() {
        return EnvironmentUtil.isNightTime(getWorld().getTime()) || getWorld().hasStorm();
    }

    private boolean isEvilMode(Block block) {
        // Weather/Day Check
        //noinspection SimplifiableIfStatement
        if (isEvilSurface()) return true;
        return isHostileTempleArea(block.getLocation()) || block.getLightLevel() == 0;
    }

    private boolean isInTemplateArea(Location location) {
        if (LocationUtil.isInRegion(getWorld(), temple, location)) {
            return true;
        }

        return location.getY() < 69 && contains(location);
    }

    public boolean isHostileTempleArea(Location location) {
        return isInTemplateArea(location) && location.getY() < 93 && location.getBlock().getLightFromSky() < 4;
    }

    public void restoreBlocks() {
        blockState.popBlocksOlderThan(BlockStateKind.GRAVEYARD, TimeUnit.SECONDS.toMillis(27));
    }

    private void localSpawn(Player player) {
        if (LocationUtil.isInRegion(getWorld(), rewards, player)) {
            // Redirect local spawns to be rewards room spawns
            Location spawnPoint = new Location(world, -130.5, 41, -685);

            for (int i = ChanceUtil.getRandom(rewardsRoomOccupiedTicks / 10); i > 0; --i) {
                Zombie zombie = spawn(spawnPoint, Zombie.class, "Guardian Zombie");
                zombie.setCanPickupItems(false);

                EntityEquipment equipment = zombie.getEquipment();
                equipment.setItemInHand(new ItemStack(Material.DIAMOND_SWORD));
                equipment.setArmorContents(new ItemStack[]{
                        CustomItemCenter.build(CustomItems.ANCIENT_BOOTS),
                        CustomItemCenter.build(CustomItems.ANCIENT_LEGGINGS),
                        CustomItemCenter.build(CustomItems.ANCIENT_CHESTPLATE),
                        CustomItemCenter.build(CustomItems.ANCIENT_HELMET)
                });

                // Drop Chances
                equipment.setItemInHandDropChance(0);
                equipment.setHelmetDropChance(0);
                equipment.setChestplateDropChance(0);
                equipment.setLeggingsDropChance(0);
                equipment.setBootsDropChance(0);
            }
            return;
        }

        if (!ChanceUtil.getChance(3)) return;

        Block playerBlock = player.getLocation().getBlock();
        for (int i = ChanceUtil.getRandom(16 - playerBlock.getLightLevel()); i > 0; --i) {
            Location ls = LocationUtil.findRandomLoc(playerBlock, 8, true, false);
            if (ls.getBlock().getType().isSolid()) {
                ls = player.getLocation();
            }

            spawnAndArm(ls, Zombie.class, true);
        }
    }

    protected  <T extends LivingEntity> T spawnAndArm(Location location, Class<T> type, boolean allowItemPickup) {
        if (!LocationUtil.isChunkLoadedAt(location)) {
            return null;
        }

        T e = spawn(location, type);
        if (e == null) return null;
        arm(e, allowItemPickup);
        return e;
    }

    private <T extends LivingEntity> T spawn(Location location, Class<T> type) {
        return spawn(location, type, "Grave Zombie");
    }

    private  <T extends LivingEntity> T spawn(Location location, Class<T> type, String name) {
        if (location == null) return null;
        T entity = location.getWorld().spawn(location, type, (e) -> e.getEquipment().clear());
        entity.setCustomName(name);
        entity.setCustomNameVisible(false);
        return entity;
    }

    private void arm(Entity e, boolean allowItemPickup) {
        // FIXME: This is 90% the same as the apocalypse arm logic

        if (!(e instanceof LivingEntity)) return;

        EntityEquipment equipment = ((LivingEntity) e).getEquipment();
        ((LivingEntity) e).setCanPickupItems(allowItemPickup);

        if (ChanceUtil.getChance(50)) {
            if (ChanceUtil.getChance(15)) {
                equipment.setArmorContents(ItemUtil.DIAMOND_ARMOR);
            } else {
                equipment.setArmorContents(ItemUtil.IRON_ARMOR);
            }

            if (ChanceUtil.getChance(4)) equipment.setHelmet(null);
            if (ChanceUtil.getChance(4)) equipment.setChestplate(null);
            if (ChanceUtil.getChance(4)) equipment.setLeggings(null);
            if (ChanceUtil.getChance(4)) equipment.setBoots(null);
        }

        if (ChanceUtil.getChance(50)) {
            ItemStack sword = new ItemStack(Material.IRON_SWORD);
            if (ChanceUtil.getChance(35)) sword = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta meta = sword.getItemMeta();
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_ALL, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_UNDEAD, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.FIRE_ASPECT, ChanceUtil.getRandom(2), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.KNOCKBACK, ChanceUtil.getRandom(2), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.LOOT_BONUS_MOBS, ChanceUtil.getRandom(3), false);
            sword.setItemMeta(meta);
            equipment.setItemInHand(sword);
        }

        equipment.setItemInHandDropChance(0.005F);
        equipment.setHelmetDropChance(0.005F);
        equipment.setChestplateDropChance(0.005F);
        equipment.setLeggingsDropChance(0.005F);
        equipment.setBootsDropChance(0.005F);
    }

    private static final DateFormat GRAVE_DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");

    private void labelGrave(Sign signState, String playerName) {
        signState.setLine(0, GRAVE_DATE_FORMAT.format(new Date()));
        signState.setLine(1, "RIP");
        signState.setLine(2, playerName);
        signState.update();
    }

    private boolean isGraveTooNew(Sign signState) {
        try {
            Date date = GRAVE_DATE_FORMAT.parse(signState.getLine(0));
            return System.currentTimeMillis() - date.getTime() < TimeUnit.DAYS.toMillis(3);
        } catch (ParseException ignored) {
            return false;
        }
    }

    private boolean isGraveOwnedByPlayer(Sign signState, Player player) {
        return signState.getLine(2).equalsIgnoreCase(player.getName());
    }

    private Chest findChestForHeadstone(Sign signState) {
        Location chestTrialLoc = signState.getLocation();

        // Try the block 2 blocks down
        chestTrialLoc.add(0, -2, 0);
        BlockState chestState = chestTrialLoc.getBlock().getState();
        if (chestState instanceof Chest) {
            return (Chest) chestState;
        }

        // Try the block 3 blocks down
        chestTrialLoc.add(0, -1, 0);
        chestState = chestTrialLoc.getBlock().getState();
        if (chestState instanceof Chest) {
            return (Chest) chestState;
        }

        // Try again, but try using the wall sign information
        if (signState.getBlockData() instanceof WallSign) {
            WallSign sign = (WallSign) signState.getBlockData();
            BlockFace attachedFace = sign.getFacing().getOppositeFace();

            // Try the block 2 blocks back
            chestTrialLoc = signState.getLocation();
            chestTrialLoc.add(attachedFace.getDirection().multiply(2));
            chestState = chestTrialLoc.getBlock().getState();
            if (chestState instanceof Chest) {
                return (Chest) chestState;
            }

            // Try the block 2 blocks back, 1 block down
            chestTrialLoc.add(0, -1, 0);
            chestState = chestTrialLoc.getBlock().getState();
            if (chestState instanceof Chest) {
                return (Chest) chestState;
            }
        }

        return null;
    }

    private static final BlockFace[] CHECK_ORDER = new BlockFace[] {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private void forGraveChest(Chest chest, Consumer<Chest> chestConsumer) {
        boolean isFoundLeft = ((org.bukkit.block.data.type.Chest) chest.getBlockData()).getType() == Type.LEFT;

        Chest left = isFoundLeft ? chest : null;
        Chest right = isFoundLeft ? null : chest;

        for (BlockFace face : CHECK_ORDER) {
            BlockState nearbyState = chest.getBlock().getRelative(face).getState();
            if (nearbyState instanceof Chest) {
                if (isFoundLeft) {
                    right = (Chest) nearbyState;
                } else {
                    left = (Chest) nearbyState;
                }

                break;
            }
        }

        // This order ensures that items are added "top" to "bottom" in the chest
        if (right != null) {
            chestConsumer.accept(right);
        }
        if (left != null) {
            chestConsumer.accept(left);
        }
    }

    private void forGraveInventory(Chest chest, Consumer<Inventory> inventoryConsumer) {
        forGraveChest(chest, (foundChest) -> inventoryConsumer.accept(foundChest.getInventory()));
    }

    private Sign findHeadstoneForChestFromChest(Chest chestState) {
        Location signTrialLoc = chestState.getLocation();

        // Try the block 2 blocks up
        signTrialLoc.add(0, 2, 0);
        BlockState signState = signTrialLoc.getBlock().getState();
        if (signState instanceof Sign) {
            return (Sign) signState;
        }

        // Try the block 3 blocks up
        signTrialLoc.add(0, 1, 0);
        signState = signTrialLoc.getBlock().getState();
        if (signState instanceof Sign) {
            return (Sign) signState;
        }

        // Prod for a nearby chest
        for (BlockFace checkDir : CHECK_ORDER) {
            signTrialLoc = chestState.getLocation();

            // Try the block 2 blocks in this direction
            signTrialLoc.add(checkDir.getDirection().multiply(2));
            signState = signTrialLoc.getBlock().getState();
            if (signState.getBlockData() instanceof WallSign) {
                return (Sign) signState;
            }

            // Try the block 2 blocks in this direction, 1 block up
            signTrialLoc.add(0, 1, 0);
            signState = signTrialLoc.getBlock().getState();
            if (signState.getBlockData() instanceof WallSign) {
                return (Sign) signState;
            }
        }

        return null;
    }

    private Sign findHeadstoneForChest(Chest chest) {
        Sign[] sign = new Sign[1];
        forGraveChest(chest, (foundChest) -> {
            if (sign[0] != null) {
                return;
            }

            sign[0] = findHeadstoneForChestFromChest(foundChest);
        });
        return sign[0];
    }

    protected boolean isGraveOpenableBy(Chest chest, Player player) {
        // Any temple chest are openable
        if (isHostileTempleArea(chest.getLocation())) {
            return true;
        }

        Sign headStone = findHeadstoneForChest(chest);
        if (headStone == null) {
            // No headstone found, assume yes
            return true;
        }

        return !isGraveTooNew(headStone) || isGraveOwnedByPlayer(headStone, player);
    }

    private void compactAndClearGrave(Chest chest, ArrayDeque<ItemStack> itemStacks) {
        forGraveInventory(chest, (inv) -> {
            List<ItemStack> remainders = new ArrayList<>();

            // Try and compact everything, by adding the current items to the existing
            // items.
            while (!itemStacks.isEmpty()) {
                ItemStack remainder = inv.addItem(itemStacks.poll()).get(0);
                if (remainder == null) {
                    continue;
                }

                remainders.add(remainder);
            }

            // Add any remainders back into the ItemStack queue
            itemStacks.addAll(remainders);

            // Add all the ItemStacks back into the ItemStack queue
            inv.forEach((item) -> {
                if (item == null) {
                    return;
                }

                itemStacks.add(item);
            });

            // Clear the chest
            inv.clear();
        });
    }

    private void prioritizeGraveLoot(ArrayDeque<ItemStack> itemStacks) {
        List<ItemStack> sortedItems = new ArrayList<>(itemStacks);

        MarketItemLookupInstance lookupInstance = MarketComponent.getLookupInstanceFromStacksImmediately(sortedItems);
        sortedItems.sort((o1, o2) -> {
            double o1SellPrice = lookupInstance.checkMaximumValue(o1).orElse(0d);
            double o2SellPrice = lookupInstance.checkMaximumValue(o2).orElse(0d);
            return (int) (o2SellPrice - o1SellPrice);
        });

        itemStacks.clear();
        itemStacks.addAll(sortedItems);
    }

    private void fillGrave(Chest chest, ArrayDeque<ItemStack> itemStacks) {
        forGraveInventory(chest, (inv) -> {
            while (!itemStacks.isEmpty()) {
                ItemStack remainder = inv.addItem(itemStacks.poll()).get(0);
                if (remainder != null) {
                    itemStacks.addFirst(remainder);
                    break;
                }
            }
        });
    }

    private void updateGrave(Chest chest, ArrayDeque<ItemStack> itemStacks) {
        compactAndClearGrave(chest, itemStacks);
        prioritizeGraveLoot(itemStacks);
        fillGrave(chest, itemStacks);
    }

    private boolean makeGrave(String playerName, ArrayDeque<ItemStack> itemStacks,
                              Location location, boolean dateChecking) {
        BlockState blockState = location.getBlock().getState();

        // Check sign validity
        if (!(blockState instanceof Sign)) {
            log.warning("Valid sign not found at: "
                    + blockState.getX() + ", " + blockState.getY() + ", " + blockState.getZ());
            return false;
        }

        // Check to see if this grave was made too recently
        Sign signState = (Sign) blockState;
        if (dateChecking && isGraveTooNew(signState)) {
            return false;
        }

        // Find a chest to put items into
        Chest chest = findChestForHeadstone(signState);
        if (chest == null) {
            log.warning("Valid grave not found for sign: "
                    + blockState.getX() + ", " + blockState.getY() + ", " + blockState.getZ());
            return false;
        }

        // Mark and fill the gravestone
        labelGrave(signState, playerName);
        updateGrave(chest, itemStacks);

        log.info("Made a Grave for: " + playerName + " at: "
                + blockState.getX() + ", " + blockState.getY() + ", " + blockState.getZ());

        return true;
    }

    private Location makeGrave(String playerName, ArrayDeque<ItemStack> itemStacks, boolean dateChecking) {
        return CollectionUtil.randomIterateFor(headStones, (headStone) -> {
            return makeGrave(playerName, itemStacks, headStone, dateChecking);
        });
    }

    private void dropOverflow(String playerName, Location location, ArrayDeque<ItemStack> itemStacks) {
        MarketItemLookupInstance lookupInstance = MarketComponent.getLookupInstanceFromStacksImmediately(itemStacks);

        for (ItemStack stack : itemStacks) {
            double itemValue = lookupInstance.checkMaximumValue(stack).orElse(100d);
            boolean destroy = itemValue < 100;

            String itemName = ItemNameCalculator.computeItemName(stack).orElse("UNKNOWN ITEM");
            String itemDescription = " (" + itemName + " x" + stack.getAmount() + (destroy ? " -- destroyed" : "") + ')';

            log.warning("Failed to create complete grave for " + playerName + itemDescription + '.');

            if (destroy) {
                continue;
            }

            location.getWorld().dropItem(location, stack);
        }

    }

    public Location makeGrave(String playerName, ItemStack[] itemStacks) {
        ArrayDeque<ItemStack> itemQueue = new ArrayDeque<>(Arrays.asList(itemStacks));

        // Prioritize finding an old grave
        Location graveLocation = makeGrave(playerName, itemQueue, true);
        if (graveLocation == null) {
            // No old grave existed, use a recent one
            graveLocation = makeGrave(playerName, itemQueue, false);
        }

        // Drop items that couldn't be placed into a grave
        dropOverflow(playerName, graveLocation, itemQueue);

        return graveLocation;
    }

    private void findHeadStones() {
        headStones.clear();
        final List<BlockVector2> chunkList = new ArrayList<>();
        BlockVector3 min = getRegion().getMinimumPoint();
        BlockVector3 max = getRegion().getMaximumPoint();
        final int minX = min.getBlockX();
        final int minZ = min.getBlockZ();
        final int maxX = max.getBlockX();
        final int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; x += 16) {
            for (int z = minZ; z <= maxZ; z += 16) {
                BlockVector2 chunkCoords = BlockVector2.at(x >> 4, z >> 4);
                if (!chunkList.contains(chunkCoords)) chunkList.add(chunkCoords);
            }
        }

        for (int i = 0; i < chunkList.size(); ++i) {
            BlockVector2 chunkCoords = chunkList.get(i);

            try {
                server.getScheduler().runTaskLater(inst, () -> {
                    for (BlockState aSign : world.getChunkAt(chunkCoords.getX(), chunkCoords.getZ()).getTileEntities()) {
                        if (!(aSign instanceof Sign)) continue;
                        checkHeadStone((Sign) aSign);
                    }
                }, i);
            } catch (NullPointerException ex) {
                log.warning("Failed to get head stones for Chunk: " + chunkCoords.getX() + ", " + chunkCoords.getZ() + ".");
                return;
            }
        }
    }

    private boolean checkHeadStone(Sign sign) {
        Location l = sign.getLocation();
        if ((l.getBlockY() != 81 && l.getBlockY() != 82) || !contains(l)) return false;
        headStones.add(sign.getLocation());
        return true;
    }

    private void findPressurePlateLockLevers() {
        RegionWalker.walk(pressurePlateLockArea, (x, y, z) -> {
            BlockState block = getWorld().getBlockAt(x, y, z).getState();
            if (block.getType() == Material.LEVER) {
                Lever lever = (Lever) block.getData();
                lever.setPowered(false);
                block.setData(lever);
                block.update(true);
                pressurePlateLocks.put(block.getLocation(), !ChanceUtil.getChance(3));
            }
        });
    }

    public boolean checkPressurePlateLock() {
        for (Map.Entry<Location, Boolean> lever : pressurePlateLocks.entrySet()) {
            Lever aLever = (Lever) lever.getKey().getBlock().getState().getData();
            if (aLever.isPowered() != lever.getValue()) return false;
        }
        ChatUtil.sendNotice(getTempleContained(Player.class), "You hear a clicking sound.");
        return true;
    }

    protected void resetPressurePlateLock() {
        for (Location entry : pressurePlateLocks.keySet()) {
            BlockState state = entry.getBlock().getState();
            Lever lever = (Lever) state.getData();
            lever.setPowered(false);
            state.setData(lever);
            state.update(true);
            pressurePlateLocks.put(entry, !ChanceUtil.getChance(3));
        }
    }

    protected void resetRewardChest() {
        for (Location location : rewardChest) {
            Chest chest = (Chest) location.getBlock().getState();

            Inventory chestInv = chest.getBlockInventory();
            chestInv.clear();

            int length = chestInv.getContents().length;
            for (int i = 0; i < length * .6; ++i) {
                int target = ChanceUtil.getRandom(length) - 1;
                if (chestInv.getContents()[target] != null) {
                    i--;
                    continue;
                }
                chestInv.setItem(target, pickRandomItem());
            }
        }
    }

    private void findRewardChest() {
        RegionWalker.walk(rewards, (x, y, z) -> {
            BlockState block = getWorld().getBlockAt(x, y, z).getState();
            if (block.getType() == Material.CHEST) {
                rewardChest.add(block.getLocation());
            }
        });
        resetRewardChest();
    }

    private ItemStack pickRandomItem() {
        switch (ChanceUtil.getRandom(39)) {
            case 1:
                return CustomItemCenter.build(CustomItems.GEM_OF_LIFE, 6);
            case 3:
                if (!ChanceUtil.getChance(200)) return null;
                if (ChanceUtil.getChance(2)) {
                    return CustomItemCenter.build(CustomItems.FEAR_SWORD);
                } else {
                    return CustomItemCenter.build(CustomItems.FEAR_SHORT_SWORD);
                }
            case 4:
                if (!ChanceUtil.getChance(200)) return null;
                return CustomItemCenter.build(CustomItems.FEAR_BOW);
            case 5:
                if (!ChanceUtil.getChance(200)) return null;
                if (ChanceUtil.getChance(2)) {
                    return CustomItemCenter.build(CustomItems.UNLEASHED_SWORD);
                } else {
                    return CustomItemCenter.build(CustomItems.UNLEASHED_SHORT_SWORD);
                }
            case 6:
                if (!ChanceUtil.getChance(200)) return null;
                return CustomItemCenter.build(CustomItems.UNLEASHED_BOW);
            case 7:
                return CustomItemCenter.build(CustomItems.IMBUED_CRYSTAL, ChanceUtil.getRandom(3));
            case 8:
                return CustomItemCenter.build(CustomItems.GEM_OF_DARKNESS, ChanceUtil.getRandom(3));
            case 9:
                return CustomItemCenter.build(CustomItems.BAT_BOW);
            case 10:
                return CustomItemCenter.build(CustomItems.PHANTOM_GOLD, ChanceUtil.getRandom(64));
            case 11:
                if (ChanceUtil.getChance(500)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_HELMET);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_HELMET);
                }
            case 12:
                if (ChanceUtil.getChance(500)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_CHESTPLATE);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_CHESTPLATE);
                }
            case 13:
                if (ChanceUtil.getChance(500)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_LEGGINGS);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_LEGGINGS);
                }
            case 14:
                if (ChanceUtil.getChance(500)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_BOOTS);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_BOOTS);
                }
            case 15:
                return CustomItemCenter.build(CustomItems.GOD_HELMET);
            case 16:
                return CustomItemCenter.build(CustomItems.GOD_CHESTPLATE);
            case 17:
                return CustomItemCenter.build(CustomItems.GOD_LEGGINGS);
            case 18:
                return CustomItemCenter.build(CustomItems.GOD_BOOTS);
            case 19:
                return CustomItemCenter.build(CustomItems.GOD_PICKAXE);
            case 20:
                return CustomItemCenter.build(CustomItems.LEGENDARY_GOD_PICKAXE);
            case 21:
                return new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandom(64));
            case 22:
                return new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(64));
            case 23:
                return new ItemStack(Material.EMERALD, ChanceUtil.getRandom(64));
            case 24:
                return CustomItemCenter.build(CustomItems.PHANTOM_POTION);
            case 25:
                return CustomItemCenter.build(CustomItems.PHANTOM_ESSENCE, ChanceUtil.getRandom(16));
            case 26:
                return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, ChanceUtil.getRandom(32));
            case 28:
                return new ItemStack(Material.SADDLE);
            case 29:
                return new ItemStack(Material.IRON_HORSE_ARMOR);
            case 30:
                return new ItemStack(Material.GOLDEN_HORSE_ARMOR);
            case 31:
                return new ItemStack(Material.DIAMOND_HORSE_ARMOR);
            default:
                return CustomItemCenter.build(CustomItems.BARBARIAN_BONE, ChanceUtil.getRandom(5));
        }
    }

    private void breakBlock(Entity e, Location location) {
        int chance = e instanceof Player ? 2 : e instanceof CaveSpider ? 30 : 6;
        Block block = location.getBlock();

        Material blockType = block.getType();
        if (blockType == Material.AIR) return;

        BlockFace[] targets;
        if (blockType.equals(Material.CRACKED_STONE_BRICKS)) {
            targets = new BlockFace[] {BlockFace.SELF};
        } else {
            targets = EnvironmentUtil.getNearbyBlockFaces();
        }

        for (BlockFace face : targets) {
            if (!ChanceUtil.getChance(chance)) continue;
            final Block aBlock = block.getRelative(face);
            Block bBlock = aBlock.getRelative(BlockFace.DOWN);
            if (bBlock.getType().isSolid()) continue;

            int delay = 20;
            if (aBlock.getType().equals(Material.CRACKED_STONE_BRICKS)) {
                delay *= .75;
            }

            server.getScheduler().runTaskLater(inst, () -> {
                if (!AUTO_BREAKABLE.contains(aBlock.getType())) {
                    return;
                }

                try {
                    blockState.pushAnonymousBlock(BlockStateKind.GRAVEYARD, aBlock.getState());
                    aBlock.setType(Material.AIR);
                } catch (UnstorableBlockStateException ex) {
                    ex.printStackTrace();
                }
            }, delay);
        }
    }

    private void fogPlayer(Player player) {
        if (ItemUtil.isItem(player.getInventory().getHelmet(), CustomItems.ANCIENT_CROWN)
                || ItemUtil.hasItem(player, CustomItems.GEM_OF_DARKNESS)) {
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 6, 1), true);
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }
}
