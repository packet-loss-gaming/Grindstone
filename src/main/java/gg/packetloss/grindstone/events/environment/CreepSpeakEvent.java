/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.environment;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class CreepSpeakEvent extends PlayerEvent implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean cancelled = false;
  private Entity targeter;
  private String message;

  public CreepSpeakEvent(Player player, Entity targeter, String message) {

    super(player);
    this.targeter = targeter;
    this.message = message;
  }

  public static HandlerList getHandlerList() {

    return handlers;
  }

  public Entity getTargeter() {

    return targeter;
  }

  public void setTargeter(Entity targeter) {

    this.targeter = targeter;
  }

  public String getMessage() {

    return message;
  }

  public void setMessage(String message) {

    this.message = message;
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
