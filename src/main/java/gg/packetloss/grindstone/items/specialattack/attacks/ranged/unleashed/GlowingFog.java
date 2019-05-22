/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed;

import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
          SERVER.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
        }

        EnvironmentUtil.generateRadialEffect(targeted, Effect.MOBSPAWNER_FLAMES);

        for (Entity aEntity : targeted.getWorld().getEntitiesByClasses(LivingEntity.class)) {
          if (!aEntity.isValid() || aEntity.equals(owner)
              || aEntity.getLocation().distanceSquared(targeted) > 16) {
            continue;
          }
          if (aEntity instanceof LivingEntity) {
            if (aEntity instanceof Player) {
              if (owner instanceof Player && !PvPComponent.allowsPvP((Player) owner, (Player) aEntity)) {
                continue;
              }
            }
            DamageUtil.damage(owner, (LivingEntity) aEntity, 5);
          }
        }
        return true;
      }

      @Override
      public void end() {

      }
    };

    TimedRunnable runnable = new TimedRunnable(glowingFog, (ChanceUtil.getRandom(15) * 3) + 7);
    runnable.setTask(SERVER.getScheduler().runTaskTimer(INST, runnable, 0, 10));

    inform("Your bow unleashes a powerful glowing fog.");
  }
}
