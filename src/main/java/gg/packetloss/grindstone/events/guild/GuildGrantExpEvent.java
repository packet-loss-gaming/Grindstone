package gg.packetloss.grindstone.events.guild;

import gg.packetloss.grindstone.guild.GuildType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class GuildGrantExpEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final GuildType guild;
    private double grantedExp;
    private boolean cancelled = false;

    public GuildGrantExpEvent(Player who, GuildType type, double grantedExpr) {
        super(who);
        this.guild = type;
        this.grantedExp = grantedExpr;
    }

    public GuildType getGuild() {
        return guild;
    }

    public double getGrantedExp() {
        return grantedExp;
    }

    public void setGrantedExp(double grantedExp) {
        this.grantedExp = grantedExp;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}