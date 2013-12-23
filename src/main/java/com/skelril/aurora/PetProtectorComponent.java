package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Pet Protector", desc = "Protectin dem petz.")
@Depend(components = {AdminComponent.class})
public class PetProtectorComponent extends BukkitComponent implements Listener {

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

    private static Set<EntityDamageEvent.DamageCause> ignored = new HashSet<>();

    static {
        ignored.add(EntityDamageEvent.DamageCause.FALL);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Tameable)) return;

        Entity e = null;
        if (event instanceof EntityDamageByEntityEvent) {
            e = ((EntityDamageByEntityEvent) event).getDamager();
        }
        if (isSafe(event.getEntity()) && !ignored.contains(event.getCause())) {
            if (e != null) {
                if (e instanceof Projectile) e = ((Projectile) e).getShooter();
                if (e != null && e instanceof Player) {
                    if (admin.isSysop((Player) e)) return;
                    ChatUtil.sendError((Player) e, "You cannot currently hurt that " + event.getEntityType().toString().toLowerCase() + ".");
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {

        if (!(event.getRightClicked() instanceof Tameable)) return;

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
