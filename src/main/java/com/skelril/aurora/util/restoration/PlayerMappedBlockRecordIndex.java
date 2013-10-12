package com.skelril.aurora.util.restoration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class PlayerMappedBlockRecordIndex extends BlockRecordIndex {

    private HashMap<String, List<BlockRecord>> recordMap = new HashMap<>();

    public void addItem(String player, BlockRecord record) {

        if (!recordMap.containsKey(player)) {
            recordMap.put(player, new ArrayList<BlockRecord>());
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
    public int size() {

        return recordMap.size();
    }
}
