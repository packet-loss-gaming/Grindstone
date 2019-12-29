package gg.packetloss.grindstone.events.playerstate;

import gg.packetloss.grindstone.state.player.PlayerStateKind;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class PlayerStatePrePopEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private PlayerStateKind kind;

    public PlayerStatePrePopEvent(Player who, PlayerStateKind kind) {
        super(who);
        this.kind = kind;
    }

    public PlayerStateKind getKind() {
        return kind;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}