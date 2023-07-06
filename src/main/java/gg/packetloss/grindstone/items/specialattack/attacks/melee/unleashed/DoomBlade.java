/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.unleashed;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DoomBlade extends EntityAttack implements MeleeSpecial {

    public DoomBlade(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    @Override
    public void activate() {

        inform("Your weapon releases a huge burst of energy.");

        if (owner instanceof Player) {
            CommandBook.callEvent(new RapidHitEvent((Player) owner));
        }

        Class<? extends Entity> filterType = target.getClass();
        if (Monster.class.isAssignableFrom(filterType)) {
            filterType = Monster.class;
        }

        double dmgTotal = 0;
        List<Entity> entityList = target.getNearbyEntities(6, 4, 6);
        entityList.add(target);
        for (Entity e : entityList) {
            if (e.isValid() && filterType.isInstance(e)) {
                if (e.equals(owner)) continue;
                double maxHit = ChanceUtil.getRangedRandom(150, 350);
                if (e instanceof Player) {
                    maxHit = (1.0 / 5.0) * maxHit;
                }

                if (!DamageUtil.damageWithSpecialAttack(owner, (LivingEntity) e, this, maxHit)) {
                    continue;
                }

                for (int i = 0; i < 20; i++) e.getWorld().playEffect(e.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                dmgTotal += maxHit;
            }
        }
        inform("Your sword dishes out an incredible " + (int) Math.ceil(dmgTotal) + " damage!");
    }
}
