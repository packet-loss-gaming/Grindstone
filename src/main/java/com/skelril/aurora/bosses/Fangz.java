/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.bosses;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitAttackDamage;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import com.skelril.aurora.bosses.detail.WBossDetail;
import com.skelril.aurora.bosses.instruction.WDamageModifier;
import com.skelril.aurora.modifiers.ModifierType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.DamageUtil;
import com.skelril.aurora.util.EntityUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;
import static com.skelril.aurora.util.item.custom.CustomItems.POTION_OF_RESTITUTION;
import static com.skelril.aurora.util.item.custom.CustomItems.SCROLL_OF_SUMMATION;

public class Fangz {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<WBossDetail> fangz;

    public Fangz() {
        fangz = new BukkitBossDeclaration<WBossDetail>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Fangz");
            }
        };
        setupFangz();
    }

    public void bind(Damageable entity, WBossDetail detail) {
        fangz.bind(new BukkitBoss<>(entity, detail));
    }

    private void setupFangz() {
        List<BindInstruction<WBossDetail>> bindInstructions = fangz.bindInstructions;
        bindInstructions.add(new BindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, BindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Fangz");
                    int level = controllable.getDetail().getLevel();
                    ((LivingEntity) anEntity).setMaxHealth(20 * 50 * level);
                    ((LivingEntity) anEntity).setHealth(20 * 50 * level);
                }
                return null;
            }
        });

        List<UnbindInstruction<WBossDetail>> unbindInstructions = fangz.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                for (Entity aEntity : boss.getNearbyEntities(7, 4, 7)) {
                    if (!(aEntity instanceof LivingEntity)) continue;
                    ((LivingEntity) aEntity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 1), true);
                    ((LivingEntity) aEntity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 30, 1), true);
                }
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
                for (int i = 0; i < baseLevel * ChanceUtil.getRandom(3); i++) {
                    itemStacks.add(CustomItemCenter.build(POTION_OF_RESTITUTION));
                }
                for (int i = 0; i < baseLevel * ChanceUtil.getRandom(10); i++) {
                    itemStacks.add(CustomItemCenter.build(SCROLL_OF_SUMMATION));
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

        List<DamageInstruction<WBossDetail>> damageInstructions = fangz.damageInstructions;
        damageInstructions.add(new WDamageModifier());
        damageInstructions.add(new DamageInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, DamageInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity)) return null;
                LivingEntity toHit = (LivingEntity) eToHit;

                DamageUtil.multiplyFinalDamage(getEvent(damage), 2);
                EntityUtil.heal(boss, damage.getDamage());

                toHit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 0), true);
                toHit.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 15, 0), true);
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
