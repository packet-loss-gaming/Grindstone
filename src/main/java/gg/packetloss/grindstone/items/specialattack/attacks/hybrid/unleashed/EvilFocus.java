/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.hybrid.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.task.CountdownHandle;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EvilFocus extends EntityAttack implements MeleeSpecial, RangedSpecial {
    private final int numRuns = (int) Math.max(3, Math.min(15, EntityUtil.getHealth(owner, target)));

    private double animationY = 0;
    private double runOffset = 0;

    private CountdownHandle taskHandle;

    public EvilFocus(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private Location getTargetLocation() {
        Location targetLoc = LocationUtil.findFreePosition(target.getLocation(), false);
        if (targetLoc == null) {
            targetLoc = target.getLocation();
        }
        return targetLoc;
    }

    private void createCage() {
        Location lockedLocation = getTargetLocation();

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setNumberOfRuns(numRuns * 20);

        taskBuilder.setAction((times) -> {
            if (target.isDead()) {
                taskHandle.cancel();
                return true;
            }

            final int radius = 4;

            if (LocationUtil.distanceSquared2D(target.getLocation(), lockedLocation) > Math.pow(radius, 2)) {
                target.teleport(lockedLocation);
            }

            final double loopInterval = 0.05; // controls how much progress we make per particle
            final double loopAdjustment = Math.PI / 6; // controls how much we move when resetting
            final double loopAmplification = 3; // controls how tight the spiraling is
            final double height = Math.max(5, target.getHeight() * 1.5); // controls how tall the spiral is
            final double speed = .05 * (height / 3); // controls how fast we go through cycles

            for (double yProgression = 0; yProgression < speed; yProgression += loopInterval) {
                double newAnimationY = (animationY + loopInterval) % height;

                // Adjust the run offset whenever we've looped around (hit the top and went back to the bottom
                // this allows the animation not move and not just stay fixed in the same position.
                if (newAnimationY < animationY) {
                    runOffset += loopAdjustment;
                }

                animationY = newAnimationY;

                double animationX = radius * Math.cos((animationY * loopAmplification) + runOffset);
                double animationZ = radius * Math.sin((animationY * loopAmplification) + runOffset);

                lockedLocation.getWorld().spawnParticle(
                        Particle.FIREWORKS_SPARK,
                        lockedLocation.getX() + animationX,
                        lockedLocation.getY() + animationY,
                        lockedLocation.getZ() + animationZ,
                        0
                );
            }

            return true;
        });

        taskHandle = taskBuilder.build();
    }

    @Override
    public void activate() {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * (numRuns + 3), 1), true);
        createCage();

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1, .02F);
        inform("Your weapon traps your foe in their own sins.");
    }
}
