package com.skelril.aurora.items.specialattack.attacks.ranged.unleashed;

import com.skelril.aurora.city.engine.PvPComponent;
import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.ranged.RangedSpecial;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.DamageUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Created by wyatt on 12/26/13.
 */
public class GlowingFog extends EntityAttack implements RangedSpecial {

    public GlowingFog(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {
        final Location targeted = target.getLocation();

        IntegratedRunnable glowingFog = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {

                if (owner instanceof Player) {
                    server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
                }

                EnvironmentUtil.generateRadialEffect(targeted, Effect.MOBSPAWNER_FLAMES);

                for (Entity aEntity : targeted.getWorld().getEntitiesByClasses(LivingEntity.class)) {
                    if (!aEntity.isValid() || aEntity.equals(owner)
                            || aEntity.getLocation().distanceSquared(targeted) > 16) continue;
                    if (aEntity instanceof LivingEntity) {
                        if (aEntity instanceof Player) {
                            if (owner instanceof Player && !PvPComponent.allowsPvP((Player) owner, (Player) aEntity)) continue;
                        }
                        DamageUtil.damage(owner, target, 5);
                    }
                }
                return true;
            }

            @Override
            public void end() {

            }
        };

        TimedRunnable runnable = new TimedRunnable(glowingFog, (ChanceUtil.getRandom(15) * 3) + 7);
        runnable.setTask(server.getScheduler().runTaskTimer(inst, runnable, 0, 10));

        inform("Your bow unleashes a powerful glowing fog.");
    }
}
