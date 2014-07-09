/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.bosses;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.entity.BukkitEntity;
import com.skelril.OSBL.bukkit.util.BukkitAttackDamage;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.Instruction;
import com.skelril.OSBL.instruction.InstructionResult;
import com.skelril.OSBL.util.AttackDamage;
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

    private BukkitBossDeclaration fangz;

    public Fangz() {
        fangz = new BukkitBossDeclaration(inst) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                Entity anEntity = BukkitUtil.getBukkitEntity(entity);
                return anEntity instanceof LivingEntity && ((LivingEntity) anEntity).getCustomName().equals("Fangz");
            }
        };
        setupFangz();
    }

    public void bind(Damageable entity) {
        fangz.bind(new BukkitBoss(entity));
    }

    private void setupFangz() {
        List<Instruction> bindInstructions = fangz.bindInstructions;
        bindInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity anEntity = BukkitUtil.getBukkitEntity(entity);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Fangz");
                    double hp = ((LivingEntity) anEntity).getMaxHealth();
                    ((LivingEntity) anEntity).setMaxHealth(hp * 10);
                    ((LivingEntity) anEntity).setHealth(hp * 10);
                }
                return null;
            }
        });

        List<Instruction> unbindInstructions = fangz.unbindInstructions;
        unbindInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                for (Entity aEntity : boss.getNearbyEntities(7, 4, 7)) {
                    if (!(aEntity instanceof LivingEntity)) continue;
                    ((LivingEntity) aEntity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 1), true);
                    ((LivingEntity) aEntity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 30, 1), true);
                }
                return null;
            }
        });
        unbindInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                Location target = boss.getLocation();
                double baseLevel = getBaseLevel(boss);
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

        List<Instruction> damageInstructions = fangz.damageInstructions;
        damageInstructions.add(new Instruction() {
            @Override
            public InstructionResult execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                LivingEntity toHit = getLivingEntity(0, objects);
                AttackDamage damage = getDamage(1, objects);

                DamageUtil.multiplyFinalDamage(getEvent(damage), 2);
                EntityUtil.heal(boss, damage.getDamage());

                toHit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 0), true);
                toHit.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 15, 0), true);
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
