package gg.packetloss.grindstone.events.guild;

import gg.packetloss.grindstone.guild.GuildType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class GuildPowersEnableEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private GuildType guild;
    private boolean cancelled = false;

    public GuildPowersEnableEvent(Player who, GuildType guild) {
        super(who);
        this.guild = guild;
    }

    public GuildType getGuild() {
        return guild;
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
