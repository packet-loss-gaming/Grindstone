/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.NinjaParkour;

public class NinjaParkourPlayerState {
    private long startTime = System.currentTimeMillis();

    public long getStartTime() {
        return startTime;
    }

    public long getElapsedTime(long fromTime) {
        return fromTime - getStartTime();
    }

    public long getElapsedTime() {
        return getElapsedTime(System.currentTimeMillis());
    }
}
