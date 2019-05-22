/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.ic;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.mechanics.ic.ICManager;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.ic.ics.DelayedRepeater;
import gg.packetloss.grindstone.ic.ics.GroupSentryGun;
import gg.packetloss.grindstone.ic.ics.NinjaStarSpawner;
import org.bukkit.Server;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "IC Component", desc = "Custom ICs!")
@Depend(plugins = "CraftBook")
public class CustomICComponent extends BukkitComponent {

  private static final CommandBook INST = CommandBook.inst();
  private static final Logger LOG = INST.getLogger();
  private static final Server SERVER = CommandBook.server();

  private ICManager ICCore;

  @Override
  public void enable() {

    SERVER.getScheduler().runTaskLater(INST, () -> {
      ICCore = ICManager.inst();
      if (ICCore == null) {
        LOG.warning("ICManager is null!");
      }
      registerICs();
    }, 1);

  }

  @SuppressWarnings("AccessStaticViaInstance")
  private void registerICs() {

    ICCore.registerIC("SK1278", "group sentry", new GroupSentryGun.Factory(SERVER), ICCore.familySISO, ICCore.familyAISO);
    ICCore.registerIC("SK9001", "star spawner", new NinjaStarSpawner.Factory(SERVER), ICCore.familySISO, ICCore.familyAISO);
    ICCore.registerIC("SK9002", "delay repeater", new DelayedRepeater.Factory(SERVER), ICCore.familySISO, ICCore.familyAISO);
  }
}
