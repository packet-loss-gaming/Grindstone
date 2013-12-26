package com.skelril.aurora.events.custom.item;

import com.skelril.aurora.items.specialattack.SpecialAttack;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import static com.skelril.aurora.items.CustomItemsComponent.SpecType;

public class SpecialAttackEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final SpecType context;
    private SpecialAttack spec;

    public SpecialAttackEvent(final Player owner, final SpecType context, final SpecialAttack spec) {

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

        Validate.isTrue(getPlayer().equals(spec.getOwner()), "The owner and the spec owner must match!");

        this.spec = spec;
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
