/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.SandArena;

import gg.packetloss.grindstone.events.apocalypse.GemOfLifeUsageEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.RefCountedTracker;
import gg.packetloss.grindstone.util.player.FallDamageDeathBlocker;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.IOException;
import java.util.UUID;

public class SandArenaListener extends AreaListener<SandArena> {
    public SandArenaListener(SandArena parent) {
        super(parent);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!parent.contains(event.getBlock())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {

        if (parent.contains(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private RefCountedTracker<UUID> protectedPlayers = new RefCountedTracker<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity injuredEntity = event.getEntity();
        if (!(injuredEntity instanceof Player)) {
            return;
        }

        Player defender = (Player) injuredEntity;
        boolean isFallDamage = event.getCause() == DamageCause.FALL;
        if (isFallDamage && !parent.contains(defender) && protectedPlayers.contains(defender.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (parent.contains(defender)) {
            FallDamageDeathBlocker.protectPlayer(defender, protectedPlayers);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (parent.contains(player, 1)) {
            try {
                parent.playerState.pushState(PlayerStateKind.SAND_ARENA, player);
                event.getDrops().clear();
                event.setDroppedExp(0);
            } catch (ConflictingPlayerStateException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    // FIXME: Priority set as workaround for Multiverse-Core#1977
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (parent.playerState.hasValidStoredState(PlayerStateKind.SAND_ARENA, event.getPlayer())) {
            event.setRespawnLocation(parent.getRespawnLocation());
        }
    }
}
