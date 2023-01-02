/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.instruction;

import gg.packetloss.grindstone.util.dropttable.BoundDropSpawner;
import gg.packetloss.grindstone.util.dropttable.OSBLKillInfo;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.EntityDetail;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.instruction.UnbindInstruction;
import org.bukkit.entity.LivingEntity;

public class PerformanceDropTableUnbind<T extends EntityDetail> extends UnbindInstruction<T> {
    private final PerformanceDropTable dropTable;

    public PerformanceDropTableUnbind(PerformanceDropTable dropTable) {
        this.dropTable = dropTable;
    }

    @Override
    public InstructionResult<T, UnbindInstruction<T>> process(LocalControllable<T> controllable) {
        LivingEntity boss = (LivingEntity) BukkitUtil.getBukkitEntity(controllable);
        new BoundDropSpawner(boss::getLocation).provide(dropTable, new OSBLKillInfo(controllable));
        return null;
    }
}
