/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Spleef;

import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;

import java.util.Set;

public class SpleefConfig extends ConfigurationBase {
    protected Set<String> arenas = Set.of(
            "glacies-mare-district-spleef-large",
            "glacies-mare-district-spleef-medium",
            "glacies-mare-district-spleef-small-1",
            "glacies-mare-district-spleef-small-2",
            "glacies-mare-district-spleef-small-3",
            "glacies-mare-district-spleef-small-4"
    );
    @Setting("anti-camper.ticks-before-activation")
    public int antiCampTicksBeforeActive = 10;
    @Setting("anti-camper.ticks-before-warning")
    public int antiCampTicksBeforeWarning = 4;
    @Setting("anti-camper.shovel-idle-seconds")
    public int antiCampShovelIdleSeconds = 10;
    @Setting("anti-camper.shovel-idle-warn-seconds")
    public int antiCampShovelIdleWarnSeconds = 8;
}
