/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.events.graveyard.PlayerCreateGraveEvent;
import gg.packetloss.grindstone.events.graveyard.PlayerDisturbGraveEvent;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PhantomPotionImpl extends AbstractItemFeatureImpl {
    private Set<UUID> affectedPlayers = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathDropRedirectEvent(PlayerCreateGraveEvent event) {
        Player player = event.getPlayer();
        getSession(player).addGraveLocation(event.getGraveLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItem();

        if (ItemUtil.isItem(stack, CustomItems.PHANTOM_POTION)) {
            Optional<Location> optLastLoc = getSession(player).getRecentGrave();
            if (optLastLoc.isEmpty()) {
                ChatUtil.sendError(player, "The phantoms find no recent grave bearing your name.");
                event.setCancelled(true);
                return;
            }

            // Protect the player for 30 seconds
            UUID playerID = player.getUniqueId();
            affectedPlayers.add(playerID);
            server.getScheduler().runTaskLater(
                    inst,
                    () -> affectedPlayers.remove(playerID),
                    20 * 30
            );

            Location lastLoc = optLastLoc.get();
            if (!player.teleport(lastLoc)) {
                ChatUtil.sendError(player, "Location Information: X: "
                                + lastLoc.getBlockX() + ", Y: "
                                + lastLoc.getBlockY() + ", Z: "
                                + lastLoc.getBlockZ() + " in "
                                + lastLoc.getWorld().getName() + '.'
                );
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDisturbGraveEvent(PlayerDisturbGraveEvent event) {
        Player player = event.getPlayer();
        UUID playerID = player.getUniqueId();

        if (affectedPlayers.contains(playerID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTargetPlayer(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() instanceof Player) {
            Player player = (Player) event.getTarget();
            UUID playerID = player.getUniqueId();

            // If the player attacks something, remove the AI protection
            if (event.getReason().equals(EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER)) {
                affectedPlayers.remove(playerID);
                return;
            }

            if (affectedPlayers.contains(playerID)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionApply(EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UUID playerID = player.getUniqueId();

            PotionEffect newEffect = event.getNewEffect();
            if (newEffect != null && newEffect.getType().equals(PotionEffectType.BLINDNESS)) {
                if (affectedPlayers.contains(playerID)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
