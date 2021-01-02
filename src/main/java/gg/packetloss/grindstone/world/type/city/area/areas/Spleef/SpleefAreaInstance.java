/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Spleef;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.region.RegionWalker;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SpleefAreaInstance {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private final SpleefArea component;

    private World world;
    private ProtectedRegion containmentRegion;
    private ProtectedRegion floorRegion;
    private ProtectedRegion wallRegion;

    private boolean isSmallArena;

    private int innerTick = 0;
    private int activeTicks = 0;

    public SpleefAreaInstance(SpleefArea component, World world, RegionManager manager, String regionName) {
        this.component = component;
        this.world = world;
        this.containmentRegion = manager.getRegion(regionName);
        this.floorRegion = manager.getRegion(regionName + "-floor");
        this.wallRegion = manager.getRegion(regionName + "-walls");

        this.isSmallArena = regionName.contains("small");;
    }

    public boolean contains(Location location) {
        return LocationUtil.isInRegion(world, containmentRegion, location);
    }

    private boolean shouldDoInnerTick() {
        innerTick = (innerTick + 1) % 8;
        return innerTick == 0;
    }

    private void updateActiveTick(Collection<Player> players) {
        if (players.size() > 1) {
            ++activeTicks;
        } else {
            activeTicks = 0;
        }
    }

    public void feed(Collection<Player> players) {
        for (Player player : players) {
            if (!component.isUsingArenaTools(player)) {
                continue;
            }

            player.setFoodLevel(20);
            player.setSaturation(20F);
            player.setExhaustion(0);
        }
    }

    public void restoreFloor(Collection<Player> players) {
        if (players.size() > 1) {
            return;
        }

        CuboidRegion snow = new CuboidRegion(floorRegion.getMaximumPoint(), floorRegion.getMinimumPoint());

        if (snow.getArea() > 8208 || snow.getHeight() > 1) {
            log.warning("The region: " + floorRegion.getId() + " is too large.");
            return;
        }

        for (BlockVector3 bv : snow) {
            Block b = world.getBlockAt(bv.getBlockX(), bv.getBlockY(), bv.getBlockZ());
            if (b.getType() != Material.SNOW_BLOCK) {
                b.setType(Material.SNOW_BLOCK, false);
            }
        }
    }

    private void tryChangeBlockAt(int x, int y, int z, Material from, Material to) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == from) {
            block.setType(to, false);
        }
    }

    private boolean shouldBuildWalls() {
        if (activeTicks > 5) {
            return true;
        }

        if (isSmallArena && activeTicks > 2) {
            return true;
        }

        return false;
    }

    public void buildWalls() {
        if (wallRegion == null) {
            return;
        }

        BlockVector3 min = wallRegion.getMinimumPoint();
        BlockVector3 max = wallRegion.getMaximumPoint();

        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        Material toMat = shouldBuildWalls() ? Material.ICE : Material.AIR;
        Material fromMat = toMat == Material.ICE ? Material.AIR : Material.ICE;

        for (int y = minY, yIncrease = 0; y < maxY; ++y, ++yIncrease) {
            if (yIncrease > 2) {
                if (toMat == Material.ICE) {
                    toMat = Material.BARRIER;
                }
                if (fromMat == Material.ICE) {
                    fromMat = Material.BARRIER;
                }
            }

            for (int x = minX; x < maxX; ++x) {
                tryChangeBlockAt(x, y, minZ, fromMat, toMat);
                tryChangeBlockAt(x, y, maxZ, fromMat, toMat);
            }

            for (int z = minZ; z < maxZ; ++z) {
                tryChangeBlockAt(minX, y, z, fromMat, toMat);
                tryChangeBlockAt(maxX, y, z, fromMat, toMat);
            }
        }
    }

    private boolean shouldDoCamperTick() {
        return innerTick % 4 == 0;
    }

    public void punishCampers(Collection<Player> players) {
        boolean punishmentsActive = activeTicks >= component.config.antiCampTicksBeforeActive;
        boolean warningsActive = activeTicks >= component.config.antiCampTicksBeforeWarning;

        long currentTime = System.currentTimeMillis();
        for (Player player : players) {
            UUID playerId = player.getUniqueId();
            long timeDiff = currentTime - component.lastBlockBreak.getOrDefault(playerId, 0L);

            long timeToPunish = TimeUnit.SECONDS.toMillis(component.config.antiCampShovelIdleSeconds);
            boolean shouldPunishPlayer = timeDiff <= timeToPunish && punishmentsActive;

            long timeToWarn = TimeUnit.SECONDS.toMillis(component.config.antiCampShovelIdleWarnSeconds);
            boolean alreadyWarned = component.warnedPlayers.contains(playerId);
            boolean shouldWarnPlayer = timeDiff > timeToWarn && warningsActive && !alreadyWarned;

            if (!shouldPunishPlayer && shouldWarnPlayer) {
                ChatUtil.sendWarning(player, "Break some snow soon, or you'll be accused of being a camper!");
                component.warnedPlayers.add(playerId);
                continue;
            }

            if (!shouldPunishPlayer) {
                continue;
            }

            Location pLoc = player.getLocation();
            CuboidRegion rg = new CuboidRegion(
                BlockVector3.at(
                    pLoc.getBlockX() - 1,
                    floorRegion.getMinimumPoint().getBlockY(),
                    pLoc.getBlockZ() - 1
                ),
                BlockVector3.at(
                    pLoc.getBlockX() + 1,
                    floorRegion.getMaximumPoint().getBlockY(),
                    pLoc.getBlockZ() + 1
                )
            );

            RegionWalker.walk(rg, (x, y, z) -> {
                tryChangeBlockAt(x, y, z, Material.SNOW_BLOCK, Material.AIR);
            });

            ChatUtil.sendWarning(player, "Stop camping without marshmallows. It's wrong.");
        }
    }

    private Collection<Player> getParticipants() {
        return LocationUtil.getPlayersStandingOnRegion(world, floorRegion, true);
    }

    // This is a heuristic approach to determine if the region is loaded
    private boolean isLoaded() {
        return RegionUtil.isLoaded(world, containmentRegion);
    }

    public Collection<Player> run() {
        if (!isLoaded()) {
            return List.of();
        }

        Collection<Player> players = getParticipants();

        if (shouldDoInnerTick()) {
            feed(players);
            updateActiveTick(players);
            restoreFloor(players);
            buildWalls();
        }

        if (shouldDoCamperTick()) {
            punishCampers(players);
        }

        return players;
    }
}
