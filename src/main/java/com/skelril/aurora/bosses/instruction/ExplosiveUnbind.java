/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.bosses.instruction;

import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.EntityDetail;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.instruction.InstructionResult;
import com.skelril.OSBL.instruction.UnbindInstruction;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public abstract class ExplosiveUnbind<T extends EntityDetail> extends UnbindInstruction<T> {

    private final boolean blockBreak;
    private final boolean fire;

    protected ExplosiveUnbind(boolean blockBreak, boolean fire) {
        this.blockBreak = blockBreak;
        this.fire = fire;
    }

    public abstract float getExplosionStrength(T t);

    @Override
    public InstructionResult<T, UnbindInstruction<T>> process(LocalControllable<T> controllable) {
        Entity boss = BukkitUtil.getBukkitEntity(controllable);
        Location target = boss.getLocation();
        double x = target.getX();
        double y = target.getY();
        double z = target.getZ();
        boss.getWorld().createExplosion(x, y, z, getExplosionStrength(controllable.getDetail()), fire, blockBreak);
        return null;
    }
}
