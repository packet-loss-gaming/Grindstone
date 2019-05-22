/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events;

import com.sk89q.worldedit.blocks.BlockID;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerSacrificeItemEvent extends PlayerEvent implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private final Block origin;
  private boolean cancelled = false;
  private ItemStack itemStack;


  public PlayerSacrificeItemEvent(final Player player, Block origin, ItemStack itemStack) {

    super(player);
    this.origin = origin;
    this.itemStack = itemStack;
  }

  public static HandlerList getHandlerList() {

    return handlers;
  }

  public Block getBlock() {

    return origin;
  }

  public ItemStack getItemStack() {

    return itemStack.clone();
  }

  public void setItemStack(ItemStack itemStack) {

    if (itemStack == null) {
      itemStack = new ItemStack(BlockID.AIR);
    }

    this.itemStack = itemStack;
  }

  public HandlerList getHandlers() {

    return handlers;
  }

  public boolean isCancelled() {

    return cancelled;
  }

  public void setCancelled(boolean cancelled) {

    this.cancelled = cancelled;
  }
}
