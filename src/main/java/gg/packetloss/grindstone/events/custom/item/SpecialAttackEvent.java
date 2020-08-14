/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.custom.item;

import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class SpecialAttackEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final SpecType context;
    private final SpecialAttack spec;
    private long coolDown;

    public SpecialAttackEvent(Player owner, SpecType context, SpecialAttack spec) {
        super(owner);

        Validate.isTrue(owner.equals(spec.getOwner()), "The owner and the spec owner must match!");

        this.context = context;
        this.spec = spec;
        this.coolDown = spec.getCoolDown(context);

        if (owner.hasPermission("aurora.tome.legends")) {
            this.coolDown *= .9;
        }
    }

    public SpecType getContext() {
        return context;
    }

    public SpecialAttack getSpec() {
        return spec;
    }

    public long getContextCoolDown() {
        return coolDown;
    }

    public void setContextCooldown(long coolDown) {
        this.coolDown = coolDown;
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
