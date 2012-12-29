package us.arrowcraft.aurora.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import us.arrowcraft.aurora.admin.AdminState;

public class PlayerAdminModeChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private AdminState newAdminState;


    public PlayerAdminModeChangeEvent(final Player player, AdminState newAdminState) {

        super(player);
        this.newAdminState = newAdminState;
    }

    public AdminState getNewAdminState() {

        return newAdminState;
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