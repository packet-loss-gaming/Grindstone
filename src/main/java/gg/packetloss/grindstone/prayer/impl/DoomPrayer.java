/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;

public class DoomPrayer extends AbstractPrayer {

  private static final AbstractPrayer[] SUB_PRAYERS = new AbstractPrayer[] {
      new SlapPrayer(), new PoisonPrayer(), new FakeTNT()
  };

  public DoomPrayer() {

    super(SUB_PRAYERS);
  }

  @Override
  public PrayerType getType() {

    return PrayerType.DOOM;
  }
}
