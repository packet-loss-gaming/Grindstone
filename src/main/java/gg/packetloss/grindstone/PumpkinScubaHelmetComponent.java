/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

@ComponentInformation(friendlyName = "Pumpkin Scuba", desc = "Breath underwater.")
public class PumpkinScubaHelmetComponent extends BukkitComponent implements Listener {
    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {

        Entity e = event.getEntity();
        if (!(e instanceof Player player) || !event.getCause().equals(EntityDamageEvent.DamageCause.DROWNING)) return;

        if (player.getInventory().getHelmet() != null && player.getInventory().getHelmet().getType() == Material.PUMPKIN) {
            player.setRemainingAir(player.getMaximumAir());
            event.setCancelled(true);
        }
    }
}
