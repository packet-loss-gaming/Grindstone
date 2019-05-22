/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.entity.Player;

public class StarvationPrayer extends AbstractPrayer {

  @Override
  public PrayerType getType() {

    return PrayerType.STARVATION;
  }

  @Override
  public void add(Player player) {

    if (player.getFoodLevel() > 0) {
      ChatUtil.sendWarning(player, "Tasty...");
      player.setFoodLevel(player.getFoodLevel() - 1);
    }
  }

  @Override
  public void clean(Player player) {

    // Nothing to do here
  }
}
