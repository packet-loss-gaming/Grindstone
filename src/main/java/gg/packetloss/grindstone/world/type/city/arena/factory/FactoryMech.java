/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena.factory;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.world.type.city.arena.AbstractRegionedArena;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class FactoryMech extends AbstractRegionedArena {
    protected boolean dirty = false;

    public FactoryMech(World world, ProtectedRegion region) {
        super(world, region);
    }

    public abstract String getName();

    public abstract void consume();

    public abstract List<ItemStack> produceUpTo(int amount);

    public abstract void load();

    protected abstract void saveImpl();

    public void save() {
        if (!dirty) {
            return;
        }

        dirty = false;
        saveImpl();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }
}
