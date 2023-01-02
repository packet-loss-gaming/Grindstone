/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.instruction;

import gg.packetloss.grindstone.world.type.range.worldlevel.RangeWorldMinibossDetail;
import gg.packetloss.grindstone.world.type.range.worldlevel.WorldLevelComponent;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.DamageInstruction;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.util.AttackDamage;

public class RangeWorldMinibossBasicDamageInstruction<T extends RangeWorldMinibossDetail> extends DamageInstruction<T> {
    @Override
    public InstructionResult<T, DamageInstruction<T>> process(LocalControllable<T> controllable, LocalEntity entity, AttackDamage damage) {
        int level = controllable.getDetail().getLevel();
        damage.setDamage(WorldLevelComponent.scaleDamageForLevel(damage.getDamage(), level));
        return null;
    }
}
