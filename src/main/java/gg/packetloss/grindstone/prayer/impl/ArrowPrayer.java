/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ArrowPrayer extends AbstractPrayer {

  @Override
  public PrayerType getType() {

    return PrayerType.ARROW;
  }

  @Override
  public void add(Player player) {

    Location eyeLoc = player.getEyeLocation();

    eyeLoc.setX(eyeLoc.getX());
    eyeLoc.setY(eyeLoc.getY());
    eyeLoc.setZ(eyeLoc.getZ());
    player.getWorld().spawn(eyeLoc, org.bukkit.entity.Arrow.class);
  }

  @Override
  public void clean(Player player) {

    // Nothing to do here
  }
}
