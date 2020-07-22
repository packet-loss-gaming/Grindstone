/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.skywars;

public class SkyWarsProfile {
    private SkyWarsTeam team = null;
    private int points = 0;

    public SkyWarsProfile() { }

    public SkyWarsTeam getTeam() {
        return team;
    }

    public void setTeam(SkyWarsTeam team) {
        this.team = team;
    }

    public int getPoints() {
        return points;
    }

    public void adjustPoints(int points) {
        this.points += points;

        // You must have at least 0 points
        if (this.points < 0) {
            this.points = 0;
        }
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
