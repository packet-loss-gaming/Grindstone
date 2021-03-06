/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.SandArena;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class SandArenaConfig extends ConfigurationBase {
    @Setting("fake-xp-amount")
    public int fakeXP = 100;
    @Setting("increase-rate")
    public int increaseRate = 8;
    @Setting("decrease-rate")
    public int decreaseRate = 16;
}
