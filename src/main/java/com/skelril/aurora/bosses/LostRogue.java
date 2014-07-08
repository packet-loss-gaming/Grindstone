/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.bosses;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.entity.BukkitEntity;
import com.skelril.OSBL.bukkit.util.BukkitAttackDamage;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.Instruction;
import com.skelril.OSBL.instruction.InstructionResult;
import com.skelril.OSBL.util.AttackDamage;
import com.skelril.aurora.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.DamageUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.logging.Logger;

public class LostRogue {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration lostRogue;

    public LostRogue() {
        lostRogue = new BukkitBossDeclaration(inst);
        setupLostRogue();
    }

    public void bind(Damageable entity) {
        lostRogue.bind(new BukkitBoss(entity));
    }

    private void setupLostRogue() {
        List<Instruction> bindInstructions = lostRogue.bindInstructions;
        bindInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity anEntity = BukkitUtil.getBukkitEntity(entity);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Lost Rogue");
                    double hp = ((LivingEntity) anEntity).getMaxHealth();
                    ((LivingEntity) anEntity).setMaxHealth(hp * 15);
                    ((LivingEntity) anEntity).setHealth(hp * 15);
                }
                return null;
            }
        });

        List<Instruction> unbindInstructions = lostRogue.unbindInstructions;
        unbindInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                Location target = boss.getLocation();
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                double min = 4;
                double max = 9;
                float force = (float) Math.min(max, Math.max(min, (min + getBaseLevel(boss)) / 2));
                boss.getWorld().createExplosion(x, y, z, force, false, true);
                return null;
            }
        });
        unbindInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                Location target = boss.getLocation();
                double baseLevel = getBaseLevel(boss);
                for (int i = 0; i < baseLevel * ChanceUtil.getRangedRandom(30, 100); i++) {
                    target.getWorld().dropItem(target, new ItemStack(BlockID.GOLD_BLOCK));
                }
                return null;
            }
        });

        List<Instruction> damageInstructions = lostRogue.damageInstructions;
        damageInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                LivingEntity toHit = getLivingEntity(0, objects);
                AttackDamage damage = getDamage(1, objects);

                EntityDamageEvent event = getEvent(damage);
                if (!event.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
                    DamageUtil.multiplyFinalDamage(event, -1);
                } else {
                    event.setCancelled(true);
                }

                toHit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 2), true);
                return null;
            }
        });
        damageInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                LivingEntity toHit = getLivingEntity(0, objects);
                if (boss instanceof LivingEntity && ChanceUtil.getChance(5)) {
                    new Nightmare((LivingEntity) boss, toHit).activate();
                }
                return null;
            }
        });

        List<Instruction> damagedInstructions = lostRogue.damagedInstructions;
        damagedInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                LivingEntity toHit = getLivingEntity(0, objects);
                AttackDamage damage = getDamage(1, objects);

                DamageUtil.multiplyFinalDamage(getEvent(damage), .75);

                if (toHit != null) {
                    if (boss instanceof LivingEntity) {
                        ((LivingEntity) boss).getActivePotionEffects().clear();
                        ((LivingEntity) boss).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 25, 2), true);
                    }
                }
                return null;
            }
        });
        damagedInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                if (ChanceUtil.getChance(5)) {
                    Entity boss = BukkitUtil.getBukkitEntity(entity);
                    Vector vel = boss.getLocation().getDirection();
                    vel.multiply(4);
                    vel.setY(Math.min(.8, Math.max(.175, vel.getY())));
                    boss.setVelocity(vel);
                }
                return null;
            }
        });
    }

    private int getBaseLevel(Entity e) {
        return getLevel(e.getLocation());
    }

    private int getLevel(Location location) {
        return Math.max(0, Math.max(Math.abs(location.getBlockX()), Math.abs(location.getBlockZ())) / 1000) + 1;
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
    }

    private LivingEntity getLivingEntity(int index, Object... objects) {
        if (objects != null && objects.length > index) {
            if (objects[index] instanceof BukkitEntity) {
                Entity entity = BukkitUtil.getBukkitEntity(objects[index]);
                if (entity instanceof LivingEntity) {
                    return (LivingEntity) entity;
                }
            }
        }
        return null;
    }

    private AttackDamage getDamage(int index, Object... objects) {
        if (objects != null && objects.length > index) {
            if (objects[index] instanceof AttackDamage) {
                return (AttackDamage) objects[index];
            }
        }
        return null;
    }
}
