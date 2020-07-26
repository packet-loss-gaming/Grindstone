/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.instruction;

import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.EntityDetail;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.instruction.UnbindInstruction;
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
        ExplosionStateFactory.createExplosion(target, getExplosionStrength(controllable.getDetail()), fire, blockBreak);
        return null;
    }
}
