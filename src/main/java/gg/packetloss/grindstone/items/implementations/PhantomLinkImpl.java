/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PhantomLinkImpl extends AbstractItemFeatureImpl {
    private void saveBlockLocation(Player player, ItemStack itemStack, Block clickedBlock) {
        if (!(clickedBlock.getState() instanceof Container container)) {
            ChatUtil.sendError(player, "This link only works with containers.");
            return;
        }

        List<Map.Entry<String, String>> entries = new ArrayList<>();
        entries.add(new AbstractMap.SimpleEntry<>("World", clickedBlock.getWorld().getName()));
        entries.add(new AbstractMap.SimpleEntry<>("X", String.valueOf(clickedBlock.getX())));
        entries.add(new AbstractMap.SimpleEntry<>("Y", String.valueOf(clickedBlock.getY())));
        entries.add(new AbstractMap.SimpleEntry<>("Z", String.valueOf(clickedBlock.getZ())));

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(List.of(ItemUtil.saveLoreKeyValues(entries)));
        itemStack.setItemMeta(itemMeta);

        ChatUtil.sendNotice(player, "Link updated.");
    }

    private Location getBlockLocation(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = itemMeta.getLore();
        if (lore == null || lore.isEmpty()) {
            return null;
        }

        List<Map.Entry<String, String>> values = ItemUtil.loadLoreKeyValues(lore.get(0));
        World world = Bukkit.getWorld(values.get(0).getValue());
        if (world == null) {
            return null;
        }

        int x = Integer.parseInt(values.get(1).getValue());
        int y = Integer.parseInt(values.get(2).getValue());
        int z = Integer.parseInt(values.get(3).getValue());
        return new Location(world, x, y, z);
    }

    public void remoteOpen(Player player, ItemStack itemStack) {
        Location location = getBlockLocation(itemStack);
        if (location == null || !(location.getBlock().getState() instanceof Container container)) {
            ChatUtil.sendError(player, "This link does not point to a valid container.");
            return;
        }

        // Add a chunk ticket in case this inventory is far away
        Chunk chunk = container.getChunk();
        chunk.addPluginChunkTicket(CommandBook.inst());

        player.openInventory(container.getInventory());

        // Setup a task to unregister this chunk ticket ASAP; because the inventory viewing API
        // sucks, simply look for a change in player location.
        Location startLoc = player.getLocation();
        TaskBuilder.Countdown builder = TaskBuilder.countdown();
        builder.setAction((times) -> {
            return player.getLocation().distanceSquared(startLoc) > Math.pow(3, 2);
        });
        builder.setFinishAction(() -> chunk.removePluginChunkTicket(CommandBook.inst()));
        builder.build();
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (ItemUtil.isHoldingItem(player, CustomItems.PHANTOM_LINK)) {
            remoteOpen(player, player.getItemInHand());
        }

        return false;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (ItemUtil.isHoldingItem(player, CustomItems.PHANTOM_LINK)) {
            saveBlockLocation(player, player.getItemInHand(), event.getClickedBlock());
        }
    }
}
