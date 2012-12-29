package us.arrowcraft.aurora.events;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Author: Turtle9598
 */
public class ApocalypseLocalSpawnEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private Location spawnLocation;


    public ApocalypseLocalSpawnEvent(final Player player, Location spawnLocation) {

        super(player);
        this.spawnLocation = spawnLocation;
    }

    public Location getLocation() {

        return spawnLocation;
    }

    public void setLocation(Location spawnLocation) {

        this.spawnLocation = spawnLocation;
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }

    public boolean isCancelled() {

        return cancelled;
    }

    public void setCancelled(boolean cancelled) {

        this.cancelled = cancelled;
    }
}
