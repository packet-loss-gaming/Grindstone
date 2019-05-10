/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area;

import org.bukkit.event.Listener;

public abstract class AreaListener<Area extends AreaComponent<?>> implements Listener {

    protected final Area parent;

    public AreaListener(Area parent) {
        this.parent = parent;
    }
}
