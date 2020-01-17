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
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class ChainLightning extends EntityAttack implements MeleeSpecial {

    public ChainLightning(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private void chainOn(LivingEntity target, Class<? extends LivingEntity> filterType, int depth, int delayModifier) {
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

            List<Entity> targets = target.getNearbyEntities(5, 5, 5).stream().filter(e -> {
                if (!filterType.isInstance(e)) {
                    return false;
                }

                if (e.equals(owner)) {
                    return false;
                }

                return true;
            }).collect(Collectors.toList());

            double damage = 15;
            if (targets.isEmpty()) {
                damage *= 3;
            }

            boolean isFirstRun = depth == 1;
            if (!DamageUtil.damageWithSpecialAttack(owner, target, this, damage) && !isFirstRun) {
                return;
            }

            target.getWorld().strikeLightningEffect(target.getLocation());

            int localDelayModifier = 0;
            for (Entity entity : targets) {
                int newDepth = depth + 1;

                if (ChanceUtil.getChance(3 * newDepth)) {
                    chainOn((LivingEntity) entity, filterType, newDepth, ++localDelayModifier);
                }
            }
        }, 4 * delayModifier);
    }

    @Override
    public void activate() {
        Class<? extends LivingEntity> filterType = target.getClass();
        if (Monster.class.isAssignableFrom(filterType)) {
            filterType = Monster.class;
        }

        chainOn(target, filterType, 1, 1);

        inform("Your sword unleashes a chain lightning attack.");
    }
}
