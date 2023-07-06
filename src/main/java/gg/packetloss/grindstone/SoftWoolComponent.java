/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.anticheat.FallBlockerEvent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;


@ComponentInformation(friendlyName = "Soft Wool", desc = "Fall softly my friends.")
public class SoftWoolComponent extends BukkitComponent implements Listener {
    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {

        Entity entity = event.getEntity();
        EntityDamageEvent.DamageCause damageCause = event.getCause();

        if (damageCause.equals(EntityDamageEvent.DamageCause.FALL)
                && LocationUtil.hasBelow(entity.getLocation(), EnvironmentUtil::isWool)) {
            if (entity instanceof Player) {
                FallBlockerEvent fEvent = new FallBlockerEvent((Player) entity);
                CommandBook.callEvent(fEvent);
                if (fEvent.isDisplayingMessage()) {
                    ChatUtil.sendNotice(entity, "The cloth negates your fall damage.");
                }
            }
            event.setCancelled(true);
        }
    }
}
