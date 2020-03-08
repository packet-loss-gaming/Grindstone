/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.BloodLustArmor;
import gg.packetloss.grindstone.items.implementations.support.AbsorbArmor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

public class AncientArmorImpl extends BloodLustArmor {
    @Override
    public boolean hasArmor(Player player) {
        return ItemUtil.hasAncientArmour(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void damageReduction(EntityDamageEvent event) {
        new AbsorbArmor(ItemUtil::hasAncientRoyalArmour, 20, 30, 1).handleEvent(event);
    }
}
