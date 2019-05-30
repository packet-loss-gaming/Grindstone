package gg.packetloss.grindstone.util.restoration;

import com.sk89q.commandbook.CommandBook;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DoubleBufferedBaseBlockRecordIndex extends BlockRecordIndex implements Serializable {
    private List<BlockRecord> recordList = new ArrayList<>();
    private List<BlockRecord> persistedBuffer = new ArrayList<>();

    public void flushPersistedBuffer() {
        reprocessBuffer(new ArrayList<>(this.persistedBuffer));
    }

    public void addItem(BlockRecord record) {
        recordList.add(record);
    }

    @Override
    public void revertByTime(long time) {

        Iterator<BlockRecord> it = recordList.iterator();
        BlockRecord active;

        List<BlockRecord> runtimeBuffer = new ArrayList<>();

        while (it.hasNext()) {
            active = it.next();
            if (System.currentTimeMillis() - active.getTime() >= time) {
                // Revert the block
                active.revert();

                // Add to the secondary buffer
                runtimeBuffer.add(active);
                persistedBuffer.add(active);

                // Remove the block from the list of restorations
                it.remove();
            }
        }

        reprocessBuffer(runtimeBuffer);
    }

    @Override
    public void revertAll() {
        Iterator<BlockRecord> it = recordList.iterator();
        BlockRecord active;

        List<BlockRecord> doubleBuffer = new ArrayList<>();

        while (it.hasNext()) {
            // Revert the block
            active = it.next();
            active.revert();

            // Add to the secondary buffer
            doubleBuffer.add(active);

            // Remove the block from the list of restorations
            it.remove();
        }

        reprocessBuffer(doubleBuffer);
    }

    @Override
    public int size() {
        return recordList.size();
    }

    private void reprocessBufferInternal(List<BlockRecord> buffer, int tickDelay) {
        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            for (BlockRecord blockRecord : buffer) {
                blockRecord.revert();
                persistedBuffer.remove(blockRecord);
            }
        }, tickDelay);
    }

    private void reprocessBuffer(List<BlockRecord> buffer) {
        reprocessBufferInternal(buffer, 1);
    }
}
