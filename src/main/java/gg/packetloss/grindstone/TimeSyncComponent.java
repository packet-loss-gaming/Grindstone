/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldGetQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldIsQuery;
import gg.packetloss.grindstone.world.managed.ManagedWorldMassQuery;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Time Sync", desc = "Synchronizes time across worlds.")
@Depend(components = {ManagedWorldComponent.class})
public class TimeSyncComponent extends BukkitComponent {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private ManagedWorldComponent managedWorld;

    @Override
    public void enable() {
        server.getScheduler().scheduleSyncRepeatingTask(inst, this::syncTime, 0, 20 * 60);
    }

    private void syncTime() {
        long leaderTime = managedWorld.get(ManagedWorldGetQuery.CITY).getTime();

        for (World world : managedWorld.getAll(ManagedWorldMassQuery.ENVIRONMENTALLY_CONTROLLED)) {
            if (managedWorld.is(ManagedWorldIsQuery.CITY, world)) {
                continue;
            }

            world.setTime(leaderTime);
        }
    }
}
