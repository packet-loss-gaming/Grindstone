package gg.packetloss.grindstone.events.economy;

import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.List;

public class MarketPurchaseEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final List<MarketTransactionLine> items;
    private final double totalCost;

    public MarketPurchaseEvent(Player who, List<MarketTransactionLine> items, double totalCost) {
        super(who);
        this.items = items;
        this.totalCost = totalCost;
    }

    public List<MarketTransactionLine> getTransactionLines() {
        return items;
    }

    public double getTotalCost() {
        return totalCost;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
