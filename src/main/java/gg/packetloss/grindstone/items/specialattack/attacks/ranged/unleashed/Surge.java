package gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.Collection;

public class Surge extends EntityAttack implements RangedSpecial {

    public Surge(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    private void runSurge(BlockIterator it, int distance, double totalDamage) {
        if (!it.hasNext()) {
            owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + (totalDamage * .25)));
            return;
        }

        server.getScheduler().runTaskLater(inst, () -> {
            Block block = it.next();

            for (int i = 0; i < 4 && it.hasNext(); ++i) {
                block = it.next();
                SingleBlockParticleEffect.burstOfFlames(block.getLocation());
            }

            Collection<LivingEntity> entityList = block.getLocation().getNearbyEntitiesByType(
                    LivingEntity.class, 4,4, 4
            );

            int newDistance = distance + 5;
            double newTotal = totalDamage;

            for (LivingEntity e : entityList) {
                if (e.isValid()) {
                    if (e.equals(owner)) continue;

                    if (!DamageUtil.damageWithSpecialAttack(owner, e, this, newDistance)) {
                        continue;
                    }

                    newTotal += newDistance;

                    SingleBlockParticleEffect.burstOfFlames(e.getLocation());
                }
            }

            runSurge(it, newDistance, newTotal);
        }, 3);
    }

    @Override
    public void activate() {
        int maxBlocks = (int) owner.getLocation().distance(target.getLocation()) + 1;

        Vector vel = target.getLocation().toVector().subtract(owner.getLocation().toVector());

        BlockIterator it = new BlockIterator(
                owner.getWorld(),
                owner.getLocation().toVector(),
                vel,
                0,
                maxBlocks
        );

        runSurge(it, 0, 3);

        inform("Your bow unleashes a surge of energy.");
    }
}
