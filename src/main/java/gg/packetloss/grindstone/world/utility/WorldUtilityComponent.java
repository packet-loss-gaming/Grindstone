/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.utility;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.ProblemDetectionComponent;

@ComponentInformation(friendlyName = "World Utility", desc = "World Utility Systems")
@Depend(components = {ProblemDetectionComponent.class})
public class WorldUtilityComponent extends BukkitComponent {
    @InjectComponent
    private ProblemDetectionComponent problemDetection;

    @Override
    public void enable() {
        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            registrar.registerAsSubCommand("wutil", "World Utility Commands", (wutilRegistrar) -> {
                wutilRegistrar.register(
                    WorldUtilityCommandsRegistration.builder(),
                    new WorldUtilityCommands(problemDetection)
                );
            });
        });
    }
}
