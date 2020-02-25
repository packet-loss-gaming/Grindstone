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

import java.util.function.Function;

public class ApocalypseDropTableInstruction<T extends EntityDetail> extends UnbindInstruction<T> {
    private DropTable<PerformanceKillInfo> dropTable;
    private Function<LocalControllable<T>, PerformanceKillInfo> infoCreator;

    public ApocalypseDropTableInstruction(DropTable<PerformanceKillInfo> dropTable) {
        this(dropTable, OSBLKillInfo::new);
    }

    public ApocalypseDropTableInstruction(DropTable<PerformanceKillInfo> dropTable,
                                          Function<LocalControllable<T>, PerformanceKillInfo> infoCreator) {
        this.dropTable = dropTable;
        this.infoCreator = infoCreator;
    }

    @Override
    public InstructionResult<T, UnbindInstruction<T>> process(LocalControllable<T> controllable) {
        if (ApocalypseHelper.areDropsSuppressed()) {
            return null;
        }

        LivingEntity boss = (LivingEntity) BukkitUtil.getBukkitEntity(controllable);
        new BoundDropSpawner(boss::getLocation).provide(dropTable, infoCreator.apply(controllable));

        return null;
    }
}
