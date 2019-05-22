/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.egg;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EggHatchEvent extends Event implements Cancellable {

  private static final HandlerList handlers = new HandlerList();
  private final Item egg;
  private boolean cancelled = false;
  private EntityType eggType;
  private Location location;

  public EggHatchEvent(Item egg, EntityType eggType, Location location) {

    this.egg = egg;
    this.eggType = eggType;
    this.location = location;
  }

  public static HandlerList getHandlerList() {

    return handlers;
  }

  public Item getEgg() {

    return egg;
  }

  public EntityType getEggType() {

    return eggType;
  }

  public void setEggType(EntityType eggType) {

    this.eggType = eggType;
  }

  public Location getLocation() {

    return location;
  }

  public void setLocation(Location location) {

    this.location = location;
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