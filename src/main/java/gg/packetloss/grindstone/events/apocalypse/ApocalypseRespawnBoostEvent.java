/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class ApocalypseRespawnBoostEvent extends PlayerEvent implements ApocalypseEvent {
  private static final HandlerList handlers = new HandlerList();

  private final Location respawnLocation;
  private boolean cancelled = false;

  public ApocalypseRespawnBoostEvent(final Player player, Location respawnLocation) {
    super(player);
    this.respawnLocation = respawnLocation;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  /**
   * @return the location the player is respawning
   */
  @Override
  public Location getLocation() {
    return respawnLocation;
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
