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

    private void nextDirection() {
        this.indexAdvancements++;

        // Advance in whatever way we're configured to do so
        if (this.forward) {
            this.cardinalIndex = (this.cardinalIndex + 1);
            if (this.cardinalIndex > 3) {
                this.cardinalIndex = 0;
            }
        } else {
            this.cardinalIndex = (this.cardinalIndex - 1);
            if (this.cardinalIndex < 0) {
                this.cardinalIndex = 3;
            }
        }

        // Skip directions if they cause us to immediately double back,
        // double back if we get stuck.
        if (getDirection() == lastDirection.getOppositeFace() && indexAdvancements < 4) {
            nextDirection();
        }
    }

    private BlockFace getDirection() {
        return EnvironmentUtil.getCardinalBlockFaces()[this.cardinalIndex];
    }

    private void advanceTo(Block block) {
        this.current = block;
        this.lastDirection = getDirection();
        this.indexAdvancements = 0;

        if (isBetter.test(current, best)) {
            this.best = current;
        }
    }

    public Location walk() {
        for (int i = 0; i < 50; ++i) {
            Block next = current.getRelative(getDirection());

            // If we've run off a cliff, don't go this way
            if (!next.getRelative(BlockFace.DOWN).getType().isSolid()) {
                if (next.getRelative(BlockFace.DOWN, 2).getType().isSolid()) {
                    advanceTo(next.getRelative(BlockFace.DOWN));
                    continue;
                }
                nextDirection();
                continue;
            }

            // If next is solid try and go up, if we can't do that, don't go this way
            if (next.getType().isSolid()) {
                if (!next.getRelative(BlockFace.UP).getType().isSolid()) {
                    advanceTo(next.getRelative(BlockFace.UP));
                    continue;
                }
                nextDirection();
                continue;
            }

            advanceTo(next);
        }

        return best.getLocation();
    }
}
