/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.bosses;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.entity.BukkitEntity;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.BoundInstruction;
import com.skelril.OSBL.instruction.Instruction;
import com.skelril.aurora.util.ChanceUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

public class ThunderZombie {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration thunderZombie;

    public ThunderZombie() {
        thunderZombie = new BukkitBossDeclaration(inst);
        setupThunderZombie();
    }

    public void bind(Damageable entity) {
        thunderZombie.bind(new BukkitBoss(entity));
    }

    private void setupThunderZombie() {
        List<Instruction> bindInstructions = thunderZombie.bindInstructions;
        bindInstructions.add(new BoundInstruction() {
            @Override
            public Instruction execute(LocalEntity entity, Object... objects) {
                Entity anEntity = BukkitUtil.getBukkitEntity(entity);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Thor Zombie");
                }
                return null;
            }

            @Override
            public Instruction execute() {
                return null;
            }
        });

        List<Instruction> unbindInstructions = thunderZombie.unbindInstructions;
        unbindInstructions.add(new BoundInstruction() {
            @Override
            public Instruction execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                Location target = boss.getLocation();
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                boss.getWorld().createExplosion(x, y, z, 4F, false, false);
                return null;
            }

            @Override
            public Instruction execute() {
                return null;
            }
        });
        unbindInstructions.add(new BoundInstruction() {
            @Override
            public Instruction execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                Location target = boss.getLocation();
                for (int i = 0; i < ChanceUtil.getRangedRandom(12, 150); i++) {
                    target.getWorld().dropItem(target, new ItemStack(ItemID.GOLD_BAR));
                }
                return null;
            }

            @Override
            public Instruction execute() {
                return null;
            }
        });

        List<Instruction> damageInstructions = thunderZombie.damageInstructions;
        damageInstructions.add(new BoundInstruction() {
            @Override
            public Instruction execute(LocalEntity entity, Object... objects) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);

                if (objects != null && objects.length > 0) {
                    if (objects[0] instanceof BukkitEntity) {
                        final Entity toHit = BukkitUtil.getBukkitEntity(objects[0]);
                        toHit.setVelocity(boss.getLocation().getDirection().multiply(2));

                        server.getScheduler().runTaskLater(inst, new Runnable() {
                            @Override
                            public void run() {
                                final Location targetLocation = toHit.getLocation();
                                server.getScheduler().runTaskLater(inst, new Runnable() {
                                    @Override
                                    public void run() {
                                        targetLocation.getWorld().strikeLightning(targetLocation);
                                    }
                                }, 15);
                            }
                        }, 30);
                    }
                }
                return null;
            }

            @Override
            public Instruction execute() {
                return null;
            }
        });
    }
}
