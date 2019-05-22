/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class NinjaGrappleEvent extends NinjaEvent implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean cancelled = false;

  private double maxClimb;

  public NinjaGrappleEvent(Player who, double maxClimb) {
    super(who);
    this.maxClimb = maxClimb;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public double getMaxClimb() {
    return maxClimb;
  }

  public void setMaxClimb(double maxClimb) {
    this.maxClimb = maxClimb;
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
