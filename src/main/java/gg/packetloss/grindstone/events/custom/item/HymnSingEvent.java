/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.custom.item;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class HymnSingEvent extends PlayerEvent implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean cancelled = false;
  private Hymn hymn;

  public HymnSingEvent(Player who, Hymn hymn) {
    super(who);
    this.hymn = hymn;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public Hymn getHymn() {
    return hymn;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  public enum Hymn {
    PHANTOM,
    CHICKEN,
    SUMMATION
  }
}
