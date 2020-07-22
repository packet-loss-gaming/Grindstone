/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items;

import gg.packetloss.grindstone.items.specialattack.SpecType;

public enum WeaponType {
    MELEE(SpecType.MELEE),
    RANGED(SpecType.RANGED);

    private final SpecType specType;

    WeaponType(SpecType specType) {
        this.specType = specType;
    }

    public SpecType getDefaultSpecType() {
        return specType;
    }
}
