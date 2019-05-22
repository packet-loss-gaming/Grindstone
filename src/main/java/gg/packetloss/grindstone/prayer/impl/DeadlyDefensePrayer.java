/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.ArrowUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DeadlyDefensePrayer extends AbstractPrayer {

  @Override
  public PrayerType getType() {

    return PrayerType.DEADLYDEFENSE;
  }

  @Override
  public void add(Player player) {

    short arrow = 0;
    for (Entity entity : player.getNearbyEntities(8, 3, 8)) {

      if (arrow > 10) {
        break;
      }
      if (EnvironmentUtil.isHostileEntity(entity)) {
        if (!player.hasLineOfSight(entity)) {
          arrow--;
          continue;
        }
        ArrowUtil.shootArrow(player, (LivingEntity) entity, 1.6F, 0F);
      }
    }
  }

  @Override
  public void clean(Player player) {

    // Nothing to do here
  }
}
