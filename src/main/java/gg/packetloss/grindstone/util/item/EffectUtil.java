/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.events.custom.item.ArmorBurstEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EffectUtil {
    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    public static class Ancient {

        public static void powerBurst(LivingEntity entity, double attackDamage) {

            if (entity instanceof Player) {
                ArmorBurstEvent event = new ArmorBurstEvent((Player) entity);
                CommandBook.callEvent(event);
                if (event.isCancelled()) {
                    return;
                }

                ChatUtil.sendNotice(entity, "Your armour releases a burst of energy.");
                ChatUtil.sendNotice(entity, "You are healed by an ancient force.");
            }

            EntityUtil.heal(entity, attackDamage);

            entity.getNearbyEntities(8, 8, 8).stream().filter(e -> e.isValid() && e instanceof LivingEntity).forEach(e -> {
                if (e instanceof Player && GeneralPlayerUtil.hasInvulnerableGamemode((Player) e)) {
                    return;
                }

                if (e.getType() == entity.getType()) {
                    ((LivingEntity) e).setHealth(Math.min(((LivingEntity) e).getHealth() + attackDamage,
                            ((LivingEntity) e).getMaxHealth()));
                    if (e instanceof Player) {
                        ChatUtil.sendNotice(e, "You are healed by an ancient force.");
                    }
                } else if (!(entity instanceof Player) || EntityUtil.isHostileMob(e)) {
                    if (e instanceof Player) {
                        server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) e));
                    }
                    e.setVelocity(new Vector(
                            Math.random() * 3 - 1.5,
                            Math.random() * 4,
                            Math.random() * 3 - 1.5
                    ));
                    e.setFireTicks(ChanceUtil.getRandom(20 * 60));
                }
            });
        }
    }

    public static class Necros {

        public static void deathStrike(LivingEntity entity, double attackDamage) {
            if (entity instanceof Player) {
                ArmorBurstEvent event = new ArmorBurstEvent((Player) entity);
                CommandBook.callEvent(event);
                if (event.isCancelled()) {
                    return;
                }

                ChatUtil.sendNotice(entity, "You feel a necrotic power sweep over your soul.");
            }

            EntityUtil.heal(entity, attackDamage * 1.7);

            entity.getNearbyEntities(8, 8, 8).stream().filter(e -> e.isValid() && e instanceof LivingEntity).forEach(e -> {
                if (e.getType() == entity.getType()) {
                    EntityUtil.heal(entity, attackDamage * 1.5);
                    if (e instanceof Player) {
                        ChatUtil.sendNotice(e, "You feel a necrotic power sweep over your soul.");
                    }
                } else if (!(entity instanceof Player) || EntityUtil.isHostileMob(e)) {
                    ((LivingEntity) e).damage(attackDamage * 1.9);
                }
            });
        }
    }

    public static class Strange {

        public static <T extends LivingEntity> void mobBarrage(Location target, Class<T> type) {


            final List<T> entities = new ArrayList<>();

            for (int i = 0; i < 125; i++) {
                T entity = target.getWorld().spawn(target, type);
                entity.setRemoveWhenFarAway(true);
                entities.add(entity);
            }

            server.getScheduler().runTaskLater(inst, () -> {
                for (T entity : entities) {
                    if (entity.isValid()) {
                        entity.remove();
                        for (int i = 0; i < 20; i++) {
                            entity.getWorld().playEffect(entity.getLocation(), Effect.SMOKE, 0);
                        }
                    }
                }
            }, 20 * 30);
        }
    }
}
