/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DiggyDiggyPrayer extends AbstractPrayer {

  private static final PotionEffect EFFECT = new PotionEffect(PotionEffectType.FAST_DIGGING, 20 * 600, 1);

  public DiggyDiggyPrayer() {

    super(null, EFFECT);
  }

  @Override
  public PrayerType getType() {

    return PrayerType.DIGGYDIGGY;
  }
}
