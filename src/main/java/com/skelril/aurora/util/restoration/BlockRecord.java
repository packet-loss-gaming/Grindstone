package com.skelril.aurora.util.restoration;

import com.sk89q.worldedit.blocks.BaseBlock;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class BlockRecord implements Comparable<BlockRecord> {

    final Location location;
    final BaseBlock blockData;
    final long time;

    public BlockRecord(Block block) {

        this.location = block.getLocation();
        this.blockData = new BaseBlock(block.getTypeId(), block.getData());
        this.time = System.currentTimeMillis();
    }

    public BlockRecord(Location location, BaseBlock blockData) {

        this.location = location.clone();
        this.blockData = blockData;
        this.time = System.currentTimeMillis();
    }

    public long getTime() {

        return time;
    }

    public void revert() {

        Chunk chunk = location.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }

        location.getBlock().setTypeIdAndData(blockData.getType(), (byte) blockData.getData(), true);
    }

    // Oldest to newest
    @Override
    public int compareTo(BlockRecord record) {

        if (record == null) return -1;

        if (this.getTime() == record.getTime()) return 0;
        if (this.getTime() > record.getTime()) return 1;
        return -1;
    }
}
