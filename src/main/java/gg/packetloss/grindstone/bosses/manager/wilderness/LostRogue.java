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
import com.skelril.OSBL.util.DamageSource;
import gg.packetloss.grindstone.bosses.detail.WBossDetail;
import gg.packetloss.grindstone.bosses.instruction.ExplosiveUnbind;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.bosses.instruction.WDamageModifier;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.guild.rogue.Nightmare;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.Location;
import org.bukkit.Material;
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

public class LostRogue {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<WBossDetail> lostRogue;

    public LostRogue() {
        lostRogue = new BukkitBossDeclaration<>(inst, new SimpleInstructionDispatch<>()) {
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
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, BindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    anEntity.setCustomName("Lost Rogue");
                    int level = controllable.getDetail().getLevel();
                    ((LivingEntity) anEntity).setMaxHealth(20 * 75 * level);
                    ((LivingEntity) anEntity).setHealth(20 * 75 * level);

                    try {
                        AttributeBook.setAttribute((LivingEntity) anEntity, AttributeBook.Attribute.FOLLOW_RANGE, 150);
                    } catch (UnsupportedFeatureException ex) {
                        ex.printStackTrace();
                    }
                }
                return null;
            }
        });

        List<UnbindInstruction<WBossDetail>> unbindInstructions = lostRogue.unbindInstructions;
        unbindInstructions.add(new ExplosiveUnbind<>(true, false) {
            @Override
            public float getExplosionStrength(WBossDetail wBossDetail) {
                double min = 4;
                double max = 9;
                return (float) Math.min(max, Math.max(min, (min + wBossDetail.getLevel()) / 2));
            }
        });
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                int baseLevel = controllable.getDetail().getLevel();
                List<ItemStack> itemStacks = new ArrayList<>();

                for (int i = ChanceUtil.getRandom(baseLevel) * ChanceUtil.getRandom(5); i > 0; --i) {
                    itemStacks.add(new ItemStack(Material.GOLD_BLOCK, ChanceUtil.getRandom(32)));
                }

                if (ChanceUtil.getChance(Math.max(3, 20 - baseLevel))) {
                    itemStacks.add(CustomItemCenter.build(CustomItems.ROGUE_OATH));
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

        List<DamageInstruction<WBossDetail>> damageInstructions = lostRogue.damageInstructions;
        damageInstructions.add(new WDamageModifier());
        damageInstructions.add(new DamageInstruction<>() {
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
        damageInstructions.add(new DamageInstruction<>() {
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
        damagedInstructions.add(new HealthPrint<>());
        damagedInstructions.add(new DamagedInstruction<>() {
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
        damagedInstructions.add(new DamagedInstruction<>() {
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
