/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events;

import gg.packetloss.grindstone.prayer.Prayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;

public abstract class PrayerEvent extends PlayerEvent implements Cancellable {

    private boolean cancelled = false;
    private final Prayer prayer;

    public PrayerEvent(final Player player, Prayer prayer) {

        super(player);
        this.prayer = prayer;
    }

    public Prayer getCause() {

        return prayer;
    }

    public boolean isCancelled() {

        return cancelled;
    }

    public void setCancelled(boolean cancelled) {

        this.cancelled = cancelled;
    }

}
