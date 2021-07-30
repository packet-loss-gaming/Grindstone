/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.FreakyFour;

import com.destroystokyo.paper.Title;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.checker.Expression;
import gg.packetloss.grindstone.util.checker.NonSolidRegionChecker;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.Optional;
import java.util.function.Predicate;

@ComponentInformation(friendlyName = "Freaky Four", desc = "The craziest bosses ever")
@Depend(components = {
        AdminComponent.class, PlayerStateComponent.class, SpectatorComponent.class, WalletComponent.class,
        HighScoresComponent.class},
        plugins = {"WorldGuard"})
public class FreakyFourArea extends AreaComponent<FreakyFourConfig> {

    protected static final int GROUND_LEVEL = 79;

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected PlayerStateComponent playerState;
    @InjectComponent
    protected SpectatorComponent spectator;
    @InjectComponent
    protected WalletComponent wallet;
    @InjectComponent
    protected HighScoresComponent highScores;

    protected ProtectedRegion heads;

    protected EnumMap<FreakyFourBoss, Monster> bossEntities = new EnumMap<>(FreakyFourBoss.class);
    protected EnumMap<FreakyFourBoss, ProtectedRegion> bossRegions = new EnumMap<>(FreakyFourBoss.class);
    protected EnumMap<FreakyFourBoss, BossBar> bossBars = new EnumMap<>(FreakyFourBoss.class);

    protected Location entrance;
    protected long lastSpiderBite = 0;

    @Override
    public void setUp() {
        spectator.registerSpectatorKind(PlayerStateKind.FREAKY_FOUR_SPECTATOR);

        world = server.getWorlds().get(0);
        entrance = new Location(world, 401.5, GROUND_LEVEL, -304, 270, 0);
        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        String base = "oblitus-district-freaky-four";
        region = manager.getRegion(base);
        bossRegions.put(FreakyFourBoss.CHARLOTTE, manager.getRegion(base + "-charlotte"));
        bossRegions.put(FreakyFourBoss.FRIMUS, manager.getRegion(base + "-frimus"));
        bossRegions.put(FreakyFourBoss.DA_BOMB, manager.getRegion(base + "-da-bomb"));
        bossRegions.put(FreakyFourBoss.SNIPEE, manager.getRegion(base + "-snipee"));
        heads = manager.getRegion(base + "-heads");
        tick = 4 * 20;
        listener = new FreakyFourListener(this);
        config = new FreakyFourConfig();

        for (FreakyFourBoss boss : FreakyFourBoss.values()) {
            bossBars.put(boss, Bukkit.createBossBar(boss.getProperName(), BarColor.RED, BarStyle.SEGMENTED_6));
        }

        CommandBook.registerEvents(new FlightBlockingListener(admin, this::contains));

        spectator.registerSpectatedRegion(PlayerStateKind.FREAKY_FOUR_SPECTATOR, region);
        spectator.registerSpectatorSkull(
                PlayerStateKind.FREAKY_FOUR_SPECTATOR,
                new Location(world, 400, GROUND_LEVEL, -307),
                () -> getFirstFullBossRoom().isPresent()
        );
    }

    @Override
    public void run() {
        if (!isEmpty()) {
            removeFireResistance();
            if (checkActiveBoss(FreakyFourBoss.CHARLOTTE)) {
                runCharlotte();
            }
            if (checkActiveBoss(FreakyFourBoss.FRIMUS)) {
                runFrimus();
            }
            if (!isBossRoomEmpty(FreakyFourBoss.SNIPEE) && !isBossAlive(FreakyFourBoss.SNIPEE)) {
                runLootTimeout();
            }
            updateBossBars();
        }
    }

    private void removeFireResistance() {
        for (Player player : getContainedParticipants()) {
            player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        }
    }

    public boolean isBossRoomEmpty(FreakyFourBoss boss) {
        return getContainedParticipantsIn(bossRegions.get(boss)).isEmpty();
    }

    public boolean isBossAlive(FreakyFourBoss boss) {
        Monster bossEnt = bossEntities.get(boss);
        return bossEnt != null && bossEnt.isValid();
    }

    public boolean checkActiveBoss(FreakyFourBoss boss) {
        return isBossAlive(boss) && !isBossRoomEmpty(boss);
    }

    protected void updateBossBarProgress(FreakyFourBoss boss) {
        Monster bossEnt = bossEntities.get(boss);
        BossBar bossBar = bossBars.get(boss);
        if (bossEnt == null) {
            bossBar.setProgress(0);
        } else {
            bossBar.setProgress(bossEnt.getHealth() / bossEnt.getMaxHealth());
        }
    }

    protected void updateBossBar(FreakyFourBoss boss) {
        BossBar bossBar = bossBars.get(boss);
        if (checkActiveBoss(boss)) {
            BossBarUtil.syncWithPlayers(bossBar, getAudiblePlayersIn(bossRegions.get(boss)));
            updateBossBarProgress(boss);
        } else {
            bossBar.removeAll();
        }
    }

    protected void updateBossBars() {
        for (FreakyFourBoss boss : FreakyFourBoss.values()) {
            updateBossBar(boss);
        }
    }

    protected void announceKill(FreakyFourBoss boss) {
        for (Player player : getAudiblePlayersIn(bossRegions.get(boss))) {
            player.sendTitle(Title.builder().title(
                Text.of(ChatColor.DARK_GREEN, boss.getProperName()).build()
            ).subtitle(
                Text.of(ChatColor.DARK_GREEN, "SLAIN").build()
            ).fadeIn(10).stay(20).fadeOut(10).build());
        }
    }

    public Location getEntrance(FreakyFourBoss boss) {
        switch (boss) {
            case CHARLOTTE -> {
                return new Location(world, 398.5, GROUND_LEVEL, -304, 90, 0);
            }
            case FRIMUS -> {
                return new Location(world, 374.5, GROUND_LEVEL, -304, 90, 0);
            }
            case DA_BOMB -> {
                return new Location(world, 350.5, GROUND_LEVEL, -304, 90, 0);
            }
            case SNIPEE -> {
                return new Location(world, 326.5, GROUND_LEVEL, -304, 90, 0);
            }
            default -> throw new IllegalStateException();
        }
    }

    public void addSkull(Player player) {
        Location v = LocationUtil.pickLocation(world, heads);
        SkullPlacer.placePlayerSkullOnWall(v, BlockFace.WEST, player);
    }

    protected Location getBossSpawnLoc(FreakyFourBoss boss) {
        return RegionUtil.getCenterAt(world, GROUND_LEVEL, bossRegions.get(boss));
    }

    private void createWall(ProtectedRegion region,
                            Expression<Block, Boolean> oldExpr,
                            Expression<Block, Boolean> newExpr,
                            Material oldType, Material newType,
                            int density, int floodFloor) {

        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        boolean[] floodMatrix = new boolean[(maxX - minX) + 1];
        for (int i = 0; i < floodMatrix.length; ++i) {
            floodMatrix[i] = ChanceUtil.getChance(density);
        }

        int initialTimes = maxZ - minZ + 1;
        IntegratedRunnable integratedRunnable = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                int startZ = minZ + (initialTimes - times) - 1;

                for (int x = minX; x <= maxX; ++x) {
                    for (int z = startZ; z < Math.min(maxZ, startZ + 4); ++z) {
                        boolean flood = floodMatrix[maxX - x];
                        for (int y = minY; y <= maxY; ++y) {
                            Block block = world.getBlockAt(x, y, z);
                            if (z == startZ && newExpr.evaluate(block)) {
                                block.setType(oldType);
                            } else if (flood && oldExpr.evaluate(block)) {
                                block.setType(newType);
                            }
                        }
                    }
                }
                return true;
            }

            @Override
            public void end() {
                if (floodFloor != -1) {
                    for (int x = minX; x <= maxX; ++x) {
                        for (int z = minZ; z <= maxZ; ++z) {
                            if (!ChanceUtil.getChance(floodFloor)) continue;
                            Block block = world.getBlockAt(x, minY, z);
                            if (oldExpr.evaluate(block)) {
                                block.setType(newType);
                            }
                        }
                    }
                }
            }
        };
        TimedRunnable timedRunnable = new TimedRunnable(integratedRunnable, initialTimes);
        BukkitTask task = server.getScheduler().runTaskTimer(inst, timedRunnable, 0, 5);
        timedRunnable.setTask(task);
    }

    protected FreakyFourBoss getBossAtLocation(Location location) {
        for (FreakyFourBoss boss : FreakyFourBoss.values()) {
            ProtectedRegion bossRegion = bossRegions.get(boss);
            if (contains(bossRegion, location)) {
                return boss;
            }
        }

        return null;
    }

    public double getConfiguredMaxHealth(FreakyFourBoss boss) {
        switch (boss) {
            case CHARLOTTE -> {
                return config.charlotteHP;
            }
            case FRIMUS -> {
                return config.frimusHP;
            }
            case DA_BOMB -> {
                return config.daBombHP;
            }
            case SNIPEE -> {
                return config.snipeeHP;
            }
            default -> throw new IllegalStateException();
        }
    }

    public void spawn(FreakyFourBoss boss) {
        Monster bossEnt = getWorld().spawn(
            getBossSpawnLoc(boss),
            boss.getEntityClass(),
            (e) -> e.getEquipment().clear()
        );

        // Handle equipment
        if (boss == FreakyFourBoss.SNIPEE) {
            bossEnt.getEquipment().setItem(EquipmentSlot.HAND, new ItemStack(Material.BOW));
        }

        // Handle vitals
        double configuredHealth = getConfiguredMaxHealth(boss);
        bossEnt.setMaxHealth(configuredHealth);
        bossEnt.setHealth(configuredHealth);
        bossEnt.setRemoveWhenFarAway(false);

        // Handle attributes
        try {
            AttributeBook.setAttribute(bossEnt, AttributeBook.Attribute.FOLLOW_RANGE, 50);
            if (boss == FreakyFourBoss.SNIPEE) {
                AttributeBook.setAttribute(bossEnt, AttributeBook.Attribute.MOVEMENT_SPEED, 0.15);
            } else if (boss == FreakyFourBoss.DA_BOMB) {
                AttributeBook.setAttribute(bossEnt, AttributeBook.Attribute.MOVEMENT_SPEED, 0.6);

            }
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }

        // Handle name
        bossEnt.setCustomName(boss.getProperName());

        // Register
        bossEntities.put(boss, bossEnt);
    }

    public void cleanSpawn(FreakyFourBoss boss) {
        switch (boss) {
            case CHARLOTTE -> {
                cleanupCharlotte();
            }
            case FRIMUS -> {
                cleanupFrimus();
            }
            case DA_BOMB -> {
                cleanupDaBomb();
            }
            case SNIPEE -> {
                cleanupSnipee();
            }
            default -> throw new IllegalStateException();
        }
        spawn(boss);
    }

    public void cleanupCharlotte() {
        ProtectedRegion charlotteRegion = bossRegions.get(FreakyFourBoss.CHARLOTTE);
        RegionWalker.walk(charlotteRegion, (x, y, z) -> {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.COBWEB) {
                block.setType(Material.AIR);
            }
        });

        for (Entity spider : getContained(charlotteRegion, Spider.class, CaveSpider.class)) {
            spider.remove();
        }
    }

    protected Location getLocationInBossRoom(FreakyFourBoss boss, Predicate<BlockVector3> extraCheck) {
        return LocationUtil.pickLocation(
            world,
            GROUND_LEVEL,
            new NonSolidRegionChecker(bossRegions.get(boss), world) {
                @Override
                public Boolean evaluate(BlockVector3 vector) {
                    if (!super.evaluate(vector)) {
                        return false;
                    }

                    return extraCheck.test(vector);
                }
            }
        );
    }

    private void spawnCharlotteMinion(Location location) {
        CaveSpider spider = world.spawn(location, CaveSpider.class);

        try {
            AttributeBook.setAttribute(spider, AttributeBook.Attribute.FOLLOW_RANGE, 50);
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }

        Monster charlotte = bossEntities.get(FreakyFourBoss.CHARLOTTE);
        if (charlotte != null) {
            spider.setTarget(charlotte.getTarget());
        }
    }

    public void runCharlotte() {
        ProtectedRegion charlotteRegion = bossRegions.get(FreakyFourBoss.CHARLOTTE);

        for (int i = ChanceUtil.getRandom(10); i > 0; --i) {
            spawnCharlotteMinion(getLocationInBossRoom(FreakyFourBoss.CHARLOTTE, (vec) -> true));
        }

        ChanceUtil.doRandom(
            () -> {
                for (CaveSpider caveSpider : getContained(charlotteRegion, CaveSpider.class)) {
                    Block block = caveSpider.getLocation().getBlock();
                    while (block.getType() != Material.AIR) {
                        if (block.getType() != Material.COBWEB) {
                            break;
                        }

                        block = block.getRelative(BlockFace.UP);
                    }
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.COBWEB);
                    }
                }
            },
            () -> {
                RegionWalker.walk(charlotteRegion, (x, y, z) -> {
                    if (!ChanceUtil.getChance(config.charlotteWebBreak)) return;

                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.COBWEB) {
                        block.setType(Material.AIR);
                        if (ChanceUtil.getChance(config.charlotteWebSpider)) {
                            spawnCharlotteMinion(block.getLocation());
                        }
                    }
                });
            }
        );
    }

    public void cleanupFrimus() {
        ProtectedRegion frimusRegion = bossRegions.get(FreakyFourBoss.FRIMUS);
        RegionWalker.walk(frimusRegion, (x, y, z) -> {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.FIRE || EnvironmentUtil.isLava(block.getType())) {
                block.setType(Material.AIR);
            }
        });

        for (Entity blaze : getContained(frimusRegion, Blaze.class)) {
            blaze.remove();
        }
    }

    private void runFrimus() {
        createWall(bossRegions.get(FreakyFourBoss.FRIMUS),
                input -> input.getType() == Material.AIR,
                EnvironmentUtil::isLava,
                Material.AIR,
                Material.LAVA,
                config.frimusWallDensity,
                -1
        );
    }

    public void cleanupDaBomb() {
        for (Creeper creeper : getContained(bossRegions.get(FreakyFourBoss.DA_BOMB), Creeper.class)) {
            creeper.remove();
        }
    }

    private boolean createSolidLootLavaWall(ProtectedRegion snipeeRegion, int z) {
        final BlockVector3 min = snipeeRegion.getMinimumPoint();
        final BlockVector3 max = snipeeRegion.getMaximumPoint();

        // Adjust to remove the walls
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int midX = minX + ((maxX - minX) / 2);
        int midY = minY + ((maxY - minY) / 2);

        // Check to make sure this row isn't already filled
        if (!world.getBlockAt(midX, midY, z).getType().isAir()) {
            return false;
        }

        // Try to fill the row
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                Block block = world.getBlockAt(x, y, z);
                if (!block.getType().isAir()) {
                    continue;
                }

                block.setType(Material.LAVA);
            }
        }

        return true;
    }

    private void runLootTimeout() {
        ProtectedRegion snipeeRegion = bossRegions.get(FreakyFourBoss.SNIPEE);

        final BlockVector3 min = snipeeRegion.getMinimumPoint();
        final BlockVector3 max = snipeeRegion.getMaximumPoint();

        int minZ = min.getBlockZ();
        int maxZ = max.getBlockZ();

        // Close in from the left
        for (int z = minZ; z < maxZ; ++z) {
            if (createSolidLootLavaWall(snipeeRegion, z)) {
                break;
            }
        }

        // Close in from the right
        for (int z = maxZ; z > minZ; --z) {
            if (createSolidLootLavaWall(snipeeRegion, z)) {
                break;
            }
        }
    }

    private Block[] getLootChestBlocks() {
        return new Block[] {
            world.getBlockAt(308, GROUND_LEVEL, -305),
            world.getBlockAt(308, GROUND_LEVEL, -304)
        };
    }

    private void setLootChestPresent(boolean present) {
        if (present) {
            Block[] blocks = getLootChestBlocks();

            // Setup the left half of the chest
            var left = (org.bukkit.block.data.type.Chest) Material.CHEST.createBlockData();
            left.setType(org.bukkit.block.data.type.Chest.Type.LEFT);
            left.setFacing(BlockFace.EAST);
            blocks[0].setBlockData(left);

            // Setup the right half of the chest
            var right = (org.bukkit.block.data.type.Chest) Material.CHEST.createBlockData();
            right.setType(org.bukkit.block.data.type.Chest.Type.RIGHT);
            right.setFacing(BlockFace.EAST);
            blocks[1].setBlockData(right);
        } else {
            for (Block block : getLootChestBlocks()) {
                block.setType(Material.AIR);
            }
        }
    }

    public void cleanupSnipee() {
        setLootChestPresent(false);

        ProtectedRegion snipeeRegion = bossRegions.get(FreakyFourBoss.SNIPEE);
        RegionWalker.walk(snipeeRegion, (x, y, z) -> {
            Block block = world.getBlockAt(x, y, z);
            if (EnvironmentUtil.isLava(block.getType())) {
                block.setType(Material.AIR);
            }
        });

        for (Entity entity : getContained(bossRegions.get(FreakyFourBoss.SNIPEE), Skeleton.class, Item.class)) {
            entity.remove();
        }
    }

    protected void generateLootChest() {
        setLootChestPresent(true);

        for (Block block : getLootChestBlocks()) {
            Chest chestState = (Chest) block.getState();
            Inventory inventory = chestState.getBlockInventory();
            for (int i = 0; i < inventory.getContents().length; ++i) {
                inventory.setItem(i, pickRandomItem());
            }
        }
    }

    private ItemStack pickRandomItem() {
        if (!ChanceUtil.getChance(4)) {
            return new ItemStack(Material.EXPERIENCE_BOTTLE, ChanceUtil.getRandom(64));
        }

        return ChanceUtil.supplyRandom(
            () -> CustomItemCenter.build(CustomItems.PHANTOM_HYMN),
            () -> CustomItemCenter.build(CustomItems.PHANTOM_POTION),
            () -> CustomItemCenter.build(CustomItems.PHANTOM_ESSENCE, ChanceUtil.getRandom(3)),
            () -> CustomItemCenter.build(CustomItems.PHANTOM_GOLD, ChanceUtil.getRandom(64)),
            () -> {
                if (ChanceUtil.getChance(250)) {
                    return CustomItemCenter.build(CustomItems.PHANTOM_SABRE);
                } else {
                    return CustomItemCenter.build(CustomItems.GOD_FISH);
                }
            },
            () -> {
                if (ChanceUtil.getChance(75)) {
                    return CustomItemCenter.build(CustomItems.PHANTOM_LINK);
                } else {
                    return ChanceUtil.supplyRandom(
                        () -> CustomItemCenter.build(CustomItems.ROGUE_OATH, ChanceUtil.getRandom(5)),
                        () -> CustomItemCenter.build(CustomItems.NINJA_OATH, ChanceUtil.getRandom(5))
                    );
                }
            },
            () -> {
                if (ChanceUtil.getChance(75)) {
                    if (ChanceUtil.getChance(2)) {
                        return CustomItemCenter.build(CustomItems.PHANTOM_CLOCK);
                    } else {
                        return CustomItemCenter.build(CustomItems.PHANTOM_SABRE);
                    }
                } else {
                    return CustomItemCenter.build(CustomItems.PHANTOM_DIAMOND);
                }
            },
            () -> {
                if (ChanceUtil.getChance(75)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_HELMET);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_HELMET);
                }
            },
            () -> {
                if (ChanceUtil.getChance(75)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_CHESTPLATE);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_CHESTPLATE);
                }
            },
            () -> {
                if (ChanceUtil.getChance(75)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_LEGGINGS);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_LEGGINGS);
                }
            },
            () -> {
                if (ChanceUtil.getChance(75)) {
                    return CustomItemCenter.build(CustomItems.ANCIENT_ROYAL_BOOTS);
                } else {
                    return CustomItemCenter.build(CustomItems.ANCIENT_BOOTS);
                }
            },
            () -> new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandom(64)),
            () -> new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(64)),
            () -> new ItemStack(Material.EMERALD, ChanceUtil.getRandom(64)),
            () -> new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, ChanceUtil.getRandom(32))
        );
    }

    public Optional<FreakyFourBoss> getFirstFullBossRoom() {
        for (FreakyFourBoss boss : FreakyFourBoss.values()) {
            if (isBossRoomEmpty(boss)) {
                continue;
            }

            return Optional.of(boss);
        }

        return Optional.empty();
    }

    // Cleans up empty arenas
    public void validateBosses() {
        for (FreakyFourBoss boss : FreakyFourBoss.values()) {
            Monster bossEnt = bossEntities.get(boss);
            if (bossEnt == null) {
                continue;
            }

            if (isBossRoomEmpty(boss)) {
                if (bossEnt.isValid()) {
                    bossEnt.remove();
                }
                bossEntities.put(boss, null);
            } else if (!bossEnt.isValid()) {
                bossEntities.put(boss, null);
            }
        }
    }
}
