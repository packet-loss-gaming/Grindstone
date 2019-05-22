/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.MirageArena;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class MirageArenaConfig extends ConfigurationBase {
  @Setting("fake-xp-amount")
  public int fakeXP = 100;
  @Setting("gold.cap")
  public int goldCap = 200;
  @Setting("gold.bar-chance")
  public int goldBarChance = 27;
}
