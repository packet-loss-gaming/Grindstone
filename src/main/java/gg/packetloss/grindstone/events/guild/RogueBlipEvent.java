/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

public class RogueBlipEvent extends RogueEvent implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean cancelled = false;

  private double modifier;
  private boolean auto;

  public RogueBlipEvent(Player who, double modifier, boolean auto) {
    super(who);
    this.modifier = modifier;
    this.auto = auto;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public double getModifier() {
    return modifier;
  }

  public void setModifier(double modifier) {
    this.modifier = modifier;
  }

  public boolean isAuto() {
    return auto;
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
