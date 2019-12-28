/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import org.bukkit.entity.Player;

public class NinjaGrappleEvent extends NinjaPowerUseEvent {
    private double maxClimb;

    public NinjaGrappleEvent(Player who, double maxClimb) {
        super(who);
        this.maxClimb = maxClimb;
    }

    public double getMaxClimb() {
        return maxClimb;
    }

    public void setMaxClimb(double maxClimb) {
        this.maxClimb = maxClimb;
    }
}
