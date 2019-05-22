/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FlashPrayer extends AbstractPrayer {

  private static final AbstractPrayer[] SUB_PRAYERS = new AbstractPrayer[] {
      new InfiniteHunger()
  };
  private static final PotionEffect EFFECT = new PotionEffect(PotionEffectType.SPEED, 20 * 600, 6);

  public FlashPrayer() {

    super(SUB_PRAYERS, EFFECT);
  }

  @Override
  public PrayerType getType() {

    return PrayerType.FLASH;
  }
}
