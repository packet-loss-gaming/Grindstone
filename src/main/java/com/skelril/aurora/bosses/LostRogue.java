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
import com.skelril.aurora.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import com.skelril.aurora.modifiers.ModifierType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;

public class LostRogue {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration lostRogue;

    public LostRogue() {
        lostRogue = new BukkitBossDeclaration(inst, new SimpleInstructionDispatch()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                Entity anEntity = BukkitUtil.getBukkitEntity(entity);
                return anEntity instanceof LivingEntity && ((LivingEntity) anEntity).getCustomName().equals("Lost Rogue");
            }
        };
        setupLostRogue();
    }

    public void bind(Damageable entity) {
        lostRogue.bind(new BukkitBoss(entity));
    }

    private void setupLostRogue() {
        List<BindInstruction> bindInstructions = lostRogue.bindInstructions;
        bindInstructions.add(new BindInstruction() {
            @Override
            public InstructionResult<BindInstruction> process(LocalControllable controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Lost Rogue");
                    double hp = ((LivingEntity) anEntity).getMaxHealth();
                    ((LivingEntity) anEntity).setMaxHealth(hp * 15);
                    ((LivingEntity) anEntity).setHealth(hp * 15);
                }
                return null;
            }
        });

        List<UnbindInstruction> unbindInstructions = lostRogue.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction() {
            @Override
            public InstructionResult<UnbindInstruction> process(LocalControllable controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
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
        unbindInstructions.add(new UnbindInstruction() {
            @Override
            public InstructionResult<UnbindInstruction> process(LocalControllable controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double baseLevel = getBaseLevel(boss);
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

        List<DamageInstruction> damageInstructions = lostRogue.damageInstructions;
        damageInstructions.add(new DamageInstruction() {
            @Override
            public InstructionResult<DamageInstruction> process(LocalControllable controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity)) return null;
                LivingEntity toHit = (LivingEntity) eToHit;
                toHit.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 2), true);

                DamageUtil.multiplyFinalDamage(getEvent(damage), 1.5);
                return null;
            }
        });
        damageInstructions.add(new DamageInstruction() {
            @Override
            public InstructionResult<DamageInstruction> process(LocalControllable controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
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

        List<DamagedInstruction> damagedInstructions = lostRogue.damagedInstructions;
        damagedInstructions.add(new DamagedInstruction() {
            @Override
            public InstructionResult<DamagedInstruction> process(LocalControllable controllable, DamageSource damageSource, AttackDamage damage) {
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
        damagedInstructions.add(new DamagedInstruction() {
            @Override
            public InstructionResult<DamagedInstruction> process(LocalControllable controllable, DamageSource damageSource, AttackDamage damage) {
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
}
