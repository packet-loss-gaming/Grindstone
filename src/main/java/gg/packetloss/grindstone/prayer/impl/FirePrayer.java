/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;

public class FirePrayer extends AbstractPrayer {

  @Override
  public PrayerType getType() {

    return PrayerType.FIRE;
  }

  @Override
  public void add(Player player) {

    if (player.getFireTicks() < 20) {
      ChatUtil.sendWarning(player, "BURN!!!");
      player.setFireTicks((20 * 60));
    }
  }

  @Override
  public void clean(Player player) {

    // Nothing to do here
  }
}
