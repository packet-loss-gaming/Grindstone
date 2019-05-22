/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.entity.Player;

public class InfiniteHunger extends AbstractPrayer {

  @Override
  public PrayerType getType() {

    return null;
  }

  @Override
  public void add(Player player) {

    player.setFoodLevel(20);
    player.setSaturation(20);
    player.setExhaustion(0);
  }

  @Override
  public void clean(Player player) {

    // Nothing to do here
  }
}
