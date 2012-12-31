package us.arrowcraft.aurora.events;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import us.arrowcraft.aurora.EggComponent;

/**
 * Author: Turtle9598
 */
public class EggDropEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private EggComponent.EggEntity eggType;
    private Location location;

    public EggDropEvent(EggComponent.EggEntity eggType, Location location) {

        this.eggType = eggType;
        this.location = location;
    }

    public EggComponent.EggEntity getEggType() {

        return eggType;
    }

    public void setEggType(EggComponent.EggEntity eggType) {

        this.eggType = eggType;
    }

    public Location getLocation() {

        return location;
    }

    public void setLocation(Location location) {

        this.location = location;
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
