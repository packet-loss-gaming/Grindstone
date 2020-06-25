package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.playerhistory.PlayerHistoryComponent;
import gg.packetloss.grindstone.util.listener.DoorRestorationListener;
import gg.packetloss.grindstone.util.listener.NaturalSpawnBlockingListener;
import gg.packetloss.grindstone.util.listener.NuisanceSpawnBlockingListener;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Sky Core", desc = "Operate the core sky world functionality.")
@Depend(components = {ManagedWorldComponent.class, PlayerHistoryComponent.class})
public class SkyWorldCoreComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private PlayerHistoryComponent playerHistory;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
        CommandBook.registerEvents(new DoorRestorationListener(this::isSkyWorld));
        CommandBook.registerEvents(new NaturalSpawnBlockingListener(this::isSkyWorld));
        CommandBook.registerEvents(new NuisanceSpawnBlockingListener(this::isSkyWorld));
    }

    private boolean isSkyWorld(World world) {
        return managedWorld.is(ManagedWorldIsQuery.SKY, world);
    }

    public boolean hasAccess(Player player) {
        if (player.hasPermission("aurora.severe-offense")) {
            return false;
        }

        if (player.hasPermission("aurora.skyworld.override")) {
            return true;
        }

        try {
            return playerHistory.getTimePlayed(player).get() >= TimeUnit.DAYS.toSeconds(30);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!isSkyWorld(event.getTo().getWorld())) {
            return;
        }

        if (hasAccess(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }

        if (!isSkyWorld(event.getEntity().getWorld())) {
            return;
        }

        event.setCancelled(true);

        Location from = event.getEntity().getLocation();

        World city = managedWorld.get(ManagedWorldGetQuery.CITY);
        Location spawn = city.getSpawnLocation();
        double entryRadius = 500;

        event.getEntity().teleport(new Location(
                city,
                spawn.getX() + ((Math.abs(from.getBlockX()) % entryRadius) - (entryRadius / 2)),
                260,
                spawn.getZ() + ((Math.abs(from.getBlockZ()) % entryRadius) - (entryRadius / 2))
        ), PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }
}
