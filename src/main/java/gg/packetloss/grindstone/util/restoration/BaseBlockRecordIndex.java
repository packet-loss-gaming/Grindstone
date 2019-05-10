/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.restoration;

import java.io.Serializable;
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

        while (it.hasNext()) {
            active = it.next();
            if (System.currentTimeMillis() - active.getTime() >= time) {
                active.revert();
                it.remove();
            }
        }
    }

    @Override
    public void revertAll() {
        Iterator<BlockRecord> it = recordList.iterator();
        BlockRecord active;

        while (it.hasNext()) {
            active = it.next();
            active.revert();
            it.remove();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BaseBlockRecordIndex && recordList.equals(((BaseBlockRecordIndex) o).recordList);
    }

    @Override
    public int size() {

        return recordList.size();
    }
}