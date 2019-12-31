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
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.bosses.instruction.WDamageModifier;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
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

public class GraveDigger {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<WBossDetail> graveDigger;

    public GraveDigger() {
        graveDigger = new BukkitBossDeclaration<>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Grave Digger");
            }
        };
        setupGraveDigger();
    }

    public void bind(Damageable entity, WBossDetail detail) {
        graveDigger.bind(new BukkitBoss<>(entity, detail));
    }

    private void setupGraveDigger() {
        List<BindInstruction<WBossDetail>> bindInstructions = graveDigger.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, BindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    anEntity.setCustomName("Grave Digger");
                    int level = controllable.getDetail().getLevel();
                    ((LivingEntity) anEntity).setMaxHealth(20 * 43 * level);
                    ((LivingEntity) anEntity).setHealth(20 * 43 * level);

                    try {
                        AttributeBook.setAttribute((LivingEntity) anEntity, AttributeBook.Attribute.FOLLOW_RANGE, 150);
                    } catch (UnsupportedFeatureException ex) {
                        ex.printStackTrace();
                    }
                }
                return null;
            }
        });

        List<UnbindInstruction<WBossDetail>> unbindInstructions = graveDigger.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                int baseLevel = controllable.getDetail().getLevel();
                List<ItemStack> itemStacks = new ArrayList<>();

                for (int i = baseLevel * ChanceUtil.getRandom(2); i > 0; --i) {
                    itemStacks.add(new ItemStack(Material.TNT, ChanceUtil.getRandom(16)));
                }
                for (int i = baseLevel * ChanceUtil.getRandom(4); i > 0; --i) {
                    itemStacks.add(new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(32)));
                }
                for (int i = baseLevel * ChanceUtil.getRandom(4); i > 0; --i) {
                    itemStacks.add(new ItemStack(Material.EMERALD, ChanceUtil.getRandom(32)));
                }
                for (int i = baseLevel * ChanceUtil.getRandom(8); i > 0; --i) {
                    itemStacks.add(new ItemStack(Material.LAPIS_LAZULI, ChanceUtil.getRandom(64)));
                }

                if (ChanceUtil.getChance(Math.max(3, 20 - baseLevel))) {
                    itemStacks.add(CustomItemCenter.build(CustomItems.NINJA_OATH));
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

        List<DamageInstruction<WBossDetail>> damageInstructions = graveDigger.damageInstructions;
        damageInstructions.add(new WDamageModifier());
        damageInstructions.add(new DamageInstruction<>() {
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
        damagedInstructions.add(new HealthPrint<>());
        damagedInstructions.add(new DamagedInstruction<>() {
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

                    setBlock(pos.clone().add(x, y, z), Material.STONE);
                    setBlock(pos.clone().add(-x, y, z), Material.STONE);
                    setBlock(pos.clone().add(x, -y, z), Material.STONE);
                    setBlock(pos.clone().add(x, y, -z), Material.STONE);
                    setBlock(pos.clone().add(-x, -y, z), Material.STONE);
                    setBlock(pos.clone().add(x, -y, -z), Material.STONE);
                    setBlock(pos.clone().add(-x, y, -z), Material.STONE);
                    setBlock(pos.clone().add(-x, -y, -z), Material.STONE);
                }
            }
        }
    }

    private void setBlock(Location l, Material type) {
        if (WorldGuardBridge.hasRegionsAt(l)) return;

        Block blk = l.getBlock();
        if (blk.getType() != Material.AIR) return;
        blk.setType(type, true);
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
    }
}
