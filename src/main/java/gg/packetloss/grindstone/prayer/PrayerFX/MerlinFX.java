/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.PrayerFX;

import gg.packetloss.grindstone.prayer.PrayerType;

public class MerlinFX extends AbstractEffect {

    private static final AbstractEffect[] subFX = new AbstractEffect[]{
            new FireFX(), new BlindnessFX(), new SmokeFX(), new MushroomFX(), new ButterFingersFX()
    };

    public MerlinFX() {

        super(subFX);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.MERLIN;
    }
}
