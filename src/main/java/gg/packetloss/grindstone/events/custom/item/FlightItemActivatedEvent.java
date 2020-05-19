package gg.packetloss.grindstone.events.custom.item;

import gg.packetloss.grindstone.items.flight.FlightCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class FlightItemActivatedEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private FlightCategory category;

    public FlightItemActivatedEvent(Player who, FlightCategory category) {
        super(who);
        this.category = category;
    }

    public FlightCategory getCategory() {
        return category;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
