/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Pet Protector", desc = "Protectin dem petz.")
@Depend(components = {AdminComponent.class})
public class PetProtectorComponent extends BukkitComponent implements Listener {

  private static Set<EntityDamageEvent.DamageCause> ignored = new HashSet<>();

  static {
    ignored.add(EntityDamageEvent.DamageCause.FALL);
  }

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = inst.getLogger();
  private final Server server = CommandBook.server();
  @InjectComponent
  private AdminComponent admin;

  @Override
  public void enable() {

    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }

  @EventHandler
  public void onEntityTarget(EntityTargetEvent event) {

    if (event.getTarget() instanceof Player && isSafe(event.getEntity())) {

      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event) {

    if (!(event.getEntity() instanceof Tameable)) {
      return;
    }

    Entity e = null;
    if (event instanceof EntityDamageByEntityEvent) {
      e = ((EntityDamageByEntityEvent) event).getDamager();
    }
    if (isSafe(event.getEntity()) && !ignored.contains(event.getCause())) {
      if (e != null) {
        ProjectileSource source = null;
        if (e instanceof Projectile) {
          source = ((Projectile) e).getShooter();
        }
        if (source != null && source instanceof Player) {
          e = (Entity) source;
          ChatUtil.sendError(e, "You cannot hurt a " + event.getEntityType().toString().toLowerCase() + " that you don't own.");
        }
      }
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onEntityInteract(PlayerInteractEntityEvent event) {

    if (!(event.getRightClicked() instanceof Tameable)) {
      return;
    }

    Player player = event.getPlayer();
    Tameable tameable = (Tameable) event.getRightClicked();

    if (event.getRightClicked() instanceof Horse && tameable.isTamed() && player.getItemInHand().getTypeId() == ItemID.RED_APPLE) {
      if (tameable.getOwner() == null) {

        tameable.setOwner(player);
        event.setCancelled(true);

        ItemUtil.removeItemOfType(player, ItemID.RED_APPLE, 1, true);

        ChatUtil.sendNotice(player, "You have gained possession of this horse.");
        return;
      } else if (player.isSneaking() && tameable.getOwner().getName().equals(player.getName())) {

        tameable.setOwner(null);
        tameable.setTamed(true);
        event.setCancelled(true);

        ItemUtil.removeItemOfType(player, ItemID.RED_APPLE, 1, true);

        ChatUtil.sendNotice(player, "You have lost possession of this horse.");
        return;
      }
    }

    if (isSafe(event.getRightClicked()) && (tameable.getOwner() == null || !tameable.getOwner().getName().equals(player.getName()))) {

      event.setCancelled(true);
      ChatUtil.sendError(player, "You cannot currently interact with that " + event.getRightClicked().getType().toString().toLowerCase() + ".");
    }
  }

  private boolean isSafe(Entity entity) {

    if (entity instanceof LivingEntity && entity instanceof Vehicle) {
      Vehicle horse = (Vehicle) entity;
      Entity passenger = horse.getPassenger();
      if (passenger != null && passenger instanceof Player) {
        return true;
      }
    }

    return entity instanceof Tameable && ((Tameable) entity).isTamed() && ((Tameable) entity).getOwner() != null;
  }
}
