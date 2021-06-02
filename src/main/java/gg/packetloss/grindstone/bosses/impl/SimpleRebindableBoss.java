/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.impl;

import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.openboss.bukkit.BukkitBossDeclaration;
import gg.packetloss.openboss.bukkit.entity.BukkitBoss;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.InstructionDispatch;
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
