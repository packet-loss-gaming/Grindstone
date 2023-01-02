/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import gg.packetloss.grindstone.util.EntityTargetUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RangeWorldMinibossTargetWatcher implements Listener {
    private final WorldLevelComponent worldLevelComponent;

    public RangeWorldMinibossTargetWatcher(WorldLevelComponent worldLevelComponent) {
        this.worldLevelComponent = worldLevelComponent;
    }

    private final Map<UUID, RangeWorldMinibossDetail> bossToDetail = new HashMap<>();

    public void addMiniBoss(UUID uuid, RangeWorldMinibossDetail detail) {
        bossToDetail.put(uuid, detail);
    }

    public void removeMiniBoss(UUID uuid) {
        bossToDetail.remove(uuid);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityTargetEntityEvent(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player targetPlayer)) {
            return;
        }

        if (EntityTargetUtil.isRetaliatoryReason(event.getReason())) {
            return;
        }

        Entity aggressorEntity = event.getEntity();
        RangeWorldMinibossDetail detail = bossToDetail.get(aggressorEntity.getUniqueId());
        if (detail == null) {
            return;
        }

        int bossWorldLevel = detail.getLevel();
        int targetWorldLevel = worldLevelComponent.getWorldLevel(targetPlayer);

        if (targetWorldLevel < bossWorldLevel) {
            event.setCancelled(true);
        }
    }
}
