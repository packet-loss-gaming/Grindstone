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
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class SummationScrollImpl extends AbstractCondenserImpl {

  public SummationScrollImpl(ItemCondenser condenser) {
    super(condenser);
  }

  @EventHandler(ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {

    Player player = event.getPlayer();
    ItemStack itemStack = event.getItem();

    if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
      // Scrolls
      boolean isScrollOfSummation = ItemUtil.isItem(itemStack, CustomItems.SCROLL_OF_SUMMATION);
      if (isScrollOfSummation) {
        ItemStack[] result = condenser.operate(player.getInventory().getContents(), false);
        if (result != null) {
          player.getInventory().setContents(result);
          ItemUtil.removeItemOfName(player, CustomItemCenter.build(CustomItems.SCROLL_OF_SUMMATION), 1, false);
          ChatUtil.sendNotice(player, ChatColor.GOLD, "The scroll glows brightly before turning to dust...");
        }
      }
    }
  }
}
