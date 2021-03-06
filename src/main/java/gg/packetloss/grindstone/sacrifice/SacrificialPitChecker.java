/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

class SacrificialPitChecker {
    private static final int MIN_HEIGHT = 2;
    private static final int MIN_AREA = 24;
    private static final int MAX_AREA = (int) Math.pow(20, 2);

    private final Location origin;
    private final Predicate<Block> isSacrificeBlock;
    private final Set<BlockVector3> points = new HashSet<>();

    private BlockVector2 minimumPoint = BlockVector2.at(Integer.MAX_VALUE, Integer.MAX_VALUE);
    private boolean foundWrongDepth = false;

    public SacrificialPitChecker(Location origin, Predicate<Block> isSacrificeBlock) {
        this.origin = origin;
        this.isSacrificeBlock = isSacrificeBlock;
    }

    private boolean isIgnitable(Block block) {
        Material blockType = block.getType();
        return blockType.isAir() || blockType == Material.FIRE;
    }

    private boolean checkColumnFormat(Block block) {
        if (!isIgnitable(block)) {
            return false;
        }

        for (int i = 0; i < MIN_HEIGHT; ++i) {
            block = block.getRelative(BlockFace.DOWN);
            if (!isSacrificeBlock.test(block)) {
                foundWrongDepth = true;
                return false;
            }
        }

        return true;
    }

    private void crawlBlocks(Block block) {
        // Short circuit recursion, this pit isn't a pit (doesn't have walls)
        if (foundWrongDepth) {
            return;
        }

        BlockVector3 blockCoords = WorldEditBridge.toBlockVec3(block);
        if (points.contains(blockCoords)) {
            return;
        }

        if (!checkColumnFormat(block)) {
            return;
        }

        points.add(blockCoords);

        minimumPoint = BlockVector2.at(
                Math.min(blockCoords.getBlockX(), minimumPoint.getBlockX()),
                Math.min(blockCoords.getBlockZ(), minimumPoint.getBlockZ())
        );

        for (BlockFace face : new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST }) {
            crawlBlocks(block.getRelative(face));
        }
    }

    public BlockVector2 getIdentifyingPoint() {
        return minimumPoint;
    }

    public boolean isValid() {
        crawlBlocks(origin.getBlock());

        return !foundWrongDepth && MIN_AREA <= points.size() && points.size() <= MAX_AREA;
    }

    private void fillWith(Material type) {
        for (BlockVector3 point : points) {
            Block block = origin.getWorld().getBlockAt(point.getBlockX(), point.getBlockY(), point.getBlockZ());
            if (block.getType() != type) {
                block.setType(type);
            }
        }
    }

    public void ignite() {
        fillWith(Material.FIRE);
    }

    public void extinguish() {
        fillWith(Material.AIR);
    }
}
