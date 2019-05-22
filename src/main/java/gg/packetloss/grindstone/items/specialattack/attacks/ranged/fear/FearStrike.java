/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class FearStrike extends EntityAttack implements RangedSpecial {

  public FearStrike(LivingEntity owner, LivingEntity target) {
    super(owner, target);
  }

  @Override
  public void activate() {

    if (owner instanceof Player) {
      SERVER.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
    }

    List<Entity> entityList = target.getNearbyEntities(8, 4, 8);
    entityList.add(target);
    for (Entity e : entityList) {
      if (e.isValid() && e instanceof LivingEntity) {
        if (e.equals(owner)) {
          continue;
        }
        if (e instanceof Player) {
          if (owner instanceof Player && !PvPComponent.allowsPvP((Player) owner, (Player) e)) {
            continue;
          }
          SERVER.getPluginManager().callEvent(new ThrowPlayerEvent((Player) e));
        }

        Vector velocity = owner.getLocation().getDirection().multiply(2);
        velocity.setY(Math.max(velocity.getY(), Math.random() * 2 + 1.27));
        e.setVelocity(velocity);
        DamageUtil.damage(owner, (LivingEntity) e, 10);
        e.setFireTicks(20 * (ChanceUtil.getRandom(40) + 20));
      }
    }
    inform("You fire a terrifyingly powerful shot.");
  }
}
