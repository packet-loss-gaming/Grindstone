/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.anticheat.FallBlockerEvent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Soft Wool", desc = "Fall softly my friends.")
public class SoftWoolComponent extends BukkitComponent implements Listener {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = inst.getLogger();
  private final Server server = CommandBook.server();

  @Override
  public void enable() {

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);

  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event) {

    Entity entity = event.getEntity();
    EntityDamageEvent.DamageCause damageCause = event.getCause();

    if (damageCause.equals(EntityDamageEvent.DamageCause.FALL)
        && LocationUtil.getBelowID(entity.getLocation(), BlockID.CLOTH)) {
      if (entity instanceof Player) {
        FallBlockerEvent fEvent = new FallBlockerEvent((Player) entity);
        server.getPluginManager().callEvent(fEvent);
        if (fEvent.isDisplayingMessage()) {
          ChatUtil.sendNotice(entity, "The cloth negates your fall damage.");
        }
      }
      event.setCancelled(true);
    }
  }
}
