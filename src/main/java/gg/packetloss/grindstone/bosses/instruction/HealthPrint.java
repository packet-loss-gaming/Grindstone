/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.instruction;

import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.EntityDetail;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.instruction.DamagedInstruction;
import com.skelril.OSBL.instruction.InstructionResult;
import com.skelril.OSBL.util.AttackDamage;
import com.skelril.OSBL.util.DamageSource;
import gg.packetloss.grindstone.world.type.city.combat.PvMComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class HealthPrint<T extends EntityDetail> extends DamagedInstruction<T> {
    @Override
    public InstructionResult<T, DamagedInstruction<T>> process(LocalControllable<T> controllable, DamageSource damageSource, AttackDamage damage) {
        if (!damageSource.involvesEntity()) return null;
        Entity boss = BukkitUtil.getBukkitEntity(controllable);
        Entity attacker = BukkitUtil.getBukkitEntity(damageSource.getDamagingEntity());
        if (boss instanceof LivingEntity && attacker instanceof Player) {
            PvMComponent.printHealth((Player) attacker, (LivingEntity) boss);
        }
        return null;
    }
}
