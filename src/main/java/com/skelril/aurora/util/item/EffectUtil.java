package com.skelril.aurora.util.item;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.skelril.aurora.events.anticheat.RapidHitEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.prayer.PrayerFX.HulkFX;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, target.getHealth() * 20, 0));
            target.setFireTicks(owner.getHealth() * 20);
            ChatUtil.sendNotice(owner, "Your sword releases a deadly blaze.");
        }

        public static void curse(Player owner, LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, owner.getHealth() * 24, 2));
            ChatUtil.sendNotice(owner, "Your weapon curses its victim.");
        }

        public static void weaken(Player owner, LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, owner.getHealth() * 18, 1));
            owner.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, owner.getHealth() * 18, 1));
            ChatUtil.sendNotice(owner, "Your sword leaches strength from its victim.");
        }

        public static void confuse(Player owner, LivingEntity target) {

            target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, owner.getHealth() * 18, 1));
            ChatUtil.sendNotice(owner, "Your sword confuses its victim.");
        }

        public static void soulSmite(final Player owner, final LivingEntity target) {

            final double targetHP = (double) target.getHealth() / (double) target.getMaxHealth();

            target.setHealth((int) Math.floor(((targetHP / 2) * target.getMaxHealth())));
            server.getScheduler().runTaskLater(inst, new Runnable() {

                @Override
                public void run() {

                    if (target.isValid()) {
                        double newTargetHP = (double) target.getHealth() / (double) target.getMaxHealth();
                        if (newTargetHP < targetHP) {
                            target.setHealth((int) Math.floor(target.getMaxHealth() * targetHP));
                        }
                    }
                    ChatUtil.sendNotice(owner, "Your sword releases its grasp on its victim.");
                }
            }, 20 * Math.min(20, target.getMaxHealth() / 5 + 1));
            ChatUtil.sendNotice(owner, "Your sword steals its victims heal for a short time.");
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

        public static int fearStrike(Player owner, LivingEntity target, int x, WorldGuardPlugin WG) {

            server.getPluginManager().callEvent(new RapidHitEvent(owner));

            RegionManager mgr = WG != null ? WG.getGlobalRegionManager().get(owner.getWorld()) : null;

            List<Entity> entityList = target.getNearbyEntities(8, 4, 8);
            entityList.add(target);
            for (Entity e : entityList) {
                if (e.isValid() && e instanceof LivingEntity) {
                    if (e.equals(owner)) continue;
                    if (e instanceof Player) {
                        if (mgr != null && !mgr.getApplicableRegions(e.getLocation()).allows(DefaultFlag.PVP)) {
                            continue;
                        }
                        server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) target));
                    }

                    Vector velocity = owner.getLocation().getDirection().multiply(2);
                    velocity.setY(Math.max(velocity.getY(), Math.random() * 2 + 1.27));
                    ChatUtil.sendNotice(owner, velocity.toString());
                    e.setVelocity(velocity);
                    e.setFireTicks(20 * ChanceUtil.getRandom(60));
                }
            }
            ChatUtil.sendNotice(owner, "You fire a terrifyingly powerful shot.");

            return x * ChanceUtil.getRangedRandom(2, 3);
        }

        public static void fearBomb(Player owner, LivingEntity target, WorldGuardPlugin WG) {

            final List<Block> blocks = new ArrayList<>();
            Block block = target.getLocation().getBlock();
            blocks.add(block);
            for (BlockFace blockFace : EnvironmentUtil.getNearbyBlockFaces()) {
                blocks.add(block.getRelative(blockFace));
            }

            List<Block> blockList = new ArrayList<>();
            for (BlockFace blockFace : EnvironmentUtil.getNearbyBlockFaces()) {
                for (Block aBlock : blocks) {
                    Block testBlock = aBlock.getRelative(blockFace);
                    if (!blocks.contains(testBlock) && !blockList.contains(testBlock)) blockList.add(testBlock);
                }
            }

            Collections.addAll(blocks, blockList.toArray(new Block[blockList.size()]));
            final RegionManager mgr = WG != null ? WG.getGlobalRegionManager().get(owner.getWorld()) : null;

            IntegratedRunnable bomb = new IntegratedRunnable() {

                @Override
                public void run(int times) {

                    for (Block block : blocks) {
                        for (int i = 0; i < 20; i++) {
                            block.getWorld().playEffect(block.getRelative(BlockFace.UP).getLocation(), Effect.SMOKE, 0);
                        }
                    }
                }

                @Override
                public void end() {

                    for (Block block : blocks) {
                        block.getWorld().createExplosion(block.getLocation(), 0F);
                        for (Entity entity : block.getChunk().getEntities()) {
                            if (!(entity instanceof LivingEntity)) continue;
                            if (entity instanceof Player) {
                                if (mgr != null) {
                                    if (!mgr.getApplicableRegions(entity.getLocation()).allows(DefaultFlag.PVP)) {
                                        continue;
                                    }
                                }
                            }
                            if (entity.getLocation().distanceSquared(block.getLocation()) <= 4) {
                                ((LivingEntity) entity).setHealth(0);
                            }
                        }
                    }
                }
            };

            TimedRunnable timedRunnable = new TimedRunnable(bomb, 6);

            BukkitTask task = server.getScheduler().runTaskTimer(inst, timedRunnable, 0, 20);
            timedRunnable.setTask(task);

            ChatUtil.sendNotice(owner, "Your bow creates a powerful bomb.");
        }
    }

    public static class Unleashed {

        public static void regen(Player owner, LivingEntity target) {

            owner.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, target.getHealth() * 10, 2));
            ChatUtil.sendNotice(owner, "You gain a healing aura.");
        }

        public static void speed(Player owner, LivingEntity target) {

            owner.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, owner.getHealth() * 18, 2));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, owner.getHealth() * 18, 2));
            ChatUtil.sendNotice(owner, "You gain a agile advantage over your opponent.");
        }

        public static void lifeLeech(Player owner, LivingEntity target) {

            final int ownerMax = owner.getMaxHealth();

            final double ownerHP = (double) owner.getHealth() / (double) ownerMax;
            final double targetHP = (double) target.getHealth() / (double) target.getMaxHealth();

            if (ownerHP > targetHP) {
                owner.setHealth(owner.getMaxHealth());
                ChatUtil.sendNotice(owner, "Your weapon fully heals you.");
            } else {
                target.setHealth((int) Math.floor(target.getMaxHealth() * ownerHP));
                owner.setHealth((int) Math.min(ownerMax, Math.floor(ownerMax * targetHP * 1.25)));
                ChatUtil.sendNotice(owner, "You leech the health of your foe.");
            }
        }

        public static void blind(Player owner, LivingEntity target) {

            if (target instanceof Player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 0));
                ChatUtil.sendNotice(owner, "Your weapon blinds your victim.");
            } else {
                healingLight(owner, target);
            }
        }

        public static void healingLight(Player owner, LivingEntity target) {

            server.getPluginManager().callEvent(new RapidHitEvent(owner));

            owner.setHealth(Math.min(owner.getMaxHealth(), owner.getHealth() + 5));
            for (int i = 0; i < 4; i++) {
                target.getWorld().playEffect(target.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
            }

            target.damage(20, owner);
            ChatUtil.sendNotice(owner, "Your weapon glows dimly.");
        }

        public static void doomBlade(Player owner, LivingEntity target, WorldGuardPlugin WG) {

            ChatUtil.sendNotice(owner, "Your weapon releases a huge burst of energy.");

            server.getPluginManager().callEvent(new RapidHitEvent(owner));

            RegionManager mgr = WG != null ? WG.getGlobalRegionManager().get(owner.getWorld()) : null;

            int dmgTotal = 0;
            List<Entity> entityList = target.getNearbyEntities(6, 4, 6);
            entityList.add(target);
            for (Entity e : entityList) {
                if (e.isValid() && e instanceof LivingEntity) {
                    if (e.equals(owner)) continue;
                    int maxHit = ChanceUtil.getRangedRandom(150, 350);
                    if (e instanceof Player) {
                        if (mgr != null && !mgr.getApplicableRegions(e.getLocation()).allows(DefaultFlag.PVP)) {
                            continue;
                        }
                        maxHit = (int) ((1.0 / 3.0) * maxHit);
                    }
                    ((LivingEntity) e).damage(maxHit, owner);
                    for (int i = 0; i < 20; i++) e.getWorld().playEffect(e.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                    dmgTotal += maxHit;
                }
            }
            ChatUtil.sendNotice(owner, "Your sword dishes out an incredible " + dmgTotal + " damage!");
        }
    }

    public static class Master {

        public static void blind(Player owner, LivingEntity target) {

            if (target instanceof Player) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 0));
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

    public static class Strange {

        public static void mobBarrage(Player owner, Location location, EntityType type) {

            final List<Entity> entities = new ArrayList<>();

            for (int i = 0; i < 125; i++) {

                entities.add(location.getWorld().spawnEntity(location, type));
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

            if (type == EntityType.BAT) {
                ChatUtil.sendNotice(owner, "Your bow releases a batty attack.");
            } else {
                ChatUtil.sendNotice(owner, "Your bow releases a " + type.getName().toLowerCase() + " attack.");
            }
        }
    }
}
