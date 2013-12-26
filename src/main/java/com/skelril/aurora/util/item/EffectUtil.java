package com.skelril.aurora.util.item;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.prayer.PrayerFX.HulkFX;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class EffectUtil {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    public static class Master {

        public static void blind(Player owner, LivingEntity target) {

            if (target instanceof Player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 0), true);
                ChatUtil.sendNotice(owner, "Your weapon blinds your victim.");
            } else {
                healingLight(owner, target);
            }
        }

        public static void healingLight(Player owner, LivingEntity target) {

            owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + 5));
            for (int i = 0; i < 4; i++) {
                target.getWorld().playEffect(target.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
            }

            target.damage(20);
            ChatUtil.sendNotice(owner, "Your weapon glows dimly.");
        }

        public static void ultimateStrength(Player owner) {

            new HulkFX().add(owner);
            ChatUtil.sendNotice(owner, "You gain a new sense of true power.");
        }

        public static void doomBlade(Player owner, Collection<LivingEntity> entities) {

            ChatUtil.sendNotice(owner, "The Master Sword releases a huge burst of energy.");

            int dmgTotal = 0;
            for (LivingEntity e : entities) {
                int maxHit = ChanceUtil.getRangedRandom(150, 250);
                for (int i = 0; i < 20; i++) e.getWorld().playEffect(e.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                e.damage(maxHit);
                dmgTotal += maxHit;
            }
            ChatUtil.sendNotice(owner, "Your sword dishes out an incredible " + dmgTotal + " damage!");
        }
    }

    public static class Ancient {

        public static void powerBurst(LivingEntity entity, double attackDamage) {

            if (entity instanceof Player) {
                ChatUtil.sendNotice((Player) entity, "Your armour releases a burst of energy.");
                ChatUtil.sendNotice((Player) entity, "You are healed by an ancient force.");
            }

            entity.setHealth(Math.min(entity.getHealth() + attackDamage, entity.getMaxHealth()));

            for (Entity e : entity.getNearbyEntities(8, 8, 8)) {
                if (e.isValid() && e instanceof LivingEntity) {
                    if (e.getType() == entity.getType()) {
                        ((LivingEntity) e).setHealth(Math.min(((LivingEntity) e).getHealth() + attackDamage,
                                ((LivingEntity) e).getMaxHealth()));
                        if (e instanceof Player) {
                            ChatUtil.sendNotice((Player) e, "You are healed by an ancient force.");
                        }
                    } else if (!(entity instanceof Player) || EnvironmentUtil.isHostileEntity(e)) {
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
                }
            }
        }
    }

    public static class Strange {

        public static void mobBarrage(Location target, EntityType type) {


            final List<Entity> entities = new ArrayList<>();

            for (int i = 0; i < 125; i++) {

                Entity entity = target.getWorld().spawnEntity(target, type);
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).setRemoveWhenFarAway(true);
                }
                entities.add(entity);
            }

            server.getScheduler().runTaskLater(inst, new Runnable() {

                @Override
                public void run() {

                    for (Entity entity : entities) {

                        if (entity.isValid()) {
                            entity.remove();
                            for (int i = 0; i < 20; i++) {
                                entity.getWorld().playEffect(entity.getLocation(), Effect.SMOKE, 0);
                            }
                        }
                    }
                }
            }, 20 * 30);
        }
    }
}
