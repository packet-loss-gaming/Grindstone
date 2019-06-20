/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackPreDamageEvent;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.logging.Logger;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE;

public class DamageUtil {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    public static void multiplyFinalDamage(EntityDamageEvent event, double multiplier) {
        event.setDamage(BASE, Math.max(0, event.getDamage() + (event.getFinalDamage() * (multiplier - 1))));
    }

    public static boolean damageWithSpecialAttack(LivingEntity attacker, LivingEntity defender,
                                                  SpecialAttack spec, double amount) {
        if (defender.isDead()) {
            return false;
        }

        SpecialAttackPreDamageEvent event = new SpecialAttackPreDamageEvent(attacker, defender, spec, amount);
        server.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        event.getDefender().damage(event.getDamage(), attacker);
        return true;
    }
}
