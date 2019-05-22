/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events;

import gg.packetloss.grindstone.prayer.Prayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PrayerApplicationEvent extends PrayerEvent {

  private static final HandlerList handlers = new HandlerList();


  public PrayerApplicationEvent(final Player player, Prayer prayer) {

    super(player, prayer);
  }

  public static HandlerList getHandlerList() {

    return handlers;
  }

  public HandlerList getHandlers() {

    return handlers;
  }
}