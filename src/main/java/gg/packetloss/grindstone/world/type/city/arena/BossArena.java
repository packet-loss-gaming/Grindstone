/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.arena;

import org.bukkit.entity.LivingEntity;


public interface BossArena extends MonitoredArena {

    boolean isBossSpawned();

    void spawnBoss();

    LivingEntity getBoss();
}
