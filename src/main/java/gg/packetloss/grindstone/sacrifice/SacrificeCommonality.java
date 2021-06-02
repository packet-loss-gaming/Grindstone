/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

enum SacrificeCommonality {
    JUNK(0),
    NORMAL(0),
    RARE_1(.8),
    RARE_2(1.2),
    RARE_3(2),
    RARE_4(2.75),
    RARE_5(3),
    RARE_6(5),
    RARE_7(15),
    RARE_8(120),
    UBER_RARE(10000);

    private final double additionalChance;

    private SacrificeCommonality(double additionalChance) {
        this.additionalChance = additionalChance;
    }

    public double getAdditionalChance() {
        return additionalChance;
    }
}
