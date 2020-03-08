/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.implementations.support.AbsorbArmor;
import gg.packetloss.grindstone.items.implementations.support.Necrosis;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class NectricArmorImpl extends AbstractItemFeatureImpl {
    public boolean hasArmor(Player player) {
        return ItemUtil.hasNectricArmour(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void damageReduction(EntityDamageEvent event) {
        new AbsorbArmor(this::hasArmor, 20, 10, 2).handleEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void necrosis(EntityDamageByEntityEvent event) {
        new Necrosis(prayers, this::hasArmor).handleEvent(event);
    }
}
