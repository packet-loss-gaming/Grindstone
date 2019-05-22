/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.impl;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import gg.packetloss.grindstone.events.DumpPlayerInventoryEvent;
import gg.packetloss.grindstone.prayer.PrayerType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BufferFingersPrayer extends AbstractPrayer {

  @Override
  public PrayerType getType() {

    return PrayerType.BUTTERFINGERS;
  }

  @Override
  public void add(Player player) {
    DumpPlayerInventoryEvent event = new DumpPlayerInventoryEvent(player);
    CommandBook.server().getPluginManager().callEvent(event);
    if (event.isCancelled()) {
      return;
    }

    for (ItemStack itemStack : player.getInventory().getArmorContents()) {
      if (itemStack != null && itemStack.getTypeId() != BlockID.AIR) {
        player.getWorld().dropItem(player.getLocation(), itemStack.clone());
      }
    }
    for (ItemStack itemStack : player.getInventory().getContents()) {
      if (itemStack != null && itemStack.getTypeId() != BlockID.AIR) {
        player.getWorld().dropItem(player.getLocation(), itemStack.clone());
      }
    }

    player.getInventory().setArmorContents(null);
    player.getInventory().clear();
  }

  @Override
  public void clean(Player player) {

    // Nothing to do here
  }
}
