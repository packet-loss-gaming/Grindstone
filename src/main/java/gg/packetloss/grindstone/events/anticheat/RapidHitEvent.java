/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.anticheat;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class RapidHitEvent extends PlayerEvent {

  private static final HandlerList handlers = new HandlerList();
  private int damage;

  public RapidHitEvent(Player player) {

    super(player);
    this.damage = -1;
  }

  public RapidHitEvent(Player player, int damage) {

    super(player);
    this.damage = damage;
  }

  public static HandlerList getHandlerList() {

    return handlers;
  }

  /**
   * @return the damage or -1 if not applicable
   */
  public int getDamage() {

    return damage;
  }

  public void setDamage(int damage) {

    Validate.isTrue(damage >= -1, "The damage must be greater than or equal to negative one");
    this.damage = damage;
  }

  public HandlerList getHandlers() {

    return handlers;
  }
}