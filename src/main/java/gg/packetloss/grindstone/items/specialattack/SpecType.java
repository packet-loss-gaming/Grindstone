/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack;

public enum SpecType {

    RED_FEATHER(1000),
    RANGED(3800),
    MELEE(3800),
    ANIMAL_BOW(15000),
    PASSIVE(0);

    private final long delay;

    private SpecType(long delay) {

        this.delay = delay;
    }

    public long getDelay() {

        return delay;
    }
}