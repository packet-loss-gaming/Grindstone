package gg.packetloss.grindstone.state.block;

import org.bukkit.util.Consumer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class BlockStateCollection {
    private List<BlockStateRecord> blocks = new ArrayList<>();
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

    public void popAll(Consumer<BlockStateRecord> consumer) {
        if (blocks.isEmpty()) {
            return;
        }

        blocks.forEach(consumer::accept);
        blocks.clear();

        dirty = true;
    }

    public void popWhere(Predicate<BlockStateRecord> predicate, Consumer<BlockStateRecord> consumer) {
        Iterator<BlockStateRecord> it = blocks.iterator();

        while (it.hasNext()) {
            BlockStateRecord record = it.next();
            if (predicate.test(record)) {
                consumer.accept(record);
                it.remove();

                dirty = true;
            }
        }
    }
}
