/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.CursedMine;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class Haunting {
    private final Ghost ghost;
    private final Consumer<Player> action;

    protected Haunting(Ghost ghost, Consumer<Player> action) {
        this.ghost = ghost;
        this.action = action;
    }

    public Ghost getGhost() {
        return ghost;
    }

    public Consumer<Player> getAction() {
        return action;
    }
}
