/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;

public class MerlinPrayer extends AbstractPrayer {

  private static final AbstractPrayer[] SUB_PRAYERS = new AbstractPrayer[] {
      new FirePrayer(), new BlindnessPrayer(), new SmokeEffectPrayer(), new MushroomPrayer(), new BufferFingersPrayer()
  };

  public MerlinPrayer() {

    super(SUB_PRAYERS);
  }

  @Override
  public PrayerType getType() {

    return PrayerType.MERLIN;
  }
}
