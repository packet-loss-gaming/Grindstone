/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.manager.wilderness;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitAttackDamage;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import gg.packetloss.grindstone.bosses.detail.WBossDetail;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.bosses.instruction.WDamageModifier;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.implementations.FearSwordImpl;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
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

public class FearKnight {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<WBossDetail> fearKnight;

    public FearKnight() {
        fearKnight = new BukkitBossDeclaration<>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Fear Knight");
            }
        };
        setupFearKnight();
    }

    public void bind(Damageable entity, WBossDetail detail) {
        fearKnight.bind(new BukkitBoss<>(entity, detail));
    }

    private void setupFearKnight() {
        List<BindInstruction<WBossDetail>> bindInstructions = fearKnight.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, BindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    anEntity.setCustomName("Fear Knight");
                    int level = controllable.getDetail().getLevel();
                    ((LivingEntity) anEntity).setMaxHealth(20 * 30 * level);
                    ((LivingEntity) anEntity).setHealth(20 * 30 * level);

                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    equipment.setHelmet(CustomItemCenter.build(CustomItems.GOD_HELMET));
                    equipment.setHelmetDropChance(.25F);
                    equipment.setChestplate(CustomItemCenter.build(CustomItems.GOD_CHESTPLATE));
                    equipment.setChestplateDropChance(.25F);
                    equipment.setLeggings(CustomItemCenter.build(CustomItems.GOD_LEGGINGS));
                    equipment.setLeggingsDropChance(.25F);
                    equipment.setBoots(CustomItemCenter.build(CustomItems.GOD_BOOTS));
                    equipment.setBootsDropChance(.25F);

                    equipment.setItemInHand(CustomItemCenter.build(CustomItems.FEAR_SWORD));
                    equipment.setItemInHandDropChance(.001F);

                    try {
                        AttributeBook.setAttribute((LivingEntity) anEntity, AttributeBook.Attribute.FOLLOW_RANGE, 150);
                    } catch (UnsupportedFeatureException ex) {
                        ex.printStackTrace();
                    }
                }
                return null;
            }
        });

        List<UnbindInstruction<WBossDetail>> unbindInstructions = fearKnight.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                int baseLevel = controllable.getDetail().getLevel();
                List<ItemStack> itemStacks = new ArrayList<>();
                for (int i = 0; i < baseLevel; i++) {
                    ItemStack stack;
                    switch (ChanceUtil.getRandom(3)) {
                        case 1:
                            stack = CustomItemCenter.build(CustomItems.GEM_OF_DARKNESS);
                            break;
                        case 2:
                            stack = CustomItemCenter.build(CustomItems.GEM_OF_LIFE);
                            break;
                        case 3:
                            stack = CustomItemCenter.build(CustomItems.IMBUED_CRYSTAL);
                            break;
                        default:
                            return null;
                    }
                    itemStacks.add(stack);
                    itemStacks.add(CustomItemCenter.build(CustomItems.PHANTOM_GOLD));
                }
                if (ModifierComponent.getModifierCenter().isActive(ModifierType.DOUBLE_WILD_DROPS)) {
                    itemStacks.addAll(itemStacks.stream().map(ItemStack::clone).collect(Collectors.toList()));
                }
                for (ItemStack itemStack : itemStacks) {
                    target.getWorld().dropItem(target, itemStack);
                }
                return null;
            }
        });
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double baseLevel = controllable.getDetail().getLevel();
                List<ItemStack> itemStacks = new ArrayList<>();
                if (ChanceUtil.getChance(5 * (100 - baseLevel))) {
                    itemStacks.add(CustomItemCenter.build(CustomItems.PHANTOM_HYMN));
                }
                if (ModifierComponent.getModifierCenter().isActive(ModifierType.DOUBLE_WILD_DROPS)) {
                    itemStacks.addAll(itemStacks.stream().map(ItemStack::clone).collect(Collectors.toList()));
                }
                for (ItemStack itemStack : itemStacks) {
                    target.getWorld().dropItem(target, itemStack);
                }
                return null;
            }
        });
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double baseLevel = controllable.getDetail().getLevel();
                List<ItemStack> itemStacks = new ArrayList<>();
                if (ChanceUtil.getChance(10 * (100 - baseLevel))) {
                    itemStacks.add(CustomItemCenter.build(CustomItems.PHANTOM_CLOCK));
                }
                if (ModifierComponent.getModifierCenter().isActive(ModifierType.DOUBLE_WILD_DROPS)) {
                    itemStacks.addAll(itemStacks.stream().map(ItemStack::clone).collect(Collectors.toList()));
                }
                for (ItemStack itemStack : itemStacks) {
                    target.getWorld().dropItem(target, itemStack);
                }
                return null;
            }
        });
        List<DamageInstruction<WBossDetail>> damageInstructions = fearKnight.damageInstructions;
        damageInstructions.add(new WDamageModifier());
        FearSwordImpl sword = new FearSwordImpl();
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, DamageInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity attacker = BukkitUtil.getBukkitEntity(controllable);
                LivingEntity boss;
                if (attacker instanceof LivingEntity) {
                    boss = (LivingEntity) attacker;
                } else {
                    return null;
                }
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity)) return null;
                sword.getSpecial(boss, null, (LivingEntity) eToHit).activate();
                return null;
            }
        });

        List<DamagedInstruction<WBossDetail>> damagedInstructions = fearKnight.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
    }
}
