/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.PlayerDeathDropRedirectEvent;
import gg.packetloss.grindstone.events.graveyard.PlayerDisturbGraveEvent;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityTargetUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PhantomPotionImpl extends AbstractItemFeatureImpl {
    private final Set<UUID> affectedPlayers = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeathDropRedirectEvent(PlayerDeathDropRedirectEvent event) {
        Player player = event.getPlayer();
        getSession(player).addDeathDropLocation(event.getDropLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItem();

        if (ItemUtil.isItem(stack, CustomItems.PHANTOM_POTION) ||
            ItemUtil.isItem(stack, CustomItems.NEWBIE_PHANTOM_POTION)) {
            Location lastLoc = getSession(player).getRecentDeathDropPoint();
            if (lastLoc != null) {
                // Protect the player for 30 seconds
                UUID playerID = player.getUniqueId();
                affectedPlayers.add(playerID);
                Bukkit.getScheduler().runTaskLater(
                        CommandBook.inst(),
                        () -> affectedPlayers.remove(playerID),
                        20 * 30
                );

                if (!player.teleport(lastLoc)) {
                    ChatUtil.sendError(player, "Location Information: X: "
                                    + lastLoc.getBlockX() + ", Y: "
                                    + lastLoc.getBlockY() + ", Z: "
                                    + lastLoc.getBlockZ() + " in "
                                    + lastLoc.getWorld().getName() + '.'
                    );
                }
                ChatUtil.sendNotice(player, "You have 30 seconds. To stay hidden don't attack anything.");
            } else {
                ChatUtil.sendError(player, "No drop locations are known to the potion.");
                event.setCancelled(true);
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
        if (event.getTarget() instanceof Player player) {
            UUID playerID = player.getUniqueId();

            // If the player attacks something, remove the AI protection
            if (EntityTargetUtil.isRetaliatoryReason(event.getReason())) {
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
        if (event.getEntity() instanceof Player player) {
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
