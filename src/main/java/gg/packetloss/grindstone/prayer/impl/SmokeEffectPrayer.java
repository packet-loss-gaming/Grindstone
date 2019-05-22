/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SmokeEffectPrayer extends AbstractPrayer {

  @Override
  public PrayerType getType() {

    return PrayerType.SMOKE;
  }

  @Override
  public void add(Player player) {

    Location[] smoke = new Location[2];
    smoke[0] = player.getLocation();
    smoke[1] = player.getEyeLocation();
    EnvironmentUtil.generateRadialEffect(smoke, Effect.SMOKE);
  }

  @Override
  public void clean(Player player) {

    // Nothing to do here
  }
}
