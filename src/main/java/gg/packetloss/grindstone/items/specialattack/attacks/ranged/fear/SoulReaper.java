/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class SoulReaper extends EntityAttack implements RangedSpecial {
    private static final int RADIUS = 5;
    private static final int RADIUS_SQ = RADIUS * RADIUS;

    private int reaped = 0;

    public SoulReaper(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private Location getTargetLocation() {
        Location targetLoc = LocationUtil.findFreePosition(target.getLocation(), false);
        if (targetLoc == null) {
            targetLoc = target.getLocation();
        }
        return targetLoc;
    }

    private void damage(LivingEntity entity) {
        ++reaped;

        SpecialAttackFactory.processDamage(owner, entity, this, entity instanceof Player ? 200 : 10000);
    }

    private void createHelix(Location lockedLocation, double radiusProgress, double circleProgress) {
        double animationX = (RADIUS - radiusProgress) * Math.cos(circleProgress);
        double animationZ = (RADIUS - radiusProgress) * Math.sin(circleProgress);

        lockedLocation.getWorld().spawnParticle(
                Particle.ENCHANT,
                lockedLocation.getX() + animationX,
                lockedLocation.getY() + .5,
                lockedLocation.getZ() + animationZ,
                0
        );
    }

    @Override
    public void activate() {
        Location lockedLocation = getTargetLocation();

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        final int numRuns = 5 * 20;
        final double stepIncrement = (double) RADIUS / numRuns; // the progress per step
        final double loopInterval = 0.05; // controls particle density, lower values ar more particles

        taskBuilder.setNumberOfRuns(numRuns);

        taskBuilder.setAction((times) -> {
            for (double progress = Math.PI * 2; progress > 0; progress -= loopInterval) {
                double animationX = RADIUS * Math.cos(progress);
                double animationZ = RADIUS * Math.sin(progress);

                lockedLocation.getWorld().spawnParticle(
                        Particle.ENCHANT,
                        lockedLocation.getX() + animationX,
                        lockedLocation.getY() + .5,
                        lockedLocation.getZ() + animationZ,
                        0
                );
            }

            int currentStep = numRuns - times;
            for (double progress = currentStep * stepIncrement;  progress > 0; progress -= loopInterval) {
                final int subSteps = 4;
                final double subStepIncrement = (Math.PI * 2) / subSteps;

                for (int subStep = 0; subStep < subSteps; ++subStep) {
                    createHelix(lockedLocation, progress, progress + (subStep * subStepIncrement));
                };
            }

            return true;
        });

        taskBuilder.setFinishAction(() -> {
            if (owner instanceof Player) {
                CommandBook.callEvent(new RapidHitEvent((Player) owner));
            }

            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1, .02F);

            Class<? extends LivingEntity> filterType = target.getClass();
            if (Monster.class.isAssignableFrom(filterType)) {
                filterType = Monster.class;
            }

            Collection<? extends LivingEntity> entityList = lockedLocation.getNearbyEntitiesByType(
                    filterType, RADIUS, (e) -> {
                        // Use a radius check to make this circular
                        double distSqrd = e.getLocation().distanceSquared(lockedLocation);
                        return distSqrd <= RADIUS_SQ;
                    }
            );

            for (LivingEntity entity : entityList) {
                if (!entity.isValid() || entity.equals(owner)) continue;

                damage(entity);
            }

            // Damage the owner if too close regardless of filter type.
            if (lockedLocation.distanceSquared(owner.getLocation()) <= RADIUS_SQ) {
                damage(owner);
            }

            inform("Your bow reaped " + reaped + " souls.");
        });

        taskBuilder.build();

        inform("Your bow channels demonic energy.");
    }
}
