/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.impl;

import gg.packetloss.grindstone.bosses.BossBarControllerSync;
import gg.packetloss.grindstone.bosses.detail.BossBarDetail;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.openboss.bukkit.BukkitBossDeclaration;
import gg.packetloss.openboss.bukkit.entity.BukkitBoss;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.InstructionDispatch;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

@Deprecated(since = "this class is not abstract and not specific enough, promotes bad composition via inheritance")
public class BossBarRebindableBoss<T extends Damageable> extends BukkitBossDeclaration<BossBarDetail> {
    private String boundName;
    private Class<T> clazz;

    public BossBarRebindableBoss(String boundName, Class<T> clazz, Plugin declarer, InstructionDispatch<BossBarDetail> dispatch) {
        super(declarer, dispatch);
        this.clazz = clazz;
        this.boundName = boundName;

        new BossBarControllerSync<>(getControlled()).startWatching();
    }

    @Override
    public void silentUnbind(LocalControllable<BossBarDetail> controllable) {
        super.silentUnbind(controllable);

        controllable.getDetail().getBossBar().removeAll();
    }

    private BossBarDetail createDefaultDetail() {
        return new BossBarDetail(Bukkit.createBossBar(boundName, BarColor.RED, BarStyle.SEGMENTED_6));
    }

    public void bind(T entity) {
        bind(new BukkitBoss<>(entity, createDefaultDetail()));
    }

    @Override
    public boolean matchesBind(LocalEntity entity) {
        Entity boss = BukkitUtil.getBukkitEntity(entity);
        return clazz.isInstance(boss) && EntityUtil.nameMatches(boss, boundName);
    }

    @Override
    public LocalControllable<BossBarDetail> tryRebind(LocalEntity entity) {
        var boss = new BukkitBoss<>(clazz.cast(BukkitUtil.getBukkitEntity(entity)), createDefaultDetail());
        silentBind(boss);
        return boss;
    }
}
