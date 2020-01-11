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
        PortalTester tester = new PortalTester(direction);
        if (!tester.walk()) {
            return false;
        }

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

    class PortalTester {
        private BlockFace walkingDirection;
        private Block currentBlock;

        private int step = 0;

        private int minVert;
        private int maxVert;
        private int minHoriz;
        private int maxHoriz;

        private void initHoriz() {
            switch (walkingDirection) {
                case NORTH:
                    minHoriz = maxHoriz = ignitedBlock.getZ();
                    break;
                case EAST:
                    minHoriz = maxHoriz = ignitedBlock.getX();
                    break;
            }
        }

        private void initVert() {
            this.minVert = this.maxVert = ignitedBlock.getY();
        }

        public PortalTester(BlockFace walkingDirection) {
            this.walkingDirection = walkingDirection;

            this.initHoriz();
            this.initVert();
        }

        private void updateHoriz(Block block) {
            switch (walkingDirection) {
                case NORTH:
                    minHoriz = Math.min(minHoriz, block.getZ());
                    maxHoriz = Math.max(maxHoriz, block.getZ());
                    break;
                case EAST:
                    minHoriz = Math.min(minHoriz, block.getX());
                    maxHoriz = Math.max(maxHoriz, block.getX());
                    break;
            }
        }

        private void updateVert(Block block) {
            minVert = Math.min(minVert, block.getY());
            maxVert = Math.max(maxVert, block.getY());
        }

        private void walk(Block block) {
            this.updateHoriz(block);
            this.updateVert(block);

            this.currentBlock = block;
        }

        private boolean trialWalk(BlockFace direction, BlockFace adjustment) {
            Block testBlock = currentBlock.getRelative(direction).getRelative(adjustment);
            if (testBlock.getType() == blockType) {
                walk(testBlock);
                return true;
            }

            return false;
        }

        public boolean walk() {
            Block startingBlock = ignitedBlock.getRelative(BlockFace.DOWN);

            currentBlock = startingBlock;
            while (!exceedsMaxStep() && !exceedsHorizLimit() && !exceedsVertLimit()) {
                if (step == 3 && currentBlock.equals(startingBlock)) {
                    return meetsHorizMin() && meetsVertMin();
                }

                BlockFace verticalDirection = step < 2 ? BlockFace.UP : BlockFace.DOWN;
                BlockFace horizontalDirection = step == 0 || step == 3 ? walkingDirection : walkingDirection.getOppositeFace();

                if (trialWalk(verticalDirection, BlockFace.SELF)) {
                    continue;
                }

                if (trialWalk(horizontalDirection, verticalDirection)) {
                    continue;
                }

                if (trialWalk(horizontalDirection, BlockFace.SELF)) {
                    continue;
                }

                ++step;
            }

            return false;
        }

        public boolean meetsVertMin() {
            return (maxHoriz + 1) - minHoriz >= 4;
        }

        public boolean meetsHorizMin() {
            return (maxVert + 1) - minVert >= 5;
        }

        public boolean exceedsVertLimit() {
            return (maxHoriz + 1) - minHoriz > 23;
        }

        public boolean exceedsHorizLimit() {
            return (maxVert + 1) - minVert > 23;
        }

        public boolean exceedsMaxStep() {
            return step > 3;
        }
    }

    private boolean isAirOrFire(Block block) {
        Material blockType = block.getType();
        return blockType == Material.AIR || blockType == Material.FIRE;
    }

    class PortalFiller {
        private BlockFace walkingDirection;

        public PortalFiller(BlockFace walkingDirection) {
            this.walkingDirection = walkingDirection;
        }

        private boolean doFillOp(Block sourceBlock, Predicate<Block> op) {
            switch (walkingDirection) {
                case NORTH:
                    return op.test(sourceBlock.getRelative(BlockFace.NORTH)) &&
                            op.test(sourceBlock.getRelative(BlockFace.SOUTH)) &&
                            op.test(sourceBlock.getRelative(BlockFace.UP));
                case EAST:
                    return op.test(sourceBlock.getRelative(BlockFace.EAST)) &&
                            op.test(sourceBlock.getRelative(BlockFace.WEST)) &&
                            op.test(sourceBlock.getRelative(BlockFace.UP));
            }

            throw new UnsupportedOperationException();
        }

        private boolean checkFill(Set<BlockVector3> alreadyChecked, Block block) {
            return doFillOp(block, (opBlock) -> {
                BlockVector3 blockCoords = WorldEditBridge.toBlockVec3(opBlock);
                if (alreadyChecked.contains(blockCoords)) {
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

                alreadyChecked.add(blockCoords);

                return checkFill(alreadyChecked, opBlock);
            });
        }

        public boolean checkFill() {
            return checkFill(new HashSet<>(), ignitedBlock);
        }

        private boolean fill(Block block) {
            return doFillOp(block, (opBlock) -> {
                Material opBlockType = opBlock.getType();
                if (opBlockType == blockType || opBlockType == Material.NETHER_PORTAL) {
                    return true;
                }

                opBlock.setType(Material.NETHER_PORTAL);
                Orientable blockData = ((Orientable) opBlock.getBlockData());
                switch (walkingDirection) {
                    case NORTH:
                        blockData.setAxis(Axis.Z);
                        break;
                    case EAST:
                        blockData.setAxis(Axis.X);
                        break;
                }
                opBlock.setBlockData(blockData);

                return fill(opBlock);
            });
        }

        public void fill() {
            fill(ignitedBlock);
        }
    }
}
