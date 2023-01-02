/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.bosses.BossBarControllerSync;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.RomanNumeralUtil;
import gg.packetloss.openboss.bukkit.BukkitBossDeclaration;
import gg.packetloss.openboss.bukkit.entity.BukkitBoss;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.SimpleInstructionDispatch;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

public class RangeWorldMinibossDeclaration<T extends Damageable> extends BukkitBossDeclaration<RangeWorldMinibossDetail> {
    private final WorldLevelComponent worldLevelComponent;
    private final String boundName;
    private final Class<T> clazz;

    public RangeWorldMinibossDeclaration(WorldLevelComponent worldLevelComponent, String boundName, Class<T> clazz) {
        super(CommandBook.inst(), new SimpleInstructionDispatch<>());
        this.worldLevelComponent = worldLevelComponent;
        this.clazz = clazz;
        this.boundName = boundName;

        new BossBarControllerSync<>(getControlled()).startWatching();
    }

    @Override
    public void silentBind(LocalControllable<RangeWorldMinibossDetail> controllable) {
        super.silentBind(controllable);

        worldLevelComponent.getMinibossTargetWatcher().addMiniBoss(controllable.getUUID(), controllable.getDetail());
    }

    @Override
    public void silentUnbind(LocalControllable<RangeWorldMinibossDetail> controllable) {
        worldLevelComponent.getMinibossTargetWatcher().removeMiniBoss(controllable.getUUID());
        controllable.getDetail().getBossBar().removeAll();

        super.silentUnbind(controllable);
    }

    private RangeWorldMinibossDetail createDetailForLevel(int level) {
        return new RangeWorldMinibossDetail(
            level,
            Bukkit.createBossBar(boundName, BarColor.RED, BarStyle.SEGMENTED_6)
        );
    }

    public void bind(T entity, int level) {
        bind(new BukkitBoss<>(entity, createDetailForLevel(level)));
    }

    @Override
    public boolean matchesBind(LocalEntity entity) {
        Entity boss = BukkitUtil.getBukkitEntity(entity);
        return clazz.isInstance(boss) && EntityUtil.nameStartsWith(boss, boundName);
    }

    @Override
    public LocalControllable<RangeWorldMinibossDetail> tryRebind(LocalEntity entity) {
        Entity bossEntity = BukkitUtil.getBukkitEntity(entity);

        String bossName = EntityUtil.getCustomName(bossEntity).get();
        String[] parts = bossName.split(" - ");
        int bossLevel = RomanNumeralUtil.fromRoman(parts[1]);

        var boss = new BukkitBoss<>(clazz.cast(BukkitUtil.getBukkitEntity(entity)), createDetailForLevel(bossLevel));
        silentBind(boss);
        return boss;
    }
}