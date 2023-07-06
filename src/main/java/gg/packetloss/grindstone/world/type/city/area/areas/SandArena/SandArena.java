/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.SandArena;

import com.sk89q.worldedit.math.BlockVector3;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Set;

@ComponentInformation(friendlyName = "Sand Arena", desc = "It's a bit dry")
@Depend(components = {AdminComponent.class, PlayerStateComponent.class}, plugins = {"WorldGuard"})
public class SandArena extends AreaComponent<SandArenaConfig> {
    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected PlayerStateComponent playerState;

    protected static final Set<Material> RESPAWN_BLOCKS = Set.of(
            Material.OAK_PLANKS, Material.STONE_BRICKS
    );

    @Override
    public void setUp() {
        world = Bukkit.getWorlds().get(0);
        region = WorldGuardBridge.getManagerFor(world).getRegion("oblitus-district-arena-pvp");
        tick = 5 * 20;
        listener = new SandArenaListener(this);
        config = new SandArenaConfig();
    }

    @Override
    public void run() {
        if (!isLoaded()) {
            return;
        }

        if (!isEmpty()) {
            addBlocks();
        }
        removeBlocks();
    }

    private boolean isLoaded() {
        return RegionUtil.isLoaded(getWorld(), getRegion());
    }

    public void addBlocks() {
        BlockVector3 min = getRegion().getMinimumPoint();
        BlockVector3 max = getRegion().getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = maxY; y >= minY; --y) {
                    Block block = getWorld().getBlockAt(x, y, z);
                    Block topBlock = getWorld().getBlockAt(x, y + 1, z);

                    if (y == minY) {
                        block.setType(Material.SAND, false);
                    }

                    if (!(y + 1 > getWorld().getMaxHeight())
                            && !(y + 1 > maxY)
                            && !EnvironmentUtil.isAirBlock(block)
                            && EnvironmentUtil.isAirBlock(topBlock)
                            && !LocationUtil.isCloseToPlayer(block, 4)) {
                        if (ChanceUtil.getChance(config.increaseRate)) {
                            topBlock.setType(Material.SAND, false);
                        }
                        break;
                    }
                }
            }
        }
    }

    public void removeBlocks() {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        int minX = min.getBlockX();
        int minY = min.getBlockY();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = world.getHighestBlockYAt(x, z) - minY; y > 0; y--) {
                    Block block = world.getBlockAt(x, y + minY, z);

                    if (!cachedEmpty()) {
                        if (y + minY < world.getMaxHeight()
                                && ChanceUtil.getChance(config.decreaseRate - (y * ChanceUtil.getRandom(5)))
                                && !LocationUtil.isCloseToPlayer(block, 4)) {
                            block.setType(Material.AIR, false);
                        } else {
                            break;
                        }
                    } else {
                        if (y + minY < world.getMaxHeight()
                                && ChanceUtil.getChance((config.decreaseRate - (y * ChanceUtil.getRandom(5))) / 4)) {
                            block.setType(Material.AIR, false);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    public Location getRespawnLocation() {
        BlockVector3 v;
        BlockVector3 min = getRegion().getParent().getMinimumPoint();
        BlockVector3 max = getRegion().getParent().getMaximumPoint();

        do {
            v = LocationUtil.pickLocation(min.getX(), max.getX(), min.getY(), max.getY(), min.getZ(), max.getZ());
        } while (getRegion().contains(v) || !isRespawnBlock(v) || !EnvironmentUtil.isAirBlock(getBlock(v)));
        return new Location(world, v.getX(), v.getY(), v.getZ());
    }

    private boolean isRespawnBlock(BlockVector3 v) {
        for (Material type : RESPAWN_BLOCKS) {
            if (type == getBlock(v.add(0, -1, 0)).getType()) return true;
        }
        return false;
    }

    private Block getBlock(BlockVector3 v) {
        return getWorld().getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }
}
