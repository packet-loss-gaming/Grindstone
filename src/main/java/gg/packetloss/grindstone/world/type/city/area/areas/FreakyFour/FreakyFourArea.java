/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.FreakyFour;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.checker.Expression;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ComponentInformation(friendlyName = "Freaky Four", desc = "The craziest bosses ever")
@Depend(components = {
        AdminComponent.class, PlayerStateComponent.class, SpectatorComponent.class, WalletComponent.class,
        HighScoresComponent.class},
        plugins = {"WorldGuard"})
public class FreakyFourArea extends AreaComponent<FreakyFourConfig> {

    protected static final int groundLevel = 79;

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

    protected ProtectedRegion charlotte_RG, frimus_RG, dabomb_RG, snipee_RG, heads;

    protected Spider charlotte;
    protected Blaze frimus;
    protected Creeper daBomb;
    protected Skeleton snipee;

    protected Location entrance;

    @Override
    public void setUp() {
        spectator.registerSpectatorKind(PlayerStateKind.FREAKY_FOUR_SPECTATOR);

        world = server.getWorlds().get(0);
        entrance = new Location(world, 401.5, 79, -304, 270, 0);
        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        String base = "oblitus-district-freaky-four";
        region = manager.getRegion(base);
        charlotte_RG = manager.getRegion(base + "-charlotte");
        frimus_RG = manager.getRegion(base + "-frimus");
        dabomb_RG = manager.getRegion(base + "-da-bomb");
        snipee_RG = manager.getRegion(base + "-snipee");
        heads = manager.getRegion(base + "-heads");
        tick = 4 * 20;
        listener = new FreakyFourListener(this);
        config = new FreakyFourConfig();

        CommandBook.registerEvents(new FlightBlockingListener(admin, this::contains));

        spectator.registerSpectatedRegion(PlayerStateKind.FREAKY_FOUR_SPECTATOR, region);
        spectator.registerSpectatorSkull(
                PlayerStateKind.FREAKY_FOUR_SPECTATOR,
                new Location(world, 400, 79, -307),
                () -> getFirstFullRoom().isPresent()
        );
    }

    @Override
    public void run() {
        if (!isEmpty()) {
            if (!checkCharlotte()) {
                runCharlotte();
            }
            if (!checkFrimus()) {
                runFrimus();
            }
        }
    }

    public Location getCharlotteEntrance() {
        return new Location(world, 398.5, 79, -304, 90, 0);
    }

    public Location getFrimusEntrance() {
        return new Location(world, 374.5, 79, -304, 90, 0);
    }

    public Location getDaBombEntrance() {
        return new Location(world, 350.5, 79, -304, 90, 0);
    }

    public Location getSnipeeEntrance() {
        return new Location(world, 326.5, 79, -304, 90, 0);
    }

    public void addSkull(Player player) {
        Location v = LocationUtil.pickLocation(world, heads);
        SkullPlacer.placePlayerSkullOnWall(v, BlockFace.WEST, player);
    }

    protected Location getCentralLoc(ProtectedRegion region) {
        return RegionUtil.getCenterAt(world, groundLevel, region);
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

    public void spawnCharlotte() {
        charlotte = getWorld().spawn(getCentralLoc(charlotte_RG), Spider.class);

        // Handle vitals
        charlotte.setMaxHealth(config.charlotteHP);
        charlotte.setHealth(config.charlotteHP);
        charlotte.setRemoveWhenFarAway(false);

        try {
            AttributeBook.setAttribute(charlotte, AttributeBook.Attribute.FOLLOW_RANGE, 50);
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }

        // Handle name
        charlotte.setCustomName("Charlotte");
    }

    public boolean checkCharlotte() {
        return getContainedParticipantsIn(charlotte_RG).isEmpty();
    }

    public void cleanupCharlotte() {
        RegionWalker.walk(charlotte_RG, (x, y, z) -> {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.COBWEB) {
                block.setType(Material.AIR);
            }
        });

        for (Entity spider : getContained(charlotte_RG, Spider.class, CaveSpider.class)) {
            spider.remove();
        }
    }

    public void runCharlotte() {
        if (charlotte == null) return;
        for (int i = ChanceUtil.getRandom(10); i > 0; --i) {
            world.spawn(charlotte.getLocation(), CaveSpider.class);
        }

        switch (ChanceUtil.getRandom(3)) {
            case 1:
                createWall(charlotte_RG,
                        input -> input.getType() == Material.AIR,
                        input -> input.getType() == Material.COBWEB,
                        Material.AIR,
                        Material.COBWEB,
                        1,
                        config.charlotteFloorWeb
                );
                break;
            case 2:
                LivingEntity target = charlotte.getTarget();
                if (target != null && contains(target)) {
                    List<Location> queList = new ArrayList<>();
                    for (Location loc : Arrays.asList(target.getLocation(), target.getEyeLocation())) {
                        for (BlockFace face : EnvironmentUtil.getNearbyBlockFaces()) {
                            if (face == BlockFace.SELF) continue;
                            queList.add(loc.getBlock().getRelative(face).getLocation());
                        }
                    }
                    for (Location loc : queList) {
                        Block block = world.getBlockAt(loc);
                        if (block.getType().isSolid()) continue;
                        block.setType(Material.COBWEB);
                    }
                }
                break;
            case 3:
                RegionWalker.walk(charlotte_RG, (x, y, z) -> {
                    if (!ChanceUtil.getChance(config.charlotteWebSpider)) return;

                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.COBWEB) {
                        block.setType(Material.AIR);
                        world.spawn(block.getLocation(), CaveSpider.class);
                    }
                });
                break;
        }
    }

    public void spawnFrimus() {
        frimus = getWorld().spawn(getCentralLoc(frimus_RG), Blaze.class);

        // Handle vitals
        frimus.setRemoveWhenFarAway(false);
        frimus.setMaxHealth(config.frimusHP);
        frimus.setHealth(config.frimusHP);

        try {
            AttributeBook.setAttribute(frimus, AttributeBook.Attribute.FOLLOW_RANGE, 50);
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }

        // Handle name
        frimus.setCustomName("Frimus");
    }

    public boolean checkFrimus() {
        return getContainedParticipantsIn(frimus_RG).isEmpty();
    }

    public void cleanupFrimus() {
        RegionWalker.walk(frimus_RG, (x, y, z) -> {
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() == Material.FIRE || EnvironmentUtil.isLava(block.getType())) {
                block.setType(Material.AIR);
            }
        });

        for (Entity spider : getContained(frimus_RG, Blaze.class)) {
            spider.remove();
        }
    }

    private void runFrimus() {
        if (frimus == null) return;
        createWall(frimus_RG,
                input -> input.getType() == Material.AIR,
                EnvironmentUtil::isLava,
                Material.AIR,
                Material.LAVA,
                config.frimusWallDensity,
                -1
        );
    }

    public void spawnDaBomb() {
        daBomb = getWorld().spawn(getCentralLoc(dabomb_RG), Creeper.class);

        // Handle vitals
        daBomb.setMaxHealth(config.daBombHP);
        daBomb.setHealth(config.daBombHP);
        daBomb.setRemoveWhenFarAway(false);

        try {
            AttributeBook.setAttribute(daBomb, AttributeBook.Attribute.FOLLOW_RANGE, 50);
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }

        // Handle name
        daBomb.setCustomName("Da Bomb");
    }

    public boolean checkDaBomb() {
        return getContainedParticipantsIn(dabomb_RG).isEmpty();
    }

    public void cleanupDaBomb() {
        for (Creeper creeper : getContained(dabomb_RG, Creeper.class)) {
            creeper.remove();
        }
    }

    public void spawnSnipee() {
        snipee = getWorld().spawn(getCentralLoc(snipee_RG), Skeleton.class);

        // Handle vitals
        snipee.setMaxHealth(config.snipeeHP);
        snipee.setHealth(config.snipeeHP);
        snipee.setRemoveWhenFarAway(false);

        try {
            AttributeBook.setAttribute(snipee, AttributeBook.Attribute.MOVEMENT_SPEED, 0.15);
            AttributeBook.setAttribute(snipee, AttributeBook.Attribute.FOLLOW_RANGE, 50);
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }

        // Handle name
        snipee.setCustomName("Snipee");
    }

    public boolean checkSnipee() {
        return getContainedParticipantsIn(snipee_RG).isEmpty();
    }

    public void cleanupSnipee() {
        for (Skeleton skeleton : getContained(snipee_RG, Skeleton.class)) {
            skeleton.remove();
        }
    }

    public Optional<Location> getFirstFullRoom() {
        if (!checkCharlotte()) {
            return Optional.of(getCharlotteEntrance());
        }
        if (!checkFrimus()) {
            return Optional.of(getFrimusEntrance());
        }
        if (!checkDaBomb()) {
            return Optional.of(getDaBombEntrance());
        }
        if (!checkSnipee()) {
            return Optional.of(getSnipeeEntrance());
        }

        return Optional.empty();
    }

    // Cleans up empty arenas
    public void validateBosses() {
        if (charlotte != null) {
            if (checkCharlotte()) {
                if (charlotte.isValid()) {
                    charlotte.remove();
                }
                charlotte = null;
            } else if (!charlotte.isValid()) {
                charlotte = null;
            }
        }
        if (frimus != null) {
            if (checkFrimus()) {
                if (frimus.isValid()) {
                    frimus.remove();
                }
                frimus = null;
            } else if (!frimus.isValid()) {
                frimus = null;
            }
        }
        if (daBomb != null) {
            if (checkDaBomb()) {
                if (daBomb.isValid()) {
                    daBomb.remove();
                }
                daBomb = null;
            } else if (!daBomb.isValid()) {
                daBomb = null;
            }
        }
        if (snipee != null) {
            if (checkSnipee()) {
                if (snipee.isValid()) {
                    snipee.remove();
                }
                snipee = null;
            } else if (!snipee.isValid()) {
                snipee = null;
            }
        }
    }
}
