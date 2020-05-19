package gg.packetloss.grindstone.events.custom.item;

import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class SpecialAttackSelectEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;
    private boolean tryAgain = false;
    private final SpecType context;
    private SpecialAttack spec;

    public SpecialAttackSelectEvent(final Player owner, final SpecType context, final SpecialAttack spec) {
        super(owner);

        Validate.isTrue(owner.equals(spec.getOwner()), "The owner and the spec owner must match!");

        this.context = context;
        this.spec = spec;
    }

    public SpecType getContext() {
        return context;
    }

    public SpecialAttack getSpec() {
        return spec;
    }

    public void setSpec(SpecialAttack spec) {
        Validate.notNull(spec);
        Validate.isTrue(getPlayer().equals(spec.getOwner()), "The owner and the spec owner must match!");

        this.spec = spec;
    }

    public boolean shouldTryAgain() {
        return tryAgain;
    }

    public void tryAgain() {
        this.tryAgain = true;
        this.cancelled = true;
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
