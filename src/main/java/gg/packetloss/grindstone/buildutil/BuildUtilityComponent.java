/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.buildutil;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ItemPointTranslator;
import gg.packetloss.grindstone.util.item.inventory.PlayerHeldItemThenStoragePriorityInventoryAdapter;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ComponentInformation(friendlyName = "Build Utility", desc = "General build utilities.")
public class BuildUtilityComponent extends BukkitComponent implements Listener {
    private static record PlacedBlockInfo(EquipmentSlot hand, ItemStack placedItem) { }

    private final Map<UUID, DebounceHandle<PlacedBlockInfo>> itemPlaceDebounce = new HashMap<>();

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    private boolean shouldMoveItemsToHand(ItemStack currentlyHeldItem, Material targetMaterial) {
        if (currentlyHeldItem == null) {
            return true;
        }

        if (currentlyHeldItem.getType() !=  targetMaterial) {
            return true;
        }

        if (currentlyHeldItem.getAmount() < targetMaterial.getMaxStackSize()) {
            return true;
        }

        return false;
    }

    private void createNewHandle(UUID playerID, PlacedBlockInfo placedBlockInfo) {
        TaskBuilder.Debounce<PlacedBlockInfo> builder = TaskBuilder.debounce();
        builder.setWaitTime(1);

        builder.setInitialValue(placedBlockInfo);
        builder.setUpdateFunction((oldPBI, newPBI) -> newPBI);

        builder.setBounceAction((finalPlacedBlockInfo) -> {
            Player player = Bukkit.getPlayer(playerID);
            if (player != null) {
                PlayerInventory inventory = player.getInventory();
                Material targetItemType = finalPlacedBlockInfo.placedItem().getType();
                if (shouldMoveItemsToHand(inventory.getItem(finalPlacedBlockInfo.hand()), targetItemType)) {
                    var adapter = new PlayerHeldItemThenStoragePriorityInventoryAdapter(
                        player,
                        finalPlacedBlockInfo.hand()
                    );

                    ItemPointTranslator beanCounter = new ItemPointTranslator();
                    beanCounter.addMapping(new ItemStack(targetItemType), 1);

                    // Rebuild the inventory
                    beanCounter.assignValue(adapter, beanCounter.calculateValue(adapter, true));

                    // Apply any changes
                    adapter.applyChanges();
                }
            }

            itemPlaceDebounce.remove(playerID);
        });

        DebounceHandle<PlacedBlockInfo> newHandle = builder.build();
        newHandle.accept(placedBlockInfo);
        itemPlaceDebounce.put(playerID, newHandle);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBlockPlace(BlockPlaceEvent event) {
        UUID playerID = event.getPlayer().getUniqueId();
        PlacedBlockInfo placedBlockInfo = new PlacedBlockInfo(event.getHand(), event.getItemInHand().clone());

        DebounceHandle<PlacedBlockInfo> existingHandle = itemPlaceDebounce.get(playerID);
        if (existingHandle != null) {
            existingHandle.accept(placedBlockInfo);
            return;
        }

        createNewHandle(playerID, placedBlockInfo);
    }
}
