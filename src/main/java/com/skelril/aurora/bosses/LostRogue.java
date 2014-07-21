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
import com.skelril.OSBL.bukkit.util.BukkitAttackDamage;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import com.skelril.OSBL.util.DamageSource;
import com.skelril.aurora.bosses.detail.WBossDetail;
import com.skelril.aurora.bosses.instruction.WDamageModifier;
import com.skelril.aurora.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import com.skelril.aurora.modifiers.ModifierType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.DamageUtil;
import com.skelril.aurora.util.EntityUtil;
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;

public class LostRogue {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<WBossDetail> lostRogue;

    public LostRogue() {
        lostRogue = new BukkitBossDeclaration<WBossDetail>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Lost Rogue");
            }
        };
        setupLostRogue();
    }

    public void bind(Damageable entity, WBossDetail detail) {
        lostRogue.bind(new BukkitBoss<>(entity, detail));
    }

    private void setupLostRogue() {
        List<BindInstruction<WBossDetail>> bindInstructions = lostRogue.bindInstructions;
        bindInstructions.add(new BindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, BindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Lost Rogue");
                    int level = controllable.getDetail().getLevel();
                    ((LivingEntity) anEntity).setMaxHealth(20 * 75 * level);
                    ((LivingEntity) anEntity).setHealth(20 * 75 * level);
                }
                return null;
            }
        });

        List<UnbindInstruction<WBossDetail>> unbindInstructions = lostRogue.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                double min = 4;
                double max = 9;
                float force = (float) Math.min(max, Math.max(min, (min + controllable.getDetail().getLevel()) / 2));
                boss.getWorld().createExplosion(x, y, z, force, false, true);
                return null;
            }
        });
        unbindInstructions.add(new UnbindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double baseLevel = controllable.getDetail().getLevel();
                List<ItemStack> itemStacks = new ArrayList<>();
                for (int i = 0; i < baseLevel * ChanceUtil.getRangedRandom(30, 100); i++) {
                    itemStacks.add(new ItemStack(BlockID.GOLD_BLOCK));
                }
                if (getModifierCenter().isActive(ModifierType.DOUBLE_WILD_DROPS)) {
                    itemStacks.addAll(itemStacks.stream().map(ItemStack::clone).collect(Collectors.toList()));
                }
                for (ItemStack itemStack : itemStacks) {
                    target.getWorld().dropItem(target, itemStack);
                }
                return null;
            }
        });

        List<DamageInstruction<WBossDetail>> damageInstructions = lostRogue.damageInstructions;
        damageInstructions.add(new WDamageModifier());
        damageInstructions.add(new DamageInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, DamageInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity)) return null;
                LivingEntity toHit = (LivingEntity) eToHit;
                toHit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 2), true);

                DamageUtil.multiplyFinalDamage(getEvent(damage), 1.5);
                return null;
            }
        });
        damageInstructions.add(new DamageInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, DamageInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity)) return null;
                LivingEntity toHit = (LivingEntity) eToHit;
                toHit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 2), true);

                if (boss instanceof LivingEntity && ChanceUtil.getChance(5)) {
                    new Nightmare((LivingEntity) boss, toHit).activate();
                }
                return null;
            }
        });

        List<DamagedInstruction<WBossDetail>> damagedInstructions = lostRogue.damagedInstructions;
        damagedInstructions.add(new DamagedInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, DamagedInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, DamageSource damageSource, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                LocalEntity localToHit = damageSource.getDamagingEntity();
                if (localToHit == null) return null;
                Entity toHit = BukkitUtil.getBukkitEntity(localToHit);
                if (toHit instanceof LivingEntity) {
                    if (boss instanceof LivingEntity) {
                        ((LivingEntity) boss).getActivePotionEffects().clear();
                        ((LivingEntity) boss).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 25, 2), true);
                    }
                }
                DamageUtil.multiplyFinalDamage(getEvent(damage), .75);
                return null;
            }
        });
        damagedInstructions.add(new DamagedInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, DamagedInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, DamageSource damageSource, AttackDamage damage) {
                if (ChanceUtil.getChance(5)) {
                    Entity boss = BukkitUtil.getBukkitEntity(controllable);
                    Vector vel = boss.getLocation().getDirection();
                    vel.multiply(4);
                    vel.setY(Math.min(.8, Math.max(.175, vel.getY())));
                    boss.setVelocity(vel);
                }
                return null;
            }
        });
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
    }
}
