/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prayer.effect.interactive;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.prayer.InteractTriggeredPrayerEffect;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ThrownFireballEffect implements InteractTriggeredPrayerEffect {
    private final Set<UUID> cooldowns = new HashSet<>();

    @Override
    public void trigger(PlayerInteractEvent event, Player player) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) {
            return;
        }

        UUID playerID = player.getUniqueId();
        if (cooldowns.contains(playerID)) {
            return;
        }

        Location loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(2))
            .toLocation(player.getWorld(), player.getLocation().getYaw(), player.getLocation().getPitch());
        player.getWorld().spawn(loc, Fireball.class);

        cooldowns.add(playerID);
        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            cooldowns.remove(playerID);
        }, 15);
    }
}
