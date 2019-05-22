/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.entity.item;

import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DropClearPulseEvent extends Event {

  private static final HandlerList handlers = new HandlerList();
  private final World world;
  private int secondsLeft;

  public DropClearPulseEvent(World world, int secondsLeft) {

    this.world = world;
    this.secondsLeft = secondsLeft;
  }

  public static HandlerList getHandlerList() {

    return handlers;
  }

  public World getWorld() {

    return world;
  }

  public int getSecondsLeft() {

    return secondsLeft;
  }

  public void setSecondsLeft(int secondsLeft) {

    this.secondsLeft = Math.min(120, secondsLeft);
  }

  public int getCurrentSecond() {

    return secondsLeft + 1;
  }

  public HandlerList getHandlers() {

    return handlers;
  }
}
