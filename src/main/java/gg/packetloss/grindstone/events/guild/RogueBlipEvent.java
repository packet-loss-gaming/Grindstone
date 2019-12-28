/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class RogueBlipEvent extends RoguePowerUseEvent implements Cancellable {
    private double modifier;
    private boolean auto;

    public RogueBlipEvent(Player who, double modifier, boolean auto) {
        super(who);
        this.modifier = modifier;
        this.auto = auto;
    }

    public double getModifier() {
        return modifier;
    }

    public void setModifier(double modifier) {
        this.modifier = modifier;
    }

    public boolean isAuto() {
        return auto;
    }
}
