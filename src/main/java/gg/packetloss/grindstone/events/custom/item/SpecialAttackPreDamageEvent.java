/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.custom.item;

import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SpecialAttackPreDamageEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final LivingEntity attacker;
    private final SpecialAttack spec;

    private LivingEntity defender;
    private double damage;

    private boolean cancelled = false;

    public SpecialAttackPreDamageEvent(LivingEntity attacker, LivingEntity defender, SpecialAttack spec, double damage) {
        this.attacker = attacker;
        this.spec = spec;

        this.defender = defender;
        this.damage = damage;
    }

    public LivingEntity getAttacker() {
        return attacker;
    }

    public LivingEntity getDefender() {
        return defender;
    }

    public void setDefender(LivingEntity defender) {
        this.defender = defender;
    }

    public SpecialAttack getSpec() {
        return spec;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
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