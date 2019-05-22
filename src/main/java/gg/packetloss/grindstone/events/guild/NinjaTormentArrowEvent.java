/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import com.sk89q.worldedit.event.Cancellable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import java.util.List;

public class NinjaTormentArrowEvent extends NinjaEvent implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private boolean cancelled = false;

  private List<Entity> entities;

  public NinjaTormentArrowEvent(Player who, List<Entity> entities) {
    super(who);
    this.entities = entities;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public List<Entity> getEntities() {
    return entities;
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
