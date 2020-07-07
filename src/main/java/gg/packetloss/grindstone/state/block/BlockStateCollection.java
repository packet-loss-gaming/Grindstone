package gg.packetloss.grindstone.state.block;

import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

public class BlockStateCollection {
    private List<BlockStateRecord> blocks = new ArrayList<>();
    private List<BlockStateRecord> inFlightBlocks = new ArrayList<>();
    private transient boolean dirty = false;

    public boolean isDirty() {
        return dirty;
    }

    public void resetDirtyFlag() {
        dirty = false;
    }

    public void push(BlockStateRecord record) {
        blocks.add(record);
        dirty = true;
    }

    public boolean hasRecordMatching(Predicate<BlockStateRecord> predicate) {
        for (BlockStateRecord record : blocks) {
            if (predicate.test(record)) {
                return true;
            }
        }
        return false;
    }

    public void dropAll() {
        if (blocks.isEmpty()) {
            return;
        }

        blocks.clear();

        dirty = true;
    }

    private void pop(BlockStateRecord record, Function<BlockStateRecord, CompletableFuture<Boolean>> restoreOp) {
        inFlightBlocks.add(record);
        restoreOp.apply(record).thenAccept((result) -> {
            Validate.isTrue(result);
            inFlightBlocks.remove(record);

            dirty = true;
        });
    }

    public void popAll(Function<BlockStateRecord, CompletableFuture<Boolean>> restoreOp) {
        if (blocks.isEmpty()) {
            return;
        }

        blocks.forEach((record) -> pop(record, restoreOp));
        blocks.clear();

        dirty = true;
    }

    public void popWhere(Predicate<BlockStateRecord> predicate, Function<BlockStateRecord, CompletableFuture<Boolean>> restoreOp) {
        Iterator<BlockStateRecord> it = blocks.iterator();

        while (it.hasNext()) {
            BlockStateRecord record = it.next();
            if (predicate.test(record)) {
                pop(record, restoreOp);
                it.remove();

                dirty = true;
            }
        }
    }
}
