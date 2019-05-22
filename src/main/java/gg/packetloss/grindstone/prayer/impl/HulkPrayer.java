/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HulkPrayer extends AbstractPrayer {

  private static final AbstractPrayer[] SUB_PRAYERS = new AbstractPrayer[] {
      new InfiniteHunger()
  };
  private static PotionEffect[] effects = new PotionEffect[] {
      new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 30, 4),
      new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 30, 4)
  };

  public HulkPrayer() {

    super(SUB_PRAYERS, effects);
  }

  @Override
  public PrayerType getType() {

    return PrayerType.HULK;
  }

  @Override
  public void clean(Player player) {

    super.clean(player);
  }
}