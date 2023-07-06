/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public class FearStrike extends EntityAttack implements RangedSpecial {

    public FearStrike(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    @Override
    public void activate() {

        if (owner instanceof Player) {
            CommandBook.callEvent(new RapidHitEvent((Player) owner));
        }

        List<Entity> entityList = target.getNearbyEntities(8, 4, 8);
        entityList.add(target);

        int distance = (int) target.getLocation().distance(owner.getLocation());
        double damage = 15 + ChanceUtil.getRandom(distance * 2);

        for (Entity e : entityList) {
            if (e.isValid() && e instanceof LivingEntity) {
                if (e.equals(owner)) continue;

                // Check this, and do the damage first so we correctly check PvP boundaries
                if (!DamageUtil.damageWithSpecialAttack(owner, (LivingEntity) e, this, damage)) {
                    continue;
                }

                if (e instanceof Player) {
                    CommandBook.callEvent(new ThrowPlayerEvent((Player) e));
                }

                // Set velocity/throw entity
                Vector velocity = owner.getLocation().getDirection().multiply(2);
                velocity.setY(Math.max(velocity.getY(), Math.random() * 2 + 1.27));
                e.setVelocity(velocity);

                // Light entity on fire
                e.setFireTicks(20 * 5);
            }
        }
        inform("You fire a terrifyingly powerful shot.");
    }
}
