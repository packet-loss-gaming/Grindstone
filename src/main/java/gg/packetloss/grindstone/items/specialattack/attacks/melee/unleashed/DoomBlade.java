/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed;

import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class DoomBlade extends EntityAttack implements MeleeSpecial {

  public DoomBlade(LivingEntity owner, LivingEntity target) {
    super(owner, target);
  }

  @Override
  public void activate() {

    inform("Your weapon releases a huge burst of energy.");

    if (owner instanceof Player) {
      SERVER.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
    }

    double dmgTotal = 0;
    List<Entity> entityList = target.getNearbyEntities(6, 4, 6);
    entityList.add(target);
    for (Entity e : entityList) {
      if (e.isValid() && e instanceof LivingEntity) {
        if (e.equals(owner)) {
          continue;
        }
        double maxHit = ChanceUtil.getRangedRandom(150, 350);
        if (e instanceof Player) {
          if (owner instanceof Player && !PvPComponent.allowsPvP((Player) owner, (Player) e)) {
            continue;
          }
          maxHit = (1.0 / 3.0) * maxHit;
        }
        DamageUtil.damage(owner, (LivingEntity) e, maxHit);
        for (int i = 0; i < 20; i++) {
          e.getWorld().playEffect(e.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
        }
        dmgTotal += maxHit;
      }
    }
    inform("Your sword dishes out an incredible " + (int) Math.ceil(dmgTotal) + " damage!");
  }
}
