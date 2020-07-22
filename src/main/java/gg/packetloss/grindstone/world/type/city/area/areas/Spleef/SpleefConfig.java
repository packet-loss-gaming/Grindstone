/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Spleef;

import com.zachsthings.libcomponents.config.ConfigurationBase;

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
}
