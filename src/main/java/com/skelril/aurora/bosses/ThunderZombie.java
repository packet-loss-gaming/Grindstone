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
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.EntityUtil;
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
        thunderZombie = new BukkitBossDeclaration(inst, new SimpleInstructionDispatch()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Thor Zombie");
            }
        };
        setupThunderZombie();
    }

    public void bind(Damageable entity) {
        thunderZombie.bind(new BukkitBoss(entity));
    }

    private void setupThunderZombie() {
        List<BindInstruction> bindInstructions = thunderZombie.bindInstructions;
        bindInstructions.add(new BindInstruction() {
            @Override
            public InstructionResult<BindInstruction> process(LocalControllable controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof LivingEntity) {
                    ((LivingEntity) anEntity).setCustomName("Thor Zombie");
                }
                return null;
            }
        });

        List<UnbindInstruction> unbindInstructions = thunderZombie.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction() {
            @Override
            public InstructionResult<UnbindInstruction> process(LocalControllable controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                double x = target.getX();
                double y = target.getY();
                double z = target.getZ();
                boss.getWorld().createExplosion(x, y, z, 4F, false, false);
                return null;
            }
        });
        unbindInstructions.add(new UnbindInstruction() {
            @Override
            public InstructionResult<UnbindInstruction> process(LocalControllable controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();
                for (int i = 0; i < ChanceUtil.getRangedRandom(12, 150); i++) {
                    target.getWorld().dropItem(target, new ItemStack(ItemID.GOLD_BAR));
                }
                return null;
            }
        });

        List<DamageInstruction> damageInstructions = thunderZombie.damageInstructions;
        damageInstructions.add(new DamageInstruction() {
            @Override
            public InstructionResult<DamageInstruction> process(LocalControllable controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                final Entity toHit = BukkitUtil.getBukkitEntity(entity);
                toHit.setVelocity(boss.getLocation().getDirection().multiply(2));

                server.getScheduler().runTaskLater(inst, () -> {
                    final Location targetLocation = toHit.getLocation();
                    server.getScheduler().runTaskLater(inst, () -> {
                        targetLocation.getWorld().strikeLightning(targetLocation);
                    }, 15);
                }, 30);
                return null;
            }
        });
    }
}
