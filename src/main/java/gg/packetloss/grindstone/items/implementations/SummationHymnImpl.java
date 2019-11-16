/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.items.generic.AbstractCondenserImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ItemCondenser;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class SummationHymnImpl extends AbstractCondenserImpl {

    public SummationHymnImpl(ItemCondenser condenser) {
        super(condenser);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        Player player = event.getPlayer();
        HymnSingEvent.Hymn hymn = event.getHymn();

        switch (hymn) {
            case SUMMATION:
                InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);
                if (condenser.operate(adapter, true)) {
                    adapter.applyChanges();
                    ChatUtil.sendNotice(player, ChatColor.GOLD, "The hymn glows brightly...");
                }
                break;
        }
    }
}
