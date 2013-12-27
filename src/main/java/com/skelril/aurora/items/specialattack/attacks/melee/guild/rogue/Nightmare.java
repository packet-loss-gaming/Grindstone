package com.skelril.aurora.items.specialattack.attacks.melee.guild.rogue;

import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.melee.MeleeSpecial;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowball;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Created by wyatt on 12/26/13.
 */
public class Nightmare extends EntityAttack implements MeleeSpecial {

    public Nightmare(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    @Override
    public void activate() {

        inform("You unleash a nightmare upon the plane.");

        final Location[] loctions = LocationUtil.getNearbyLocations(target.getLocation().add(0, 5, 0), 12);

        IntegratedRunnable hellFire = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                for (Location location : loctions) {
                    if (ChanceUtil.getChance(6)) {
                        Snowball snowball = (Snowball) location.getWorld().spawnEntity(location, EntityType.SNOWBALL);
                        snowball.setMetadata("rogue-snowball", new FixedMetadataValue(inst, true));
                        snowball.setMetadata("nightmare", new FixedMetadataValue(inst, true));
                        snowball.setShooter(owner);
                    }
                }
                return true;
            }

            @Override
            public void end() {
                inform("Your nightmare fades away...");
            }
        };

        TimedRunnable runnable = new TimedRunnable(hellFire, 40);
        runnable.setTask(server.getScheduler().runTaskTimer(inst, runnable, 50, 10));
    }
}
