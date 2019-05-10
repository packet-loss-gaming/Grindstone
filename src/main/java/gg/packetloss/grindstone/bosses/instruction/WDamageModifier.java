/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.instruction;

import com.skelril.OSBL.bukkit.util.BukkitAttackDamage;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.DamageInstruction;
import com.skelril.OSBL.instruction.InstructionResult;
import com.skelril.OSBL.util.AttackDamage;
import gg.packetloss.grindstone.bosses.detail.WBossDetail;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.event.entity.EntityDamageEvent;

public class WDamageModifier extends DamageInstruction<WBossDetail> {
    @Override
    public InstructionResult<WBossDetail, DamageInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, LocalEntity entity, AttackDamage damage) {
        EntityDamageEvent event = getEvent(damage);
        if (event == null) return null;
        double origDmg = event.getOriginalDamage(EntityDamageEvent.DamageModifier.BASE);
        int level = controllable.getDetail().getLevel();
        int addDmg = ChanceUtil.getRandom(ChanceUtil.getRandom(level)) - 1;
        event.setDamage(EntityDamageEvent.DamageModifier.BASE, origDmg + addDmg);
        return null;
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
    }
}
