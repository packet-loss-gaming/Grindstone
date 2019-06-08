/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.generic.BloodLustArmor;
import gg.packetloss.grindstone.items.implementations.support.Necrosis;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class NecrosArmorImpl extends BloodLustArmor {
    @Override
    public boolean hasArmor(Player player) {
        return ItemUtil.hasNecrosArmour(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void necrosis(EntityDamageByEntityEvent event) {
        new Necrosis(prayers).handleEvent(event);
    }
}
