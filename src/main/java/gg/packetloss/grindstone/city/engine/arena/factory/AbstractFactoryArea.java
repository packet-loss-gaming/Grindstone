/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena.factory;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.city.engine.arena.AbstractRegionedArena;
import gg.packetloss.grindstone.util.CollectionUtil;
import org.bukkit.World;

public class AbstractFactoryArea extends AbstractRegionedArena {

    protected ProtectedRegion potionChamber;
    protected ProtectedRegion[] smeltingChamber;

    public AbstractFactoryArea(World world, ProtectedRegion region,
                               ProtectedRegion potionChamber, ProtectedRegion[] smeltingChamer) {
        super(world, region);
        this.potionChamber = potionChamber;
        this.smeltingChamber = smeltingChamer;
    }

    public ProtectedRegion getChamber(ChamberType type) {
        switch (type) {
            case POTION:
                return potionChamber;
            case SMELTING:
                return CollectionUtil.getElement(smeltingChamber);
        }
        return null;
    }

    protected enum ChamberType {
        POTION,
        SMELTING
    }
}
