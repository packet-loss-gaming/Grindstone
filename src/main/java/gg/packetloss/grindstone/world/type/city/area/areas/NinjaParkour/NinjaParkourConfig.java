/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.NinjaParkour;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class NinjaParkourConfig extends ConfigurationBase {
    @Setting("columns.count")
    public int columnCount = 10;
    @Setting("columns.min-range")
    public int columnMinRange = 1;
    @Setting("columns.max-range")
    public int columnMaxRange = 5;
    @Setting("columns.degrade-chance")
    public int degradeChance = 100;
    @Setting("columns.protected-time")
    public int columnProtectedTime = 15;
    @Setting("xp.new-record-multiplier")
    public int newRecordXpMultiplier = 10;
    @Setting("xp.base-exp")
    public int baseXp = 225;
}
