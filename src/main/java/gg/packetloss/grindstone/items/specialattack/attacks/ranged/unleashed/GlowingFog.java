/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.unleashed;

import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttackFactory;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GlowingFog extends EntityAttack implements RangedSpecial {

    public GlowingFog(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    @Override
    public void activate() {
        final Location targeted = target.getLocation();

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(10);
        taskBuilder.setNumberOfRuns((ChanceUtil.getRandom(15) * 3) + 7);

        taskBuilder.setAction((times) -> {
            if (owner instanceof Player) {
                server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
            }

            EnvironmentUtil.generateRadialEffect(targeted, Effect.MOBSPAWNER_FLAMES);

            Class<? extends Entity> filterType = target.getClass();
            if (Monster.class.isAssignableFrom(filterType)) {
                filterType = Monster.class;
            }

            for (Entity aEntity : targeted.getNearbyEntitiesByType(filterType, 4)) {
                if (!aEntity.isValid() || aEntity.equals(owner)) continue;

                SpecialAttackFactory.processDamage(owner, (LivingEntity) aEntity, this, 5);
            }
            return true;
        });

        taskBuilder.build();

        inform("Your bow unleashes a powerful glowing fog.");
    }
}
