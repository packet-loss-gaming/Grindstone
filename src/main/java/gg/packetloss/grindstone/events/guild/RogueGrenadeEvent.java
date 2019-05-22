/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class RogueGrenadeEvent extends RogueEvent implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean cancelled = false;

  private int grenadeCount;

  public RogueGrenadeEvent(Player who, int grenadeCount) {
    super(who);
    this.grenadeCount = grenadeCount;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public int getGrenadeCount() {
    return grenadeCount;
  }

  public void setGrenadeCount(int grenadeCount) {
    this.grenadeCount = grenadeCount;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
