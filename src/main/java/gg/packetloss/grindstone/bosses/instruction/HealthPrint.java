/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.instruction;

import gg.packetloss.grindstone.world.type.city.combat.PvMComponent;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.EntityDetail;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.instruction.DamagedInstruction;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.util.AttackDamage;
import gg.packetloss.openboss.util.DamageSource;
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
