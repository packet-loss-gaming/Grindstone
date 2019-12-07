/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractCondenserImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ItemCondenser;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class SummationScrollImpl extends AbstractCondenserImpl {

    public SummationScrollImpl(ItemCondenser condenser) {
        super(condenser);
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (ItemUtil.isHoldingItem(player, CustomItems.SCROLL_OF_SUMMATION)) {
            InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);
            if (condenser.operate(adapter, false)) {
                adapter.applyChanges();
                ItemUtil.removeItemOfName(player, CustomItemCenter.build(CustomItems.SCROLL_OF_SUMMATION), 1, false);
                ChatUtil.sendNotice(player, ChatColor.GOLD, "The scroll glows brightly before turning to dust...");
            }
            return true;
        }

        return false;
    }
}
