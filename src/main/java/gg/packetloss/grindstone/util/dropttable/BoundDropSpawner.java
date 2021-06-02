/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.dropttable;

import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Location;
import org.bukkit.entity.Item;

import java.util.function.Supplier;

public class BoundDropSpawner implements DropProvider {
    private final Supplier<Location> dropDestination;

    public BoundDropSpawner(Supplier<Location> dropDestination1) {
        this.dropDestination = dropDestination1;
    }

    @Override
    public <T extends KillInfo> void provide(DropTable<T> dropTable, T killInfo) {
        dropTable.getDrops(killInfo, (drop) -> {
            Location destination  = dropDestination.get();
            Item item = destination.getWorld().dropItem(destination, drop.getDrop());

            drop.getPlayer().ifPresent(player -> EntityUtil.protectDrop(item, player));
        });
    }
}
