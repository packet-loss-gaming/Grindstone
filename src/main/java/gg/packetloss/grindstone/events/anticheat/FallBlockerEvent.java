/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class FallBlockerEvent extends PlayerEvent {

  private static final HandlerList handlers = new HandlerList();
  private boolean displayMessage = true;

  public FallBlockerEvent(Player player) {

    super(player);
  }

  public static HandlerList getHandlerList() {

    return handlers;
  }

  public boolean isDisplayingMessage() {

    return displayMessage;
  }

  public void setDisplayMessage(boolean displayMessage) {

    this.displayMessage = displayMessage;
  }

  public HandlerList getHandlers() {

    return handlers;
  }
}
