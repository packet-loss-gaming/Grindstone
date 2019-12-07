package gg.packetloss.grindstone.events.custom.item;

import gg.packetloss.grindstone.items.custom.CustomItems;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class BuildToolUseEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    private Location startingPoint;
    private CustomItems tool;

    public BuildToolUseEvent(Player who, Location startingPoint, CustomItems tool) {
        super(who);
        this.startingPoint = startingPoint;
        this.tool = tool;
    }

    public Location getStartingPoint() {
        return startingPoint.clone();
    }

    public CustomItems getTool() {
        return tool;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
