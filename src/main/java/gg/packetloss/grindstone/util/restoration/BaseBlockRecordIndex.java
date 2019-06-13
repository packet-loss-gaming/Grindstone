/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.restoration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class BaseBlockRecordIndex extends BlockRecordIndex implements Serializable {
    private List<BlockRecord> recordList = new ArrayList<>();

    public void addItem(BlockRecord record) {
        recordList.add(record);
    }

    @Override
    public void revertByTime(long time) {
        revertByTimeWithFilter(time, (ignored) -> true);
    }

    public void revertByTimeWithFilter(long time, Predicate<BlockRecord> test) {
        Iterator<BlockRecord> it = recordList.iterator();
        BlockRecord active;

        while (it.hasNext()) {
            active = it.next();
            if (System.currentTimeMillis() - active.getTime() >= time) {
                if (test.test(active)) {
                    active.revert();
                    it.remove();
                }
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
    public int size() {
        return recordList.size();
    }

    @Override
    public void dropAll() {
        recordList.clear();
    }
}