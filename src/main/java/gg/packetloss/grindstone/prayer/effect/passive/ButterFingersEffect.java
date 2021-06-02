/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.DumpPlayerInventoryEvent;
import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ButterFingersEffect implements PassivePrayerEffect {
    @Override
    public void trigger(Player player) {
        DumpPlayerInventoryEvent event = new DumpPlayerInventoryEvent(player);
        CommandBook.server().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        PlayerInventory pInv = player.getInventory();
        Location pLoc = player.getLocation();
        ItemStack[] stacks = pInv.getContents();

        for (int i = 0; i < stacks.length; ++i) {
            ItemStack stack = stacks[i];
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }

            player.getWorld().dropItem(pLoc, stack);
            stacks[i] = null;
        }

        pInv.setContents(stacks);
        player.updateInventory();
    }

    @Override
    public void strip(Player player) { }
}
