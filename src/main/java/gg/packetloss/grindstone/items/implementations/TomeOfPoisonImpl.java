/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TomeOfPoisonImpl extends AbstractItemFeatureImpl {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityPotionEffectEvent event) {
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED &&
            event.getAction() != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("aurora.tome.poison")) {
            return;
        }

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null || !newEffect.getType().equals(PotionEffectType.POISON)) {
            return;
        }

        if (ChanceUtil.getChance(5)) {
            Bukkit.getScheduler().runTask(CommandBook.inst(), () -> {
                if (player.getPotionEffect(PotionEffectType.REGENERATION) == null) {
                    player.addPotionEffect(new PotionEffect(
                        PotionEffectType.REGENERATION,
                        Math.min(20 * 60 * 2, newEffect.getDuration()),
                        Math.min(1, newEffect.getAmplifier())
                    ));
                }
                player.removePotionEffect(PotionEffectType.POISON);
            });
        }
    }
}
