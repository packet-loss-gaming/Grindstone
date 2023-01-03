/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.apache.commons.lang.Validate;

import java.util.HashMap;
import java.util.Map;

public class RefCountedTracker<T> {
    private Map<T, Integer> counter = new HashMap<>();

    /**
     *
     * @param key
     * @return true if added
     */
    public boolean increment(T key) {
        int newValue = counter.compute(key, (ignored, values) -> {
            if (values == null) {
                values = 0;
            }
            return values + 1;
        });

        return newValue == 1;
    }

    /**
     *
     * @param key
     * @return true if removed
     */
    public boolean decrement(T key) {
        Validate.isTrue(contains(key));
        int count = counter.get(key) - 1;
        if (count == 0) {
            counter.remove(key);
            return true;
        } else {
            counter.put(key, count);
            return false;
        }
    }

    public boolean contains(T key) {
        return counter.containsKey(key);
    }
}
