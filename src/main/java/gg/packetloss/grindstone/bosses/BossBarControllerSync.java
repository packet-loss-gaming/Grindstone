/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.BossBarProvider;
import gg.packetloss.grindstone.util.BossBarUtil;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.EntityDetail;
import gg.packetloss.openboss.entity.LocalControllable;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class BossBarControllerSync<T extends BossBarProvider & EntityDetail> {
    private final Map<UUID, LocalControllable<T>> controlled;

    private BukkitTask bossBarPlayerSyncTask;
    private BukkitTask bossBarProgressSyncTask;

    public BossBarControllerSync(Map<UUID, LocalControllable<T>> controlled) {
        this.controlled = controlled;
    }

    public void startWatching() {
        Validate.isTrue(bossBarPlayerSyncTask == null && bossBarProgressSyncTask == null);

        bossBarPlayerSyncTask = Bukkit.getScheduler().runTaskTimer(
            CommandBook.inst(),
            () -> {
                controlled.values().forEach(this::updateBossBarPlayers);
            },
            20 * 10,
            20 * 2
        );
        bossBarProgressSyncTask = Bukkit.getScheduler().runTaskTimer(
            CommandBook.inst(),
            () -> {
                controlled.values().forEach(this::updateBossBarProgress);
            },
            20 * 10,
            5
        );
    }

    public void stopWatching() {
        bossBarPlayerSyncTask.cancel();
        bossBarPlayerSyncTask = null;

        bossBarProgressSyncTask.cancel();
        bossBarProgressSyncTask = null;
    }

    private void updateBossBarPlayers(LocalControllable<T> controllable) {
        Entity entity = BukkitUtil.getBukkitEntity(controllable.getLocalEntity());
        BossBar bossBar = controllable.getDetail().getBossBar();
        Collection<Player> players = entity.getLocation().getNearbyPlayers(25);
        BossBarUtil.syncWithPlayers(bossBar, players);
    }

    private void updateBossBarProgress(LocalControllable<T> controllable) {
        LivingEntity entity = (LivingEntity) BukkitUtil.getBukkitEntity(controllable.getLocalEntity());
        BossBar bossBar = controllable.getDetail().getBossBar();
        bossBar.setProgress(entity.getHealth() / entity.getMaxHealth());
    }
}
