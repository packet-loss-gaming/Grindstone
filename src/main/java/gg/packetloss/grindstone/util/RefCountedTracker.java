/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import java.util.HashMap;
import java.util.Map;

public class RefCountedTracker<T> {
    private Map<T, Integer> counter = new HashMap<>();

    public void increment(T key) {
        counter.putIfAbsent(key, 0);
        counter.put(key, counter.get(key) + 1);
    }

    public void decrement(T key) {
        int count = counter.get(key) - 1;
        if (count == 0) {
            counter.remove(key);
        } else {
            counter.put(key, count);
        }
    }

    public boolean contains(T key) {
        return counter.containsKey(key);
    }
}
