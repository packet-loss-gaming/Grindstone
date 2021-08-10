/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

enum SacrificeCommonality {
    JUNK(0),
    NORMAL(0),
    RARE_1(500),
    RARE_2(1000),
    RARE_3(2000),
    RARE_4(5000),
    RARE_5(12000),
    RARE_6(16000),
    RARE_7(50000),
    RARE_8(750000),
    UBER_RARE(1000000);

    private final double additionalChance;

    private SacrificeCommonality(double additionalChance) {
        this.additionalChance = additionalChance;
    }

    public double getAdditionalChance() {
        return additionalChance;
    }
}
