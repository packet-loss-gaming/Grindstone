package gg.packetloss.grindstone.city.engine.area.areas.Spleef;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
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

        for (int y = minY; y < maxY; ++y) {
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

    private Collection<Player> getParticipants() {
        return LocationUtil.getPlayersStandingOnRegion(world, floorRegion, true);
    }

    // This is a heuristic approach to determine if the region is loaded
    private boolean isLoaded() {
        BlockVector3 min = containmentRegion.getMinimumPoint();
        if (!world.isChunkLoaded(min.getBlockX() >> 4, min.getBlockZ() >> 4)) {
            return false;
        }

        BlockVector3 max = containmentRegion.getMaximumPoint();
        if (!world.isChunkLoaded(max.getBlockX() >> 4, max.getBlockZ() >> 4)) {
            return false;
        }

        return true;
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

        return players;
    }
}
