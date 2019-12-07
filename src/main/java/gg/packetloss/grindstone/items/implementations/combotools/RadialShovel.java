/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations.combotools;

import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.implementations.support.RadialExecutor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;

public class RadialShovel extends ComboShovel {
    private static RadialExecutor executor = new RadialExecutor(CustomItems.RADIAL_SHOVEL) {
        @Override
        public boolean accepts(Material material) {
            return acceptedMaterials.contains(material);
        }
    };

    @EventHandler(ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        executor.process(event);
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        return executor.impedeRightClick(event.getPlayer());
    }
}
