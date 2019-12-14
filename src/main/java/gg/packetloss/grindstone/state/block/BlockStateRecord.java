package gg.packetloss.grindstone.state.block;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class BlockStateRecord {
    private final UUID owner;

    private final String blockType;

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    private final long creationTime = System.currentTimeMillis();

    protected BlockStateRecord(@Nullable UUID owner, String blockType, String worldName, int x, int y, int z) {
        this.owner = owner;

        this.blockType = blockType;

        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Optional<UUID> getOwner() {
        return Optional.ofNullable(owner);
    }

    public String getBlockType() {
        return blockType;
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

    public long getCreationTime() {
        return creationTime;
    }
}
