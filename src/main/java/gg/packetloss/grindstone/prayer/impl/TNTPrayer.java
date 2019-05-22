/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TNTPrayer extends AbstractPrayer {

  @Override
  public PrayerType getType() {

    return PrayerType.TNT;
  }

  @Override
  public void add(Player player) {

    Location playerLoc = player.getLocation();
    player.getWorld().createExplosion(playerLoc.getX(), playerLoc.getY(), playerLoc.getZ(), .1F, false, false);
  }

  @Override
  public void clean(Player player) {

    // Nothing to do here
  }
}
