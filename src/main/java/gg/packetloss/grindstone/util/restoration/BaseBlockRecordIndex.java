/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.restoration;

import com.sk89q.commandbook.CommandBook;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class BaseBlockRecordIndex extends BlockRecordIndex implements Serializable {

    private List<BlockRecord> recordList = new Vector<>();

    public void addItem(BlockRecord record) {

        recordList.add(record);
    }

    @Override
    public void revertByTime(long time) {

        Iterator<BlockRecord> it = recordList.iterator();
        BlockRecord active;

        List<BlockRecord> doubleBuffer = new ArrayList<>();

        while (it.hasNext()) {
            active = it.next();
            if (System.currentTimeMillis() - active.getTime() >= time) {
                // Revert the block
                active.revert();

                // Add to the secondary buffer
                doubleBuffer.add(active);

                // Remove the block from the list of restorations
                it.remove();
            }
        }

        reprocessBuffer(doubleBuffer);
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
    public boolean equals(Object o) {
        return o instanceof BaseBlockRecordIndex && recordList.equals(((BaseBlockRecordIndex) o).recordList);
    }

    @Override
    public int size() {

        return recordList.size();
    }

    private void reprocessBuffer(List<BlockRecord> buffer) {
        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            for (BlockRecord blockRecord : buffer) {
                blockRecord.revert();
            }
        }, 1);
    }
}