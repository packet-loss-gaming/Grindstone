/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.hybrid.fear;

import com.destroystokyo.paper.ParticleBuilder;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class HellCano extends EntityAttack implements MeleeSpecial, RangedSpecial {
    private static final int RADIUS = 4;
    private static final int RADIUS_SQ = RADIUS * RADIUS;

    private boolean hasErupted = false;
    private boolean isBigWave = false;

    public HellCano(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private Location getTargetLocation() {
        Location targetLoc = LocationUtil.findFreePosition(target.getLocation(), false);
        if (targetLoc == null) {
            targetLoc = target.getLocation();
        }
        return targetLoc;
    }

    private void damage(Entity entity, double damage) {
        entity.setFireTicks(20 * 3);
        SpecialAttackFactory.processDamage(owner, (LivingEntity) entity, this, damage);
    }

    public int getParticleSize() {
        if (!hasErupted) {
            return 1;
        }

        if (isBigWave) {
            return 40;
        }

        return 5;
    }

    private void createHelloCano() {
        Location lockedLocation = getTargetLocation();

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(1);
        int numRuns = (ChanceUtil.getRandom(6) + 3) * 2 * 20;
        taskBuilder.setNumberOfRuns(numRuns);

        taskBuilder.setAction((times) -> {
            if (owner instanceof Player) {
                server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
            }

            if (!hasErupted && ChanceUtil.getChance(60)) {
                hasErupted = true;
            }

            if (ChanceUtil.getChance(40)) {
                isBigWave = !isBigWave;
            }

            new ParticleBuilder(Particle.LAVA).count(getParticleSize()).location(lockedLocation).allPlayers().spawn();

            if (!hasErupted) {
                return false;
            }

            if (times % 10 != 0) {
                return true;
            }

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

            double damage = isBigWave ? 12 : 3;
            for (Entity aEntity : entityList) {
                if (!aEntity.isValid() || aEntity.equals(owner)) continue;

                damage(aEntity, damage);
            }

            // Damage the owner if too close regardless of filter type.
            if (owner.getWorld().equals(lockedLocation.getWorld())) {
                if (lockedLocation.distanceSquared(owner.getLocation()) <= RADIUS_SQ) {
                    damage(owner, damage);
                }
            }

            return true;
        });

        taskBuilder.build();
    }

    @Override
    public void activate() {
        createHelloCano();

        inform("Your weapon unleashes a hell-cano.");
    }
}