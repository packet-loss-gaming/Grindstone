package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Pet Protector", desc = "Protectin dem petz.")
@Depend(plugins = {"WorldGuard"})
public class PetProtectorComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private WorldGuardPlugin worldGuard;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        setUpWorldGuard();
    }

    private void setUpWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            worldGuard = null;
        }

        //noinspection ConstantConditions
        worldGuard = (WorldGuardPlugin) plugin;
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {

        if (event.getTarget() instanceof Player && isSafe(event.getEntity())) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

        if (!(event.getEntity() instanceof Tameable)) return;

        Entity e = event.getDamager();
        if (e instanceof Projectile) e = ((Projectile) e).getShooter();
        if (e != null && e instanceof Player) {
            Player player = (Player) e;
            Tameable tameable = (Tameable) event.getEntity();

            if (isSafe(event.getEntity()) && (tameable.getOwner() == null || !tameable.getOwner().getName().equals(player.getName()))) {

                event.setCancelled(true);
                ChatUtil.sendError(player, "You cannot currently hurt that " + event.getEntityType().toString().toLowerCase() + ".");
            }
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

                ItemUtil.removeItemOfType(player, ItemID.RED_APPLE, 1);

                ChatUtil.sendNotice(player, "You have gained possession of this horse.");
                return;
            } else if (player.isSneaking() && tameable.getOwner().getName().equals(player.getName())) {

                tameable.setOwner(null);
                tameable.setTamed(true);
                event.setCancelled(true);

                ItemUtil.removeItemOfType(player, ItemID.RED_APPLE, 1);

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
            if (passenger != null) {
                return true;
            }
        }

        if (!(entity instanceof Tameable) || !((Tameable) entity).isTamed() || ((Tameable) entity).getOwner() == null) return false;

        org.bukkit.Location loc = entity.getLocation();
        RegionManager mgr = worldGuard.getGlobalRegionManager().get(loc.getWorld());
        ApplicableRegionSet applicable = mgr.getApplicableRegions(new Vector(loc.getX(), loc.getY(), loc.getZ()));

        for (ProtectedRegion region : applicable) {

            if (region.getOwners().contains(((Tameable) entity).getOwner().getName())) return true;
        }

        try {
            PlayerUtil.matchPlayerExactly(Bukkit.getConsoleSender(), ((Tameable) entity).getOwner().getName());
        } catch (CommandException e) {
            return false;
        }
        return true;
    }
}
