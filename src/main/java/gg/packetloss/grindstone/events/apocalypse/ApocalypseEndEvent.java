/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ApocalypseEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    public ApocalypseEndEvent() {
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
