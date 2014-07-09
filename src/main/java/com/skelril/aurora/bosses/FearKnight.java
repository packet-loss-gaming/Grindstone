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
import com.skelril.aurora.items.specialattack.EntityAttack;
import com.skelril.aurora.items.specialattack.attacks.hybrid.fear.Curse;
import com.skelril.aurora.items.specialattack.attacks.melee.fear.*;
import com.skelril.aurora.modifiers.ModifierType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.EntityUtil;
import com.skelril.aurora.util.item.custom.CustomItemCenter;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;
import static com.skelril.aurora.util.item.custom.CustomItems.*;

public class FearKnight {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration fearKnight;

    public FearKnight() {
        fearKnight = new BukkitBossDeclaration(inst, new SimpleInstructionDispatch()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Fear Knight");
            }
        };
        setupFearKnight();
    }

    public void bind(Damageable entity) {
        fearKnight.bind(new BukkitBoss(entity));
    }

    private void setupFearKnight() {
        List<BindInstruction> bindInstructions = fearKnight.bindInstructions;
        bindInstructions.add(new BindInstruction() {
            @Override
            public InstructionResult<BindInstruction> process(LocalControllable controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Fear Knight");
                    double hp = ((LivingEntity) anEntity).getMaxHealth();
                    ((LivingEntity) anEntity).setMaxHealth(hp * 6);
                    ((LivingEntity) anEntity).setHealth(hp * 6);

                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    equipment.setHelmet(CustomItemCenter.build(GOD_HELMET));
                    equipment.setHelmetDropChance(.25F);
                    equipment.setChestplate(CustomItemCenter.build(GOD_CHESTPLATE));
                    equipment.setChestplateDropChance(.25F);
                    equipment.setLeggings(CustomItemCenter.build(GOD_LEGGINGS));
                    equipment.setLeggingsDropChance(.25F);
                    equipment.setBoots(CustomItemCenter.build(GOD_BOOTS));
                    equipment.setBootsDropChance(.25F);

                    equipment.setItemInHand(CustomItemCenter.build(FEAR_SWORD));
                    equipment.setItemInHandDropChance(.001F);
                }
                return null;
            }
        });

        List<UnbindInstruction> unbindInstructions = fearKnight.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction() {
            @Override
            public InstructionResult<UnbindInstruction> process(LocalControllable controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double baseLevel = getBaseLevel(boss);
                List<ItemStack> itemStacks = new ArrayList<>();
                for (int i = 0; i < baseLevel; i++) {
                    ItemStack stack;
                    switch (ChanceUtil.getRandom(3)) {
                        case 1:
                            stack = CustomItemCenter.build(GEM_OF_DARKNESS);
                            break;
                        case 2:
                            stack = CustomItemCenter.build(GEM_OF_LIFE);
                            break;
                        case 3:
                            stack = CustomItemCenter.build(IMBUED_CRYSTAL);
                            break;
                        default:
                            return null;
                    }
                    itemStacks.add(stack);
                    itemStacks.add(CustomItemCenter.build(PHANTOM_GOLD));
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
        unbindInstructions.add(new UnbindInstruction() {
            @Override
            public InstructionResult<UnbindInstruction> process(LocalControllable controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double baseLevel = getBaseLevel(boss);
                List<ItemStack> itemStacks = new ArrayList<>();
                if (ChanceUtil.getChance(5 * (100 - baseLevel))) {
                    itemStacks.add(CustomItemCenter.build(PHANTOM_HYMN));
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
        unbindInstructions.add(new UnbindInstruction() {
            @Override
            public InstructionResult<UnbindInstruction> process(LocalControllable controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double baseLevel = getBaseLevel(boss);
                List<ItemStack> itemStacks = new ArrayList<>();
                if (ChanceUtil.getChance(10 * (100 - baseLevel))) {
                    itemStacks.add(CustomItemCenter.build(PHANTOM_CLOCK));
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
        List<DamageInstruction> damageInstructions = fearKnight.damageInstructions;
        damageInstructions.add(new DamageInstruction() {
            @Override
            public InstructionResult<DamageInstruction> process(LocalControllable controllable, LocalEntity entity, AttackDamage damage) {
                Entity attacker = BukkitUtil.getBukkitEntity(entity);
                LivingEntity boss;
                if (attacker instanceof LivingEntity) {
                    boss = (LivingEntity) attacker;
                } else {
                    return null;
                }
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity)) return null;
                LivingEntity toHit = (LivingEntity) eToHit;

                EntityAttack spec;
                switch (ChanceUtil.getRandom(6)) {
                    case 1:
                        spec = new Confuse(boss, toHit);
                        break;
                    case 2:
                        spec = new FearBlaze(boss, toHit);
                        break;
                    case 3:
                        spec = new Curse(boss, toHit);
                        break;
                    case 4:
                        spec = new Weaken(boss, toHit);
                        break;
                    case 5:
                        spec = new Decimate(boss, toHit);
                        break;
                    case 6:
                        spec = new SoulSmite(boss, toHit);
                        break;
                    default:
                        return null;
                }
                spec.activate();
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
