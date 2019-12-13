/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.FreakyFour;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.SkullBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.APIUtil;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.checker.Expression;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ComponentInformation(friendlyName = "Freaky Four", desc = "The craziest bosses ever")
@Depend(components = {AdminComponent.class, PlayerStateComponent.class}, plugins = {"WorldGuard"})
public class FreakyFourArea extends AreaComponent<FreakyFourConfig> {

    protected static final int groundLevel = 79;

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected PlayerStateComponent playerState;

    protected Economy economy;

    protected ProtectedRegion charlotte_RG, frimus_RG, dabomb_RG, snipee_RG, heads;

    protected Spider charlotte;
    protected Blaze frimus;
    protected Creeper daBomb;
    protected Skeleton snipee;

    protected Location entrance;

    @Override
    public void setUp() {
        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();
            world = server.getWorlds().get(0);
            entrance = new Location(world, 401.5, 79, -304, 270, 0);
            RegionManager manager = WG.getRegionManager(world);
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

            setupEconomy();
        } catch (UnknownPluginException e) {
            log.info("WorldGuard could not be found!");
        }
    }

    @Override
    public void enable() {
        server.getScheduler().runTaskLater(inst, super::enable, 1);
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
            if (!checkSnipee()) {
                runSnipee();
            }
        }
    }

    public void addSkull(Player player) {
        final BlockVector min = heads.getMinimumPoint();
        final BlockVector max = heads.getMaximumPoint();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();
        com.sk89q.worldedit.Vector v = LocationUtil.pickLocation(minX, maxX, minY, maxY, minZ, maxZ);
        BukkitWorld world = new BukkitWorld(this.world);
        EditSession skullEditor = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, 1);
        skullEditor.rawSetBlock(v, new SkullBlock(12, (byte) 12, player.getName()));
    }

    protected Location getCentralLoc(ProtectedRegion region) {
        BlockVector min = region.getMinimumPoint();
        BlockVector max = region.getMaximumPoint();

        Region rg = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(world, rg.getCenter().setY(groundLevel));
    }

    private void createWall(ProtectedRegion region,
                            Expression<Block, Boolean> oldExpr,
                            Expression<Block, Boolean> newExpr,
                            Material oldType, Material newType,
                            int density, int floodFloor) {

        final BlockVector min = region.getMinimumPoint();
        final BlockVector max = region.getMaximumPoint();
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
        charlotte.setRemoveWhenFarAway(true);

        // Handle name
        charlotte.setCustomName("Charlotte");
    }

    public boolean checkCharlotte() {
        return !LocationUtil.containsPlayer(world, charlotte_RG);
    }

    public void cleanupCharlotte() {
        final BlockVector min = charlotte_RG.getMinimumPoint();
        final BlockVector max = charlotte_RG.getMaximumPoint();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.WEB) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        for (Entity spider : getContained(charlotte_RG, Spider.class, CaveSpider.class)) {
            spider.remove();
        }
    }

    public void runCharlotte() {
        if (charlotte == null) return;
        for (int i = ChanceUtil.getRandom(10); i > 0; --i) {
            world.spawn(charlotte.getLocation(), CaveSpider.class);
        }

        final BlockVector min = charlotte_RG.getMinimumPoint();
        final BlockVector max = charlotte_RG.getMaximumPoint();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        switch (ChanceUtil.getRandom(3)) {
            case 1:
                createWall(charlotte_RG,
                        input -> input.getType() == Material.AIR,
                        input -> input.getType() == Material.WEB,
                        Material.AIR,
                        Material.WEB,
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
                        block.setType(Material.WEB);
                    }
                }
                break;
            case 3:
                for (int y = minY; y <= maxY; ++y) {
                    for (int x = minX; x <= maxX; ++x) {
                        for (int z = minZ; z <= maxZ; ++z) {
                            if (!ChanceUtil.getChance(config.charlotteWebSpider)) continue;
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() == Material.WEB) {
                                block.setType(Material.AIR);
                                world.spawn(block.getLocation(), CaveSpider.class);
                            }
                        }
                    }
                }
                break;
        }
    }

    public void spawnFrimus() {
        frimus = getWorld().spawn(getCentralLoc(frimus_RG), Blaze.class);

        // Handle vitals
        frimus.setRemoveWhenFarAway(true);
        // Work around for health
        server.getScheduler().runTaskLater(inst, () -> {
            frimus.setMaxHealth(config.frimusHP);
            frimus.setHealth(config.frimusHP);
        }, 1);

        // Handle name
        frimus.setCustomName("Frimus");
    }

    public boolean checkFrimus() {
        return !LocationUtil.containsPlayer(world, frimus_RG);
    }

    public void cleanupFrimus() {
        final BlockVector min = frimus_RG.getMinimumPoint();
        final BlockVector max = frimus_RG.getMaximumPoint();
        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.FIRE || EnvironmentUtil.isLava(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

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
        daBomb.setRemoveWhenFarAway(true);

        // Handle name
        daBomb.setCustomName("Da Bomb");
    }

    public boolean checkDaBomb() {
        return !LocationUtil.containsPlayer(world, dabomb_RG);
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
        snipee.setRemoveWhenFarAway(true);

        // Handle name
        snipee.setCustomName("Snipee");
    }

    public boolean checkSnipee() {
        return !LocationUtil.containsPlayer(world, snipee_RG);
    }

    public void cleanupSnipee() {
        for (Skeleton skeleton : getContained(snipee_RG, Skeleton.class)) {
            skeleton.remove();
        }
    }

    public void runSnipee() {
        if (snipee == null) return;
        com.sk89q.commandbook.util.entity.EntityUtil.sendProjectilesFromEntity(snipee, 20, 1.6F, Arrow.class);
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

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
}
