/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.SimpleRayTrace;
import gg.packetloss.grindstone.util.VectorUtil;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

public class Surge extends EntityAttack implements RangedSpecial {
    private static final int RADIUS = 4;
    private static final int RADIUS_SQ = RADIUS * RADIUS;

    public Surge(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private void runSurge(SimpleRayTrace it, int distance, double totalDamage) {
        if (!it.hasNext()) {
            owner.removePotionEffect(PotionEffectType.REGENERATION);
            owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * distance, 1));

            int dmgAmt = (int) Math.ceil(totalDamage);
            inform("The surge dealt an impressive " + dmgAmt + ".");
            return;
        }

        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            Location loc = it.next();

            for (int i = 0; i < 5 && it.hasNext(); ++i) {
                loc = it.next();
                SingleBlockParticleEffect.burstOfFlames(loc);
            }

            Location finalLoc = loc;

            Class<? extends LivingEntity> filterType = target.getClass();
            if (Monster.class.isAssignableFrom(filterType)) {
                filterType = Monster.class;
            }

            Collection<? extends LivingEntity> entityList = loc.getNearbyEntitiesByType(
                    filterType, RADIUS, (e) -> {
                        // Prevent targets being hit multiple times on corners, because of this being a bounding box
                        // as apposed to a proper radius check.
                        double distSqrd = e.getLocation().distanceSquared(finalLoc);
                        return distSqrd <= RADIUS_SQ;
                    }
            );

            int newDistance = distance + 1;
            double newTotal = totalDamage;

            for (LivingEntity e : entityList) {
                if (e.isValid() && !e.equals(owner)) {
                    double damage = (e instanceof Player ? 5 : 15) * newDistance;
                    if (!SpecialAttackFactory.processDamage(owner, e, this, damage)) {
                        continue;
                    }

                    newTotal += damage;

                    SingleBlockParticleEffect.burstOfFlames(e.getLocation());
                }
            }

            runSurge(it, newDistance, newTotal);
        }, 2);
    }

    @Override
    public void activate() {
        int maxBlocks = (int) owner.getLocation().distance(target.getLocation()) + 15;

        Vector vel = VectorUtil.createDirectionalVector(owner.getLocation(), target.getLocation());

        SimpleRayTrace it = new SimpleRayTrace(
                owner.getLocation(),
                vel,
                maxBlocks
        );

        runSurge(it, 0, 0);

        inform("Your bow unleashes a surge of energy.");
    }
}
