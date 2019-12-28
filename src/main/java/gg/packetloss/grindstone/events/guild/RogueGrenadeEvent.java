/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class RogueGrenadeEvent extends RoguePowerUseEvent implements Cancellable {
    private int grenadeCount;

    public RogueGrenadeEvent(Player who, int grenadeCount) {
        super(who);
        this.grenadeCount = grenadeCount;
    }

    public int getGrenadeCount() {
        return grenadeCount;
    }

    public void setGrenadeCount(int grenadeCount) {
        this.grenadeCount = grenadeCount;
    }
}
