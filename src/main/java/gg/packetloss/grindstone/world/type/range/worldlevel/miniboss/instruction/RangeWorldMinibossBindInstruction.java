/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.instruction;

import gg.packetloss.grindstone.util.RomanNumeralUtil;
import gg.packetloss.grindstone.world.type.range.worldlevel.RangeWorldMinibossDetail;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.instruction.BindInstruction;
import gg.packetloss.openboss.instruction.InstructionResult;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.function.Supplier;

public class RangeWorldMinibossBindInstruction<T extends RangeWorldMinibossDetail> extends BindInstruction<T> {
    private final String boundName;
    private final Supplier<Double> healthPerLevelSupplier;

    public RangeWorldMinibossBindInstruction(String boundName, Supplier<Double> healthPerLevelSupplier) {
        this.boundName = boundName;
        this.healthPerLevelSupplier = healthPerLevelSupplier;
    }

    @Override
    public InstructionResult<T, BindInstruction<T>> process(LocalControllable<T> controllable) {
        Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
        RangeWorldMinibossDetail detail = controllable.getDetail();
        int level = detail.getLevel();

        // Set name
        anEntity.setCustomName(boundName + " - " + RomanNumeralUtil.toRoman(level));

        // Set health
        double healthPerLevel = healthPerLevelSupplier.get();
        ((LivingEntity) anEntity).setMaxHealth(level * healthPerLevel);
        ((LivingEntity) anEntity).setHealth(level * healthPerLevel);

        return null;
    }
}
