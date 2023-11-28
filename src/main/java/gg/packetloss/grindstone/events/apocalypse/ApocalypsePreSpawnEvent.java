/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class ApocalypsePreSpawnEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Location initialStrikePoint;
    private final List<Location> strikePoints;

    public ApocalypsePreSpawnEvent(Location initialStrikePoint, List<Location> strikePoints) {
        this.initialStrikePoint = initialStrikePoint;
        this.strikePoints = strikePoints;
    }

    public Location getInitialLightningStrikePoint() {
        return initialStrikePoint.clone();
    }

    public List<Location> getLightningStrikePoints() {
        return strikePoints;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
