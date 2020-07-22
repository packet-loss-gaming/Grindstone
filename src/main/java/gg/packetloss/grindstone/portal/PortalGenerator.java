/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.portal;

import com.sk89q.worldedit.math.BlockVector3;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Orientable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class PortalGenerator {
    private final Predicate<Block> permissionCheck;
    private final Block ignitedBlock;
    private final Material blockType;

    public PortalGenerator(Predicate<Block> permissionCheck, Block ignitedBlock, Material blockType) {
        this.permissionCheck = permissionCheck;
        this.ignitedBlock = ignitedBlock;
        this.blockType = blockType;
    }

    private boolean tryWalk(BlockFace direction) {
        PortalFiller filler = new PortalFiller(direction);
        if (!filler.checkFill()) {
            return false;
        }

        filler.fill();
        return true;
    }

    private static final List<BlockFace> CHECK_DIRECTIONS = List.of(
            BlockFace.NORTH,
            BlockFace.EAST
    );

    public boolean walk() {
        for (BlockFace checkDirection : CHECK_DIRECTIONS) {
            if (tryWalk(checkDirection)) {
                return true;
            }
        }

        return false;
    }

    private boolean isAirOrFire(Block block) {
        Material blockType = block.getType();
        return blockType == Material.AIR || blockType == Material.FIRE;
    }

    class PortalFiller {
        private BlockFace walkingDirection;
        private Set<BlockVector3> blocks;

        public PortalFiller(BlockFace walkingDirection) {
            this.walkingDirection = walkingDirection;
        }

        private boolean doFillOp(Block sourceBlock, Predicate<Block> op) {
            switch (walkingDirection) {
                case NORTH:
                    return op.test(sourceBlock.getRelative(BlockFace.DOWN)) &&
                            op.test(sourceBlock.getRelative(BlockFace.NORTH)) &&
                            op.test(sourceBlock.getRelative(BlockFace.SOUTH)) &&
                            op.test(sourceBlock.getRelative(BlockFace.UP));
                case EAST:
                    return op.test(sourceBlock.getRelative(BlockFace.DOWN)) &&
                            op.test(sourceBlock.getRelative(BlockFace.EAST)) &&
                            op.test(sourceBlock.getRelative(BlockFace.WEST)) &&
                            op.test(sourceBlock.getRelative(BlockFace.UP));
            }

            throw new UnsupportedOperationException();
        }

        private boolean checkFill(Block block) {
            return doFillOp(block, (opBlock) -> {
                BlockVector3 blockCoords = WorldEditBridge.toBlockVec3(opBlock);
                if (blocks.contains(blockCoords)) {
                    return true;
                }

                if (opBlock.getType() == blockType) {
                    return true;
                }

                if (!isAirOrFire(opBlock)) {
                    return false;
                }

                if (!permissionCheck.test(opBlock)) {
                    return false;
                }

                blocks.add(blockCoords);
                if (blocks.size() > Math.pow(22, 2)) {
                    return false;
                }

                return checkFill(opBlock);
            });
        }

        public boolean checkFill() {
            blocks = new HashSet<>();
            return checkFill(ignitedBlock);
        }

        public void fill() {
            blocks.forEach((pos) -> {
                Block block = ignitedBlock.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
                block.setType(Material.NETHER_PORTAL);
                Orientable blockData = ((Orientable) block.getBlockData());
                switch (walkingDirection) {
                    case NORTH:
                        blockData.setAxis(Axis.Z);
                        break;
                    case EAST:
                        blockData.setAxis(Axis.X);
                        break;
                }
                block.setBlockData(blockData);
            });
        }
    }
}
