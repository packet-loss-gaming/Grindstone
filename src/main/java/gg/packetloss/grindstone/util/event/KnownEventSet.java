/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.event;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.event.Event;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class KnownEventSet<T extends Event> {
    private final Deque<T> knownEvents = new ArrayDeque<>();

    public void callEvent(T event) {
        knownEvents.addLast(event);
        try {
            CommandBook.callEvent(event);
        } finally {
            knownEvents.removeLastOccurrence(event);
        }
    }

    public boolean isCalledBySelf(T event) {
        Iterator<T> it = knownEvents.descendingIterator();
        while (it.hasNext()) {
            T possibleMatch = it.next();
            if (possibleMatch == event) {
                return true;
            }
        }
        return false;
    }
}
