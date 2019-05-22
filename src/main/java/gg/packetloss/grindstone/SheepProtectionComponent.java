/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.DyeColor;
import org.bukkit.EntityEffect;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Sheep", desc = "No more dying sheep!")
public class SheepProtectionComponent extends BukkitComponent implements Listener {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = CommandBook.logger();
  private final Server server = CommandBook.server();

  @Override
  public void enable() {

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  @EventHandler
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

    Player player = event.getPlayer();
    final Entity entity = event.getRightClicked();

    if (entity instanceof Sheep && player.getItemInHand().getTypeId() == ItemID.INK_SACK) {

      // Create a fake explosion
      entity.getWorld().createExplosion(entity.getLocation(), 0);

      // Hurt them
      if ((player.getHealth() - 10) > 1) {
        player.setHealth(player.getHealth() - 10);
      } else {
        player.setHealth(1);
      }

      // Make an damage effect
      player.playEffect(EntityEffect.HURT);

      // Tell them not to do it again
      ChatUtil.sendWarning(player, "Don't dye sheep!");

      server.getScheduler().scheduleSyncDelayedTask(inst, () -> {
        Sheep sheep = (Sheep) entity;
        sheep.setColor(DyeColor.WHITE);
      }, 1);
    }
  }
}
