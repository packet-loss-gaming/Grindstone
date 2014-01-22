package com.skelril.aurora.util.restoration;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerMappedBlockRecordIndex extends BlockRecordIndex implements Serializable {

    private Map<String, List<BlockRecord>> recordMap = new ConcurrentHashMap<>();

    public void addItem(String player, BlockRecord record) {

        if (!recordMap.containsKey(player)) {
            recordMap.put(player, new Vector<BlockRecord>());
        }
        recordMap.get(player).add(record);
    }

    @Override
    public void revertByTime(long time) {

        Iterator<List<BlockRecord>> primeIt = recordMap.values().iterator();
        List<BlockRecord> activeRecordList;
        while (primeIt.hasNext()) {
            activeRecordList = primeIt.next();

            Iterator<BlockRecord> it = activeRecordList.iterator();
            BlockRecord activeRecord;
            while (it.hasNext()) {
                activeRecord = it.next();
                if (System.currentTimeMillis() - activeRecord.getTime() >= time) {
                    activeRecord.revert();
                    it.remove();
                }
            }

            if (activeRecordList.isEmpty()) {
                primeIt.remove();
            }
        }
    }

    public boolean hasRecordForPlayer(String player) {

        return recordMap.containsKey(player);
    }

    public void revertByPlayer(String player) {

        if (!hasRecordForPlayer(player)) return;

        List<BlockRecord> activeRecordList = recordMap.get(player);

        if (activeRecordList.isEmpty()) {
            recordMap.remove(player);
            return;
        }

        Iterator<BlockRecord> it = activeRecordList.iterator();
        BlockRecord activeRecord;
        while (it.hasNext()) {
            activeRecord = it.next();
            activeRecord.revert();
            it.remove();
        }

        recordMap.remove(player);
    }

    @Override
    public void revertAll() {

        Iterator<List<BlockRecord>> primeIt = recordMap.values().iterator();
        List<BlockRecord> activeRecordList;
        while (primeIt.hasNext()) {
            activeRecordList = primeIt.next();

            Iterator<BlockRecord> it = activeRecordList.iterator();
            BlockRecord activeRecord;
            while (it.hasNext()) {
                activeRecord = it.next();
                activeRecord.revert();
                it.remove();
            }

            if (activeRecordList.isEmpty()) {
                primeIt.remove();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlayerMappedBlockRecordIndex && recordMap.equals(((PlayerMappedBlockRecordIndex) o).recordMap);
    }

    @Override
    public int size() {

        return recordMap.size();
    }
}
