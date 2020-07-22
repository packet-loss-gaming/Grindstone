/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.custom.item;

import gg.packetloss.grindstone.items.flight.FlightCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class FlightItemActivatedEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private FlightCategory category;

    public FlightItemActivatedEvent(Player who, FlightCategory category) {
        super(who);
        this.category = category;
    }

    public FlightCategory getCategory() {
        return category;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
