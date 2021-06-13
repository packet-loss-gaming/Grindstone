/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.function.BiPredicate;

public class BlockWanderer {
    private final boolean forward;
    private final BiPredicate<Block, Block> isBetter;

    private int cardinalIndex;
    private BlockFace lastDirection;
    private int indexAdvancements = 0;
    private boolean isBacktracking = false;

    private Block current;
    private Block best;

    public BlockWanderer(Location origin, BiPredicate<Block, Block> isBetter) {
        this.forward = ChanceUtil.getChance(2);
        this.isBetter = isBetter;

        int offset = ChanceUtil.getRandom(4) - 1;
        this.cardinalIndex = offset % 4;
        this.lastDirection = getDirection();
        this.current = origin.getBlock();
        this.best = current;
    }

    private int getNextIndex() {
        int tmpIndex = (this.cardinalIndex + 1);
        if (tmpIndex > 3) {
            tmpIndex = 0;
        }
        return tmpIndex;
    }

    private int getPreviousIndex() {
        int tmpIndex = (this.cardinalIndex - 1);
        if (tmpIndex < 0) {
            tmpIndex = 3;
        }
        return tmpIndex;
    }

    private void nextDirection() {
        this.indexAdvancements++;

        // Advance in whatever way we're configured to do so
        if (this.forward) {
            this.cardinalIndex = getNextIndex();
        } else {
            this.cardinalIndex = getPreviousIndex();
        }

        // Skip directions if they cause us to immediately double back,
        // double back if we get stuck.
        if (getDirection() == lastDirection.getOppositeFace()) {
            if (indexAdvancements < 4) {
                nextDirection();
            } else {
                this.isBacktracking = true;
            }
        }
    }

    private BlockFace getDirection() {
        return EnvironmentUtil.getCardinalBlockFaces()[this.cardinalIndex];
    }

    private void advanceTo(Block block) {
        this.current = block;

        if (isBetter.test(current, best)) {
            this.best = current;
        }
    }

    private void markAdvancement() {
        this.lastDirection = getDirection();
        this.indexAdvancements = 0;
    }

    private boolean tryMoveDirection(BlockFace direction) {
        Block next = current.getRelative(direction);

        // If we've run off a cliff, don't go this way
        if (!next.getRelative(BlockFace.DOWN).getType().isSolid()) {
            if (next.getRelative(BlockFace.DOWN, 2).getType().isSolid()) {
                advanceTo(next.getRelative(BlockFace.DOWN));
                return true;
            }
            return false;
        }

        // If next is solid try and go up, if we can't do that, don't go this way
        if (next.getType().isSolid()) {
            if (!next.getRelative(BlockFace.UP).getType().isSolid()) {
                advanceTo(next.getRelative(BlockFace.UP));
                return true;
            }
            return false;
        }

        advanceTo(next);
        return true;
    }

    private boolean tryRecovery(int directionIndex) {
        BlockFace face = EnvironmentUtil.getCardinalBlockFaces()[directionIndex];

        if (tryMoveDirection(face)) {
            this.cardinalIndex = directionIndex;
            markAdvancement();
            return true;
        }

        return false;

    }

    private boolean tryRecoveryA() {
        int direction = this.forward ? getNextIndex() : getPreviousIndex();
        return tryRecovery(direction);
    }

    private boolean tryRecoveryB() {
        int direction = this.forward ? getPreviousIndex() : getNextIndex();
        return tryRecovery(direction);
    }

    public Location walk() {
        for (int i = 0; i < 50; ++i) {
            if (isBacktracking) {
                if (tryRecoveryA() || tryRecoveryB()) {
                    isBacktracking = false;
                    continue;
                }
            }

            if (tryMoveDirection(getDirection())) {
                markAdvancement();
                continue;
            }

            nextDirection();
        }

        return best.getLocation();
    }
}
