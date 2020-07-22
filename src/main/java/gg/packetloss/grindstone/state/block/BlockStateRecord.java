/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.block;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class BlockStateRecord {
    private final UUID owner;

    private final String blockData;

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    private final long creationTime = System.currentTimeMillis();

    protected BlockStateRecord(@Nullable UUID owner, String blockData, String worldName, int x, int y, int z) {
        this.owner = owner;

        this.blockData = blockData;

        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Optional<UUID> getOwner() {
        return Optional.ofNullable(owner);
    }

    public String getBlockData() {
        return blockData;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getChunkX() {
        return x >> 4;
    }

    public int getChunkZ() {
        return z >> 4;
    }

    public long getCreationTime() {
        return creationTime;
    }
}
