/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.guild;

import gg.packetloss.grindstone.guild.GuildType;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class GuildCalculateCombatExpEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final GuildType guild;
    private final LivingEntity target;
    private final Projectile associatedProjectile;
    private final double damageDealt;
    private double expCap = 50;
    private double damageMultiplier = .1;
    private double damageCap;
    private double projectileModifier;

    public GuildCalculateCombatExpEvent(Player who, LivingEntity target, Projectile associatedProjectile,
                                        GuildType type, double damageDealt, double damageCap) {
        super(who);
        this.target = target;
        this.associatedProjectile = associatedProjectile;
        this.guild = type;
        this.damageDealt = damageDealt;
        this.damageCap = damageCap;
        this.projectileModifier = getDefaultProjectileModifier();
    }

    private double getDefaultProjectileModifier() {
        double projectileModifier = 1;

        if (associatedProjectile != null) {
            if (associatedProjectile.hasMetadata("guild-exp-modifier")) {
                List<MetadataValue> values = associatedProjectile.getMetadata("guild-exp-modifier");
                for (MetadataValue value : values) {
                    projectileModifier *= value.asDouble();
                }
            }
        }

        return projectileModifier;
    }

    public double getCalculatedExp() {
        return Math.min(
            expCap,
            this.damageMultiplier * this.projectileModifier * Math.min(this.damageCap, this.damageDealt)
        );
    }

    public GuildType getGuild() {
        return guild;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public Projectile getAssociatedProjectile() {
        return associatedProjectile;
    }

    public double getExpCap() {
        return expCap;
    }

    public void setExpCap(double expCap) {
        this.expCap = expCap;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public void setDamageMultiplier(double damageMultiplier) {
        this.damageMultiplier = damageMultiplier;
    }

    public double getDamageDealt() {
        return damageDealt;
    }

    public double getDamageCap() {
        return damageCap;
    }

    public void setDamageCap(double damageCap) {
        this.damageCap = damageCap;
    }

    public double getProjectileModifier() {
        Validate.isTrue(associatedProjectile != null);
        return projectileModifier;
    }

    public void setProjectileModifier(double projectileModifier) {
        Validate.isTrue(associatedProjectile != null);
        this.projectileModifier = projectileModifier;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
