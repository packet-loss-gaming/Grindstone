/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.manager.apocalypse.instruction;

import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.EntityDetail;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.instruction.InstructionResult;
import com.skelril.OSBL.instruction.UnbindInstruction;
import gg.packetloss.grindstone.apocalypse.ApocalypseHelper;
import gg.packetloss.grindstone.util.dropttable.BoundDropSpawner;
import gg.packetloss.grindstone.util.dropttable.DropTable;
import gg.packetloss.grindstone.util.dropttable.OSBLKillInfo;
import gg.packetloss.grindstone.util.dropttable.PerformanceKillInfo;
import org.bukkit.entity.LivingEntity;

public class ApocalypseDropTableInstruction<T extends EntityDetail> extends UnbindInstruction<T> {
    private DropTable<PerformanceKillInfo> dropTable;

    public ApocalypseDropTableInstruction(DropTable<PerformanceKillInfo> dropTable) {
        this.dropTable = dropTable;
    }

    @Override
    public InstructionResult<T, UnbindInstruction<T>> process(LocalControllable<T> controllable) {
        if (ApocalypseHelper.areDropsSuppressed()) {
            return null;
        }

        LivingEntity boss = (LivingEntity) BukkitUtil.getBukkitEntity(controllable);
        new BoundDropSpawner(boss::getLocation).provide(dropTable, new OSBLKillInfo(controllable));

        return null;
    }
}
