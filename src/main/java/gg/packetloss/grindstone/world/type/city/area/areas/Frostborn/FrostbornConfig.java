/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Frostborn;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

public class FrostbornConfig extends ConfigurationBase {
    @Setting("loot.chance-of-dupe")
    public double chanceOfDupe = 2;
    @Setting("loot.chance-of-activation")
    public double chanceofActivation = 2;
    @Setting("combat.fountain-origins")
    public int fountainOrigins = 3;
    @Setting("block-restore.time")
    public int timeToRestore = 4;
}
