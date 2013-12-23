package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.events.entity.ProjectileTickEvent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Projectile Watcher", desc = "Projectile Watcher.")
public class ProjectileWatchingComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private Map<Integer, BukkitTask> projectileTask = new HashMap<>();
    private Map<Integer, Location> projectileLoc = new HashMap<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    // Entity
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBowFire(EntityShootBowEvent event) {

        Entity p = event.getProjectile();

        startTracker(p, event.getForce());

        ItemStack bow = event.getBow();

        if (bow != null) {
            p.setMetadata("launcher", new FixedMetadataValue(inst, bow));
        }
    }

    // Not entity
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrowFire(ProjectileLaunchEvent event) {

        startTracker(event.getEntity(), -1);
    }

    public boolean hasChangedLocation(Entity p) {

        return !projectileLoc.containsKey(p.getEntityId())
                || !projectileLoc.get(p.getEntityId()).equals(p.getLocation());
    }

    public void startTracker(final Entity projectile, final float force) {

        if (projectileTask.containsKey(projectile.getEntityId()) || !(projectile instanceof Projectile)) return;

        BukkitTask task = server.getScheduler().runTaskTimer(inst, new Runnable() {

            @Override
            public void run() {

                Location loc = projectile.getLocation();

                if (projectile.isDead() || !hasChangedLocation(projectile)) {
                    projectileLoc.remove(projectile.getEntityId());
                    projectileTask.get(projectile.getEntityId()).cancel();
                } else {
                    server.getPluginManager().callEvent(new ProjectileTickEvent((Projectile) projectile, force));
                    projectileLoc.put(projectile.getEntityId(), loc);
                }
            }
        }, 0, 1); // Start at 0 ticks and repeat every 1 ticks
        projectileTask.put(projectile.getEntityId(), task);
    }
}
