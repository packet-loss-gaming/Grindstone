/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.combat;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "PvM", desc = "Skelril PvM management.")
@Depend(components = {SessionComponent.class})
public class PvMComponent extends BukkitComponent implements Listener {

  private final static CommandBook inst = CommandBook.inst();
  private final static Logger log = inst.getLogger();
  private final static Server server = CommandBook.server();

  @InjectComponent
  private static SessionComponent sessions;
  private static List<PvPScope> pvpLimitors = new ArrayList<>();
  private static WorldGuardPlugin WG;
  @InjectComponent
  private PrayerComponent prayers;

  public static void printHealth(Player player, LivingEntity target) {
    final int oldCurrent = (int) Math.ceil(target.getHealth());

    server.getScheduler().runTaskLater(inst, () -> {

      int current = (int) Math.ceil(target.getHealth());

      if (oldCurrent == current) {
        return;
      }

      PvMSession session = sessions.getSession(PvMSession.class, player);

      int max = (int) Math.ceil(target.getMaxHealth());

      String message;

      if (current > 0) {
        message = ChatColor.DARK_AQUA
            + String.valueOf(session.checkLast(target.getUniqueId()) ? ChatColor.ITALIC : "")
            + "Entity Health: " + current + " / " + max;
      } else {
        message = ChatColor.GOLD + String.valueOf(ChatColor.BOLD) + "KO!";
      }

      ChatUtil.sendNotice(player, message);
    }, 1);
  }

  @Override
  public void enable() {
    //noinspection AccessStaticViaInstance
    inst.registerEvents(this);
  }
}