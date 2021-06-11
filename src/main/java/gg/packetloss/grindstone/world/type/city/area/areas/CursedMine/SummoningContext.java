/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.CursedMine;

import org.bukkit.Material;

public class SummoningContext {
    private final Material brokenBlock;
    private final boolean isEvil;

    public SummoningContext(Material brokenBlock, boolean isEvil) {
        this.brokenBlock = brokenBlock;
        this.isEvil = isEvil;
    }

    public Material getBrokenBlock() {
        return brokenBlock;
    }

    public boolean isEvil() {
        return isEvil;
    }
}
