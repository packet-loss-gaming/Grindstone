/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.passive;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.prayer.PassivePrayerEffect;
import gg.packetloss.grindstone.util.ArrowUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;

public class DeadlyDefenseEffect implements PassivePrayerEffect {
    private Arrow shootArrow(Player owner, LivingEntity target) {
        Arrow arrowEnt = ArrowUtil.shootArrow(owner, (LivingEntity) target, 1.6F, 0F);
        if (arrowEnt == null) {
            return null;
        }

        arrowEnt.setMetadata(
            "guild-exp-modifier",
            new FixedMetadataValue(CommandBook.inst(), 0D)
        );
        return arrowEnt;
    }

    private List<Arrow> shootArrowsFor(Player player) {
        List<Arrow> arrowEnts = new ArrayList<>();
        for (LivingEntity entity : player.getLocation().getNearbyEntitiesByType(Mob.class, 8, 3, 8)) {
            if (!EntityUtil.isHostileMob(entity)) {
                continue;
            }

            if (!player.hasLineOfSight(entity)) {
                continue;
            }

            Arrow arrowEnt = shootArrow(player, entity);
            if (arrowEnt == null) {
                continue;
            }

            arrowEnts.add(arrowEnt);
            if (arrowEnts.size() >= 10) {
                break;
            }
        }
        return arrowEnts;
    }

    private void registerArrowCleanup(List<Arrow> arrowEnts) {
        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            for (Arrow arrowEnt : arrowEnts) {
                arrowEnt.remove();
            }
        }, 20 * 3);
    }

    @Override
    public void trigger(Player player) {
        registerArrowCleanup(shootArrowsFor(player));
    }

    @Override
    public void strip(Player player) { }
}
