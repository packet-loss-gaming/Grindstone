package gg.packetloss.grindstone.city.engine.area.areas.NinjaParkour;

import gg.packetloss.grindstone.city.engine.area.AreaListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NinjaParkourListener extends AreaListener<NinjaParkour> {
    public NinjaParkourListener(NinjaParkour parent) {
        super(parent);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            parent.teleportToStart(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        parent.clearState(event.getPlayer());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();

        if (parent.contains(to) && !event.getCause().equals(PlayerTeleportEvent.TeleportCause.UNKNOWN)) {
            event.setTo(parent.resetPoint.clone());
        }
    }
}
