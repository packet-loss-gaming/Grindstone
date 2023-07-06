/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.combat;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

@ComponentInformation(friendlyName = "PvM", desc = "Skelril PvM management.")
@Depend(components = {SessionComponent.class})
public class PvMComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private static SessionComponent sessions;
    @InjectComponent
    private PrayerComponent prayers;

    @Override
    public void enable() {
        CommandBook.registerEvents(this);
    }

    public static void printHealth(Player player, LivingEntity target) {
        final int oldCurrent = (int) Math.ceil(target.getHealth());

        Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
            int current = (int) Math.ceil(EntityUtil.getHealth(player, target));

            if (oldCurrent == current) return;

            PvMSession session = sessions.getSession(PvMSession.class, player);

            int max = (int) Math.ceil(EntityUtil.getMaxHealth(player, target));

            String message;

            if (current > 0) {
                message = ChatColor.DARK_AQUA
                        + String.valueOf(session.checkLast(target.getUniqueId()) ? ChatColor.ITALIC : "")
                        + "Entity Health: " + current + " / " + max;
            } else {
                message = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "KO!";
            }

            if (target.getCustomName() != null) {
                message += " (" + target.getCustomName() + ")";
            }

            ChatUtil.sendNotice(player, message);
        }, 1);
    }
}