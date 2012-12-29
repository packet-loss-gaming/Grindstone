package us.arrowcraft.aurora.events;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Author: Turtle9598
 */
public class JungleFallBlockerEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    public JungleFallBlockerEvent(Player player) {

        super(player);
    }

    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
