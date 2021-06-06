/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class EntityHealthInContextEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final LivingEntity entity;
    private final HealthKind kind;
    private double value;

    public EntityHealthInContextEvent(Player who, LivingEntity entity, double value, boolean upscale) {
        super(who);
        this.entity = entity;
        this.kind = upscale ? HealthKind.DAMAGE_FROM_PLAYER_TO_TARGET_UPSCALED
                            : HealthKind.DAMAGE_FROM_PLAYER_TO_TARGET_DOWNSCALED;
        this.value = value;
    }

    public EntityHealthInContextEvent(Player who, LivingEntity entity, HealthKind kind) {
        super(who);
        this.entity = entity;
        this.kind = kind;
        this.value = kind == HealthKind.CURRENT ? entity.getHealth() : entity.getMaxHealth();
    }

    public LivingEntity getTarget() {
        return entity;
    }

    public HealthKind getKind() {
        return kind;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public static enum HealthKind {
        CURRENT(),
        MAX(),
        DAMAGE_FROM_PLAYER_TO_TARGET_UPSCALED(false),
        DAMAGE_FROM_PLAYER_TO_TARGET_DOWNSCALED();

        private final boolean isDescaleOperation;

        HealthKind() {
            this.isDescaleOperation = true;
        }
        HealthKind(boolean isDescaleOperation) {
            this.isDescaleOperation = isDescaleOperation;
        }

        public boolean isDescale() {
            return isDescaleOperation;
        }
    }
}
