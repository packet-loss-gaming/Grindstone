package gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.SimpleRayTrace;
import gg.packetloss.grindstone.util.VectorUtil;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;

public class Surge extends EntityAttack implements RangedSpecial {
    private static final int RADIUS = 4;
    private static final int RADIUS_SQ = RADIUS * RADIUS;

    public Surge(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    private void runSurge(SimpleRayTrace it, int distance, double totalDamage) {
        if (!it.hasNext()) {
            owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * distance, 1), true);

            int dmgAmt = (int) Math.ceil(totalDamage);
            inform("The surge dealt an impressive " + dmgAmt + ".");
            return;
        }

        server.getScheduler().runTaskLater(inst, () -> {
            Location loc = it.next();

            for (int i = 0; i < 5 && it.hasNext(); ++i) {
                loc = it.next();
                SingleBlockParticleEffect.burstOfFlames(loc);
            }

            Location finalLoc = loc;
            Collection<LivingEntity> entityList = loc.getNearbyEntitiesByType(
                    LivingEntity.class, RADIUS, (e) -> {
                        // Prevent targets being hit multiple times on corners, because of this being a bounding box
                        // as apposed to a proper radius check.
                        double distSqrd = e.getLocation().distanceSquared(finalLoc);
                        return distSqrd <= RADIUS_SQ;
                    }
            );

            int newDistance = distance + 1;
            double newTotal = totalDamage;

            Class<? extends Entity> filterType = target.getClass();
            if (Monster.class.isAssignableFrom(filterType)) {
                filterType = Monster.class;
            }

            for (LivingEntity e : entityList) {
                if (e.isValid() && filterType.isInstance(e)) {
                    if (e.equals(owner)) continue;

                    e.setNoDamageTicks(0);

                    double damage = (e instanceof Player ? 5 : 15) * newDistance;
                    if (!DamageUtil.damageWithSpecialAttack(owner, e, this, damage)) {
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
