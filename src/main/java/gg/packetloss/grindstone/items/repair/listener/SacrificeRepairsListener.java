/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.repair.listener;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import gg.packetloss.grindstone.events.custom.item.RepairItemEvent;
import gg.packetloss.grindstone.items.repair.profile.RepairProfile;
import gg.packetloss.grindstone.items.repair.profile.SacrificeItemRepairProfile;
import gg.packetloss.grindstone.items.repair.profile.SacrificeRepairProfile;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.ItemPointTranslator;
import gg.packetloss.grindstone.util.item.inventory.InventoryAdapter;
import gg.packetloss.grindstone.util.item.inventory.PlayerStoragePriorityInventoryAdapter;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SacrificeRepairsListener implements Listener {
    private List<RepairProfile> repairProfiles;

    public SacrificeRepairsListener(List<RepairProfile> repairProfiles) {
        this.repairProfiles = repairProfiles;
    }

    private void returnItemToPlayer(Player player, ItemStack item) {
        ItemStack remainder = player.getInventory().addItem(item).get(0);
        if (remainder != null) {
            EntityUtil.spawnProtectedItem(remainder, player);
        }
    }

    private void handleSacrificeRepair(Player player, ItemStack item, SacrificeRepairProfile repairProfile) {
        RepairItemEvent repairEvent = new RepairItemEvent(player, item, repairProfile.getRepairPercentage());
        CommandBook.callEvent(repairEvent);

        float repairPercentage = repairEvent.getRepairPercentage();

        int maxDurability = item.getType().getMaxDurability();
        int maxRepair = (int) (maxDurability * (1 - repairPercentage));
        item.setDurability((short) Math.min(item.getDurability(), maxRepair));

        returnItemToPlayer(player, item);
        player.updateInventory();
    }

    private void handleSacrificeItemRepair(Player player, ItemStack item, SacrificeItemRepairProfile repairProfile) {
        RepairItemEvent repairEvent = new RepairItemEvent(player, item, repairProfile.getRepairPercentage());
        CommandBook.callEvent(repairEvent);

        float repairPercentage = repairEvent.getRepairPercentage();

        // Setup counting for this repair.
        InventoryAdapter adapter = new PlayerStoragePriorityInventoryAdapter(player);

        ItemPointTranslator converter = new ItemPointTranslator();
        converter.addMapping(repairProfile.getRepairItem(), 1);

        // Count the number of repair items in the inventory.
        int repairItemCount = converter.calculateValue(adapter, true);

        // Figure out how much we can repair.
        int maxDurability = item.getType().getMaxDurability();
        short remainingDurability = item.getDurability();
        while (repairItemCount > 0 && remainingDurability > 0) {
            // Remove a repair item
            --repairItemCount;

            remainingDurability = (short) Math.max(0, remainingDurability - (repairPercentage * maxDurability));
        }

        item.setDurability(remainingDurability);
        returnItemToPlayer(player, item);

        // Set the new inventory value.
        int remainingValue = converter.assignValue(adapter, repairItemCount);
        Validate.isTrue(remainingValue == 0, "Failed to return remaining repair items!");

        // Update inventory state.
        adapter.applyChanges();
        player.updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSacrifice(PlayerSacrificeItemEvent event) {
        ItemStack item = event.getItemStack();

        for (RepairProfile repairProfile : repairProfiles) {
            if (!repairProfile.matches(item)) {
                continue;
            }

            if (repairProfile instanceof SacrificeRepairProfile) {
                handleSacrificeRepair(event.getPlayer(), item, (SacrificeRepairProfile) repairProfile);
                event.setItemStack(null);
                return;
            }

            if (repairProfile instanceof SacrificeItemRepairProfile) {
                handleSacrificeItemRepair(event.getPlayer(), item, (SacrificeItemRepairProfile) repairProfile);
                event.setItemStack(null);
                return;
            }
        }
    }
}
