/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;

import java.util.List;

public class NinjaArrowBombEvent extends NinjaPowerUseEvent {
    private List<Arrow> arrows;

    public NinjaArrowBombEvent(Player who, List<Arrow> arrows) {
        super(who);
        this.arrows = arrows;
    }

    public List<Arrow> getArrows() {
        return arrows;
    }

    @Override
    public boolean isCancelled() {
        return super.isCancelled() || arrows.isEmpty();
    }
}
