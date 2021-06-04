/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.collection;

public class FiniteCache<T> {
    private final T[] items;
    private final int size;

    public FiniteCache(int amount) {
        this.items = (T[]) new Object[amount];
        this.size = amount;
    }

    private int getPosition(T item) {
        int i = 0;
        for (T existingItem : items) {
            if (item.equals(existingItem)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    public boolean contains(T item) {
        return getPosition(item) != -1;
    }

    public void add(T item) {
        int pos = getPosition(item);
        if (pos == 0) {
            return;
        }

        if (pos == -1) {
            // Shift everything to the right by 1
            for (int i = size - 1; i > 0; --i) {
                items[i] = items[i - 1];
            }
        } else {
            // Move everything earlier than pos to the right
            for (int i = pos; i > 0; --i) {
                items[i] = items[i - 1];
            }
        }

        // Update the lead item (the most recently used thing)
        items[0] = item;
    }
}
