/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractCondenserImpl;
import gg.packetloss.grindstone.util.ItemCondenser;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class ImbuedCrystalImpl extends AbstractCondenserImpl {

    public ImbuedCrystalImpl(ItemCondenser condenser) {
        super(condenser);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent event) {

        final Player player = event.getPlayer();
        ItemStack itemStack = event.getItem().getItemStack();

        if (condenser.supports(itemStack)) {

            if (!ItemUtil.hasItem(player, CustomItems.IMBUED_CRYSTAL)) {
                return;
            }

            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);
                if (condenser.operate(adapter, true)) {
                    adapter.applyChanges();
                }
            }, 1);
        }
    }
}
