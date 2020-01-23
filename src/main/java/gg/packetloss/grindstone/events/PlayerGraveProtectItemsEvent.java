package gg.packetloss.grindstone.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerGraveProtectItemsEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final Location deathLocation;
    private boolean usingGemOfLife;
    private ItemStack[] drops;
    private boolean cancelled = false;

    public PlayerGraveProtectItemsEvent(Player who, boolean usingGemOfLife, ItemStack[] drops) {
        super(who);
        this.deathLocation = who.getLocation();
        this.usingGemOfLife = usingGemOfLife;
        this.drops = drops;
    }

    public Location getDeathLocation() {
        return deathLocation.clone();
    }

    public boolean isUsingGemOfLife() {
        return usingGemOfLife;
    }

    public ItemStack[] getDrops() {
        return drops;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
