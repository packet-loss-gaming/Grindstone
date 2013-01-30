package us.arrowcraft.aurora.events;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import us.arrowcraft.aurora.prayer.Prayer;

/**
 * Author: Turtle9598
 */
public class PrayerApplicationEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Prayer prayer;


    public PrayerApplicationEvent(final Player player, Prayer prayer) {

        super(player);
        this.prayer = prayer;
    }

    public Prayer getCause() {

        return prayer;
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
