/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.betterweather;

public enum WeatherType {
    CLEAR(false, false),
    RAIN(true, false),
    THUNDERSTORM(true, true);

    private boolean storming;
    private boolean thundering;

    private WeatherType(boolean storming, boolean thundering) {
        this.storming = storming;
        this.thundering = thundering;
    }

    public boolean isStorm() {
        return storming;
    }

    public boolean hasThunder() {
        return thundering;
    }
}
