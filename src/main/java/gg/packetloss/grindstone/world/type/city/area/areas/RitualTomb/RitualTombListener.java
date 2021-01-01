/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.RitualTomb;

import gg.packetloss.grindstone.events.PlayerSacrificeRewardEvent;
import gg.packetloss.grindstone.events.playerstate.PlayerStatePushEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaListener;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.io.IOException;

public class RitualTombListener extends AreaListener<RitualTomb> {
    public RitualTombListener(RitualTomb parent) {
        super(parent);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            parent.teleportToRitualSite(player);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();

        if (parent.contains(to) && !event.getCause().equals(PlayerTeleportEvent.TeleportCause.UNKNOWN)) {
            event.setTo(parent.getRitualSiteLoc().clone());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerSacrificeReward(PlayerSacrificeRewardEvent event) {
        Player player = event.getPlayer();
        if (parent.contains(player)) {
            event.setCancelled(true);
            parent.increaseRitualValue(player, event.getValue());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTargetEntityEvent(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() == null) {
            return;
        }

        if (parent.contains(event.getEntity()) && !parent.contains(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageByEntityEvent event) {
        Entity defender = event.getEntity();
        if (!parent.contains(defender)) {
            return;
        }

        Entity attacker = event.getDamager();
        if (attacker instanceof Vex && defender instanceof Player) {
            int playerHealth = (int) Math.floor(((Player) defender).getHealth());

            int damage = ChanceUtil.getRandomNTimes(Math.min(playerHealth - 1, parent.getRitualLevel()), 3);
            if (damage < 1 && !parent.areDemonsLethal()) {
                return;
            }

            EntityUtil.forceDamage(defender, damage);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!parent.contains(entity)) {
            return;
        }

        Player killer = entity.getKiller();
        if (killer != null && entity instanceof Vex) {
            parent.demonKilled(killer);
        }
    }

    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!block.getLocation().equals(parent.ritualFireLoc)) {
            return;
        }

        Player player = event.getPlayer();
        parent.tryTeleportToRitualTomb(player);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!parent.contains(player)) {
            return;
        }

        try {
            parent.playerState.pushState(PlayerStateKind.RITUAL_TOMB, player);
            event.getDrops().clear();
            event.setDroppedExp(0);
        } catch (ConflictingPlayerStateException | IOException e) {
            e.printStackTrace();
        }

        String deathMessage;
        switch (ChanceUtil.getRandom(2)) {
            case 1:
                deathMessage = " was vexed";
                break;
            default:
                deathMessage = " died for Hallow";
                break;
        }

        event.setDeathMessage(player.getName() + deathMessage);
    }

    @EventHandler
    public void onPlayerStatePush(PlayerStatePushEvent event) {
        if (event.getKind() != PlayerStateKind.RITUAL_TOMB_SPECTATOR) {
            return;
        }

        Player player = event.getPlayer();
        parent.teleportToRitualTomb(player, true);
    }
}
