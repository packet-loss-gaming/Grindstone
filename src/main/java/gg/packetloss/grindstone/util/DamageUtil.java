/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.sk89q.commandbook.CommandBook;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE;

public class DamageUtil {

  private static final CommandBook INST = CommandBook.inst();
  private static final Logger LOG = INST.getLogger();
  private static final Server SERVER = CommandBook.server();

  private static Map<Entity, Entity> entries = new ConcurrentHashMap<>();

  public static void damage(LivingEntity attacker, LivingEntity defender, double amount) {

    entries.put(attacker, defender);

    defender.damage(amount, attacker);
  }

  public static boolean remove(Entity attacker, Entity defender) {

    Entity testDefender = entries.remove(attacker);

    return testDefender != null && testDefender.equals(defender);
  }

  public static void multiplyFinalDamage(EntityDamageEvent event, double multiplier) {
    event.setDamage(BASE, Math.max(0, event.getDamage() + (event.getFinalDamage() * (multiplier - 1))));
  }
}
