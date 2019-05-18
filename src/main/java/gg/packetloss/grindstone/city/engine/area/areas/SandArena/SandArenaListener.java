/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.SandArena;

import gg.packetloss.grindstone.city.engine.area.AreaListener;
import gg.packetloss.grindstone.events.apocalypse.GemOfLifeUsageEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.Disarm;
import gg.packetloss.grindstone.util.RefCountedTracker;
import gg.packetloss.grindstone.util.player.FallDamageDeathBlocker;
import gg.packetloss.grindstone.util.player.PlayerState;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SandArenaListener extends AreaListener<SandArena> {
    public SandArenaListener(SandArena parent) {
        super(parent);
    }

    private static Set<Class> blacklistedSpecs = new HashSet<>();

    static {
        blacklistedSpecs.add(Disarm.class);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpecialAttack(SpecialAttackEvent event) {

        SpecialAttack attack = event.getSpec();

        if (!parent.contains(attack.getLocation())) return;

        if (blacklistedSpecs.contains(attack.getClass())) {

            event.setCancelled(true);
        }
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

        HashMap<String, PlayerState> playerState = parent.playerState;

        Player player = event.getEntity();
        if (!playerState.containsKey(player.getName()) && parent.contains(player, 1) && !parent.admin.isAdmin(player)) {

            playerState.put(player.getName(), new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getLevel(),
                    player.getExp()));
            event.getDrops().clear();
            event.setDroppedExp(0);
            event.setKeepLevel(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        HashMap<String, PlayerState> playerState = parent.playerState;

        Player player = event.getPlayer();
        final Location fallBack = event.getRespawnLocation();

        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !parent.admin.isAdmin(player)) {

            try {
                PlayerState identity = playerState.get(player.getName());

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                player.setLevel(identity.getLevel());
                player.setExp(identity.getExperience());

                event.setRespawnLocation(parent.getRespawnLocation());
            } catch (Exception e) {
                e.printStackTrace();
                event.setRespawnLocation(fallBack);
            } finally {
                playerState.remove(player.getName());
            }
        }
    }
}
