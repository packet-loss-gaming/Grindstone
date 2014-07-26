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
import com.skelril.aurora.bosses.instruction.HealthPrint;
import com.skelril.aurora.bosses.instruction.WDamageModifier;
import com.skelril.aurora.modifiers.ModifierType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.EntityUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;
import static com.skelril.aurora.util.item.custom.CustomItems.*;

public class StormBringer {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<WBossDetail> stormBringer;

    public StormBringer() {
        stormBringer = new BukkitBossDeclaration<WBossDetail>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Storm Bringer");
            }
        };
        setupFangz();
    }

    public void bind(Damageable entity, WBossDetail detail) {
        stormBringer.bind(new BukkitBoss<>(entity, detail));
    }

    private void setupFangz() {
        List<BindInstruction<WBossDetail>> bindInstructions = stormBringer.bindInstructions;
        bindInstructions.add(new BindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, BindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Storm Bringer");
                    int level = controllable.getDetail().getLevel();
                    ((LivingEntity) anEntity).setMaxHealth(20 * 30 * level);
                    ((LivingEntity) anEntity).setHealth(20 * 30 * level);
                }
                return null;
            }
        });

        List<UnbindInstruction<WBossDetail>> unbindInstructions = stormBringer.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                int baseLevel = controllable.getDetail().getLevel();
                List<ItemStack> itemStacks = new ArrayList<>();
                itemStacks.add(CustomItemCenter.build(BAT_BOW));
                for (int i = baseLevel * ChanceUtil.getRandom(3); i > 0; --i) {
                    itemStacks.add(CustomItemCenter.build(BARBARIAN_BONE));
                }
                for (int i = baseLevel * ChanceUtil.getRandom(9); i > 0; --i) {
                    itemStacks.add(CustomItemCenter.build(GOD_FISH));
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

        List<DamageInstruction<WBossDetail>> damageInstructions = stormBringer.damageInstructions;
        damageInstructions.add(new WDamageModifier());
        damageInstructions.add(new DamageInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, DamageInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity) || !getEvent(damage).getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) return null;
                LivingEntity toHit = (LivingEntity) eToHit;

                Location target = toHit.getLocation();
                for (int i = controllable.getDetail().getLevel() * ChanceUtil.getRangedRandom(1, 10); i >= 0; --i) {
                    server.getScheduler().runTaskLater(inst, () -> {
                        // Simulate a lightning strike
                        LightningStrike strike = target.getWorld().strikeLightningEffect(target);
                        for (Entity e : strike.getNearbyEntities(2, 4, 2)) {
                            if (!e.isValid() || !(e instanceof LivingEntity)) continue;
                            // Pig Zombie
                            if (e instanceof Pig) {
                                e.getWorld().spawn(e.getLocation(), PigZombie.class);
                                e.remove();
                                continue;
                            }
                            // Creeper
                            if (e instanceof Creeper) {
                                ((Creeper) e).setPowered(true);
                            }
                            ((LivingEntity) e).damage(1, boss);
                        }
                    }, (5 * (6 + i)));
                }
                return null;
            }
        });

        List<DamagedInstruction<WBossDetail>> damagedInstructions = stormBringer.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
    }
}
