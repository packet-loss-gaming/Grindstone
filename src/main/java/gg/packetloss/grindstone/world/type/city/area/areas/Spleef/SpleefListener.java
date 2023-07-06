/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Spleef;

import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SpleefListener implements Listener {
    private SpleefArea parent;

    public SpleefListener(SpleefArea parent) {
        this.parent = parent;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!parent.anyContains(event.getBlock().getLocation())) {
            return;
        }

        event.setDropItems(false);

        Player player = event.getPlayer();
        if (!parent.isUsingArenaTools(player)) {
            return;
        }

        ItemStack heldTool = player.getInventory().getItemInMainHand();
        int maxDurability = heldTool.getType().getMaxDurability();
        if (maxDurability > 0 && heldTool.getDurability() > maxDurability * .8) {
            heldTool.setDurability((short) 0);
            player.getInventory().setItemInMainHand(heldTool);
        }

        // Update info for the anti-camping system
        UUID playerId = player.getUniqueId();
        parent.lastBlockBreak.put(playerId, System.currentTimeMillis());
        parent.warnedPlayers.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.SUFFOCATION) {
            return;
        }

        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        if (parent.anyContains(player.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!parent.isUsingArenaTools(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (!parent.isUsingArenaTools(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockChangePreLog(BlockChangePreLogEvent event) {
        if (parent.anyContains(event.getLocation())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuildEnable(GuildPowersEnableEvent event) {
        if (parent.isUsingArenaTools(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
