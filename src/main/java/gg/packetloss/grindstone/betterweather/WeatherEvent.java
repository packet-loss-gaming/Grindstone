/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.betterweather;

public class WeatherEvent {
    private long activationTime;
    private WeatherType weatherType;

    public WeatherEvent(long activationTime, WeatherType weatherType) {
        this.activationTime = activationTime;
        this.weatherType = weatherType;
    }

    public long getActivationTime() {
        return activationTime;
    }

    public boolean shouldActivate() {
        return System.currentTimeMillis() - activationTime >= 0;
    }

    public WeatherType getWeatherType() {
        return weatherType;
    }

    public void setWeatherType(WeatherType weatherType) {
        this.weatherType = weatherType;
    }
}
