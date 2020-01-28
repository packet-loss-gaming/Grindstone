package gg.packetloss.grindstone.bosses.impl;

import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.InstructionDispatch;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class SimpleRebindableBoss<T extends Damageable> extends BukkitBossDeclaration<GenericDetail> {
    private String boundName;
    private Class<T> clazz;

    public SimpleRebindableBoss(String boundName, Class<T> clazz, Plugin declarer, InstructionDispatch<GenericDetail> dispatch) {
        super(declarer, dispatch);
        this.clazz = clazz;
        this.boundName = boundName;
    }

    @Override
    public boolean matchesBind(LocalEntity entity) {
        Entity boss = BukkitUtil.getBukkitEntity(entity);
        return clazz.isInstance(boss) && EntityUtil.nameMatches(boss, boundName);
    }

    @Override
    public LocalControllable<GenericDetail> tryRebind(LocalEntity entity) {
        var boss = new BukkitBoss<>(clazz.cast(BukkitUtil.getBukkitEntity(entity)), new GenericDetail());
        silentBind(boss);
        return boss;
    }
}
