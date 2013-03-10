package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
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

            if (isSafe(event.getEntity()) && !tameable.getOwner().getName().equals(player.getName())) {

                event.setCancelled(true);
                ChatUtil.sendError(player, "That is not your " + event.getEntityType().toString().toLowerCase() + ".");
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {

        if (!(event.getRightClicked() instanceof Tameable)) return;

        Player player = event.getPlayer();
        Tameable tameable = (Tameable) event.getRightClicked();

        if (isSafe(event.getRightClicked()) && !tameable.getOwner().getName().equals(player.getName())) {

            event.setCancelled(true);
            ChatUtil.sendError(player, "That is not your "
                    + event.getRightClicked().getType().toString().toLowerCase() + ".");
        }
    }

    private boolean isSafe(Entity entity) {

        if (!(entity instanceof Tameable) || ((Tameable) entity).getOwner() == null) return false;

        org.bukkit.Location loc = entity.getLocation();
        ApplicableRegionSet applicable = worldGuard.getGlobalRegionManager()
                .get(loc.getWorld()).getApplicableRegions(new Vector(loc.getX(), loc.getY(), loc.getZ()));

        for (ProtectedRegion region : applicable) {

            if (region.getOwners().contains(((Tameable) entity).getOwner().getName())) return true;
        }
        return false;
    }
}
