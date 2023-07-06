/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.listener;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.sk89q.commandbook.CommandBook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class BossBuggedRespawnListener implements Listener {
    private final String bossName;

    private final Predicate<LivingEntity> shouldRespawn;
    private final Consumer<LivingEntity> respawnHook;

    public BossBuggedRespawnListener(String bossName, Predicate<LivingEntity> shouldRespawn,
                                     Consumer<LivingEntity> respawnHook) {
        this.bossName = bossName;
        this.shouldRespawn = shouldRespawn;
        this.respawnHook = respawnHook;
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        LivingEntity living = (LivingEntity) entity;
        if (shouldRespawn.test(living)) {
            // Delay to avoid possible concurrent modification during entity processing.
            Bukkit.getScheduler().runTask(CommandBook.inst(), () -> {
                CommandBook.logger().info("Respawning " + bossName + " due to bugged despawn");
                respawnHook.accept(living);
            });
        }
    }
}
