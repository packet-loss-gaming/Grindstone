package com.skelril.aurora.util.item;

import com.sk89q.commandbook.CommandBook;
import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.prayer.PrayerFX.HulkFX;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import org.bukkit.Effect;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class EffectUtil {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    public static class Fear {

        public static void fearBlaze(Player owner, LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, target.getHealth() * 20, 1));
            target.setFireTicks(owner.getHealth() * 20);
            ChatUtil.sendNotice(owner, "Your sword releases a deadly blaze.");
        }

        public static void poison(Player owner, LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, target.getHealth() * 10, 3));
            ChatUtil.sendNotice(owner, "Your weapon poisons its victim.");
        }

        public static void weaken(Player owner, LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, owner.getHealth() * 18, 2));
            owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, owner.getHealth() * 18, 2));
            ChatUtil.sendNotice(owner, "Your sword leaches strength from its victim.");
        }

        public static void confuse(Player owner, LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, owner.getHealth() * 18, 1));
            ChatUtil.sendNotice(owner, "Your sword confuses its victim.");
        }

        public static void wrath(final Player owner, final LivingEntity target, final int x, int y) {

            final int z = (((1 + ChanceUtil.getRandom(3)) * x) / y^2)^3 + 4;

            String damageRating;
            if (z > 50) {
                damageRating = "lethal";
            } else if (z > 40) {
                damageRating = "devastating";
            } else if (z > 30) {
                damageRating = "strong";
            } else if (z > 20) {
                damageRating = "frightening";
            } else {
                damageRating = "weak";
            }

            target.damage(z);
            for (int i = 0; i < y - 1; i++) {
                server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {
                    @Override
                    public void run() {

                        if (!target.isDead() && target.getHealth() < target.getMaxHealth()) {
                            RapidHitEvent event = new RapidHitEvent(owner, z);
                            server.getPluginManager().callEvent(event);
                            target.damage(event.getDamage(), owner);
                        }
                    }
                }, (i + 1) * 25);
            }
            ChatUtil.sendNotice(owner, "Your sword releases a series of rapid " + damageRating + " attacks.");
        }

        public static boolean disarm(Player owner, LivingEntity target) {

            ItemStack held;
            if (target instanceof Player) {
                held = ((Player) target).getItemInHand();
                if (held != null) held = held.clone();
                ((Player) target).setItemInHand(null);
            } else {
                held = target.getEquipment().getItemInHand();
                if (held != null) held = held.clone();
                target.getEquipment().setItemInHand(null);
            }
            if (held == null || held.getTypeId() == 0) return false;
            Item item = target.getWorld().dropItem(target.getLocation(), held);
            item.setPickupDelay(25);
            ChatUtil.sendNotice(owner, "Your bow disarms its victim.");
            return true;
        }

        public static void magicChain(Player owner, LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, owner.getHealth() * 18, 2));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, owner.getHealth() * 18, 2));
            ChatUtil.sendNotice(owner, "Your bow slows its victim.");
        }

        public static int fearStrike(Player owner, LivingEntity target, int x) {

            server.getPluginManager().callEvent(new RapidHitEvent(owner));

            for (Entity e : target.getNearbyEntities(8, 8, 8)) {
                if (e.isValid() && e instanceof LivingEntity) {
                    if (e.equals(owner)) continue;
                    if (e instanceof Player) {
                        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(
                                owner, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 2
                        );
                        server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) target));
                        server.getPluginManager().callEvent(event);
                        if (event.isCancelled()) continue;
                    }
                    e.setVelocity(owner.getLocation().getDirection().multiply(2).setY(Math.random() * 2 + 1.27));
                    e.setFireTicks(ChanceUtil.getRandom(20 * 60));
                }
            }
            ChatUtil.sendNotice(owner, "You fire a terrifyingly powerful shot.");

            return x * ChanceUtil.getRandom(2);
        }
    }

    public static class Master {

        public static void blind(Player owner, LivingEntity target) {

            if (target instanceof Player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 1));
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
                for (int i = 0; i < 20; i++) e.getWorld().playEffect(e.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                e.damage(200);
                dmgTotal += 200;
            }
            ChatUtil.sendNotice(owner, "Your sword dishes out an incredible " + dmgTotal + " damage!");
        }
    }

    public static class Ancient {

        public static void powerBurst(Player player, int attackDamage) {

            ChatUtil.sendNotice(player, "Your armour releases a burst of energy.");
            ChatUtil.sendNotice(player, "You are healed by an ancient force.");

            player.setHealth(Math.min(player.getHealth() + attackDamage, player.getMaxHealth()));

            for (Entity e : player.getNearbyEntities(8, 8, 8)) {
                if (e.isValid() && e instanceof LivingEntity) {
                    if (e instanceof Player) {
                        ((Player) e).setHealth(Math.min(((Player) e).getHealth() + attackDamage,
                                ((Player) e).getMaxHealth()));
                        ChatUtil.sendNotice((Player) e, "You are healed by an ancient force.");
                    } else if (EnvironmentUtil.isHostileEntity(e)) {
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
}
