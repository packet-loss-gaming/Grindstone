package gg.packetloss.grindstone.city.engine;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldIsQuery;
import gg.packetloss.grindstone.playerhistory.PlayerHistoryComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
        if (!managedWorld.is(ManagedWorldIsQuery.SKY, event.getTo().getWorld())) {
            return;
        }

        if (hasAccess(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
    }
}
