/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowball;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Nightmare extends EntityAttack implements MeleeSpecial {

  private Random r;

  public Nightmare(LivingEntity owner, LivingEntity target) {
    super(owner, target);
    r = new Random(System.currentTimeMillis());
  }

  @Override
  public void activate() {

    inform("You unleash a nightmare upon the plane.");

    final Set<Location> locations = new HashSet<>();

    Location origin = target.getLocation().add(0, 5, 0);

    for (int i = 0; i < 100; i++) {

      double angle = r.nextDouble() * Math.PI * 2;
      double radius = r.nextDouble() * 12;

      Location pt = origin.clone();
      pt.setX(origin.getX() + radius * Math.cos(angle));
      pt.setZ(origin.getZ() + radius * Math.sin(angle));

      locations.add(pt);
    }

    IntegratedRunnable hellFire = new IntegratedRunnable() {
      @Override
      public boolean run(int times) {
        locations.stream().filter(location -> ChanceUtil.getChance(3)).forEach(location -> {
          Snowball snowball = location.getWorld().spawn(location, Snowball.class);
          snowball.setMetadata("rogue-snowball", new FixedMetadataValue(INST, true));
          snowball.setMetadata("nightmare", new FixedMetadataValue(INST, true));
          snowball.setShooter(owner);
        });
        return true;
      }

      @Override
      public void end() {
        inform("Your nightmare fades away...");
      }
    };

    TimedRunnable runnable = new TimedRunnable(hellFire, 40);
    runnable.setTask(SERVER.getScheduler().runTaskTimer(INST, runnable, 50, 10));
  }
}
