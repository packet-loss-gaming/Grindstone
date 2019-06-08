/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.fear;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ChainLightning extends EntityAttack implements MeleeSpecial {

    public ChainLightning(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    private void chainOn(LivingEntity target, int depth, int delayModifier) {
        if (depth != 1 && !ChanceUtil.getChance(3 * depth)) {
            return;
        }

        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            if (owner.isDead()) {
                return;
            }

            if (!owner.hasLineOfSight(target)) {
                return;
            }

            if (owner instanceof Player) {
                server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
            }

            target.getWorld().strikeLightningEffect(target.getLocation());
            DamageUtil.damageWithSpecialAttack(owner, target, this, 15);

            int localDelayModifier = 0;
            for (Entity entity : target.getNearbyEntities(5, 5, 5)) {
                if (!(entity instanceof LivingEntity)) {
                    continue;
                }

                if (entity.equals(owner)) {
                    continue;
                }

                chainOn((LivingEntity) entity, depth + 1, ++localDelayModifier);
            }
        }, 10 * delayModifier);
    }

    @Override
    public void activate() {
        chainOn(target, 1, 1);

        inform("Your sword unleashes a chain lightning attack.");
    }
}
