/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.bosses;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
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
import com.skelril.aurora.modifiers.ModifierType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.EntityUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.skelril.aurora.modifiers.ModifierComponent.getModifierCenter;

public class GraveDigger {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<WBossDetail> graveDigger;

    public GraveDigger() {
        graveDigger = new BukkitBossDeclaration<WBossDetail>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Grave Digger");
            }
        };
        setupFangz();
    }

    public void bind(Damageable entity, WBossDetail detail) {
        graveDigger.bind(new BukkitBoss<>(entity, detail));
    }

    private void setupFangz() {
        List<BindInstruction<WBossDetail>> bindInstructions = graveDigger.bindInstructions;
        bindInstructions.add(new BindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, BindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Grave Digger");
                    int level = controllable.getDetail().getLevel();
                    ((LivingEntity) anEntity).setMaxHealth(20 * 43 * level);
                    ((LivingEntity) anEntity).setHealth(20 * 43 * level);
                }
                return null;
            }
        });

        List<UnbindInstruction<WBossDetail>> unbindInstructions = graveDigger.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                int baseLevel = controllable.getDetail().getLevel();
                List<ItemStack> itemStacks = new ArrayList<>();
                for (int i = baseLevel * ChanceUtil.getRandom(2); i > 0; --i) {
                    itemStacks.add(new ItemStack(BlockID.TNT, ChanceUtil.getRandom(16)));
                }
                for (int i = baseLevel * ChanceUtil.getRandom(4); i > 0; --i) {
                    itemStacks.add(new ItemStack(ItemID.DIAMOND, ChanceUtil.getRandom(32)));
                }
                for (int i = baseLevel * ChanceUtil.getRandom(4); i > 0; --i) {
                    itemStacks.add(new ItemStack(ItemID.EMERALD, ChanceUtil.getRandom(32)));
                }
                for (int i = baseLevel * ChanceUtil.getRandom(8); i > 0; --i) {
                    itemStacks.add(new ItemStack(ItemID.INK_SACK, ChanceUtil.getRandom(64), (short) 4));
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

        List<DamageInstruction<WBossDetail>> damageInstructions = graveDigger.damageInstructions;
        damageInstructions.add(new WDamageModifier());
        damageInstructions.add(new DamageInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, DamageInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity)) return null;
                LivingEntity toHit = (LivingEntity) eToHit;

                Location target = toHit.getLocation();
                makeSphere(target, 3, 3, 3);
                for (int i = 0; i < controllable.getDetail().getLevel(); ++i) {
                    target.getWorld().spawn(target, TNTPrimed.class);
                }
                return null;
            }
        });

        List<DamagedInstruction<WBossDetail>> damagedInstructions = graveDigger.damagedInstructions;
        damagedInstructions.add(new DamagedInstruction<WBossDetail>() {
            @Override
            public InstructionResult<WBossDetail, DamagedInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, DamageSource damageSource, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                LocalEntity localToHit = damageSource.getDamagingEntity();
                if (localToHit == null) return null;
                Entity toHit = BukkitUtil.getBukkitEntity(localToHit);
                if (getEvent(damage).getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
                    Location target = toHit.getLocation();
                    makeSphere(target, 3, 3, 3);
                    for (int i = 0; i < 4 * controllable.getDetail().getLevel(); ++i) {
                        target.getWorld().spawn(target, TNTPrimed.class);
                    }
                    // target.getWorld().spawn(target, Pig.class).setCustomName("Help Me!");
                }
                return null;
            }
        });
    }

    private static double lengthSq(double x, double y, double z) {
        return (x * x) + (y * y) + (z * z);
    }

    public void makeSphere(Location pos, double radiusX, double radiusY, double radiusZ) {

        BaseBlock block = new BaseBlock(1);

        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double nextXn = 0;
        forX: for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY: for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * invRadiusZ;

                    double distanceSq = lengthSq(xn, yn, zn);
                    if (distanceSq > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break;
                    }

                    if (lengthSq(nextXn, yn, zn) <= 1 && lengthSq(xn, nextYn, zn) <= 1 && lengthSq(xn, yn, nextZn) <= 1) {
                        continue;
                    }

                    setBlock(pos.clone().add(x, y, z), block);
                    setBlock(pos.clone().add(-x, y, z), block);
                    setBlock(pos.clone().add(x, -y, z), block);
                    setBlock(pos.clone().add(x, y, -z), block);
                    setBlock(pos.clone().add(-x, -y, z), block);
                    setBlock(pos.clone().add(x, -y, -z), block);
                    setBlock(pos.clone().add(-x, y, -z), block);
                    setBlock(pos.clone().add(-x, -y, -z), block);
                }
            }
        }
    }

    private WorldGuardPlugin WG = null;

    private void setBlock(Location l, BaseBlock b) {
        if (WG == null) WG = WGBukkit.getPlugin();
        if (WG.getRegionManager(l.getWorld()).getApplicableRegions(l).size() > 0) return;
        Block blk = l.getBlock();
        if (blk.getType() != Material.AIR) return;
        blk.setTypeIdAndData(b.getType(), (byte) b.getData(), true);
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
    }
}
