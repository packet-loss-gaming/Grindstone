/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;

@ComponentInformation(friendlyName = "Lost Custom Items", desc = "Lost item commands.")
public class LostItemsComponent extends BukkitComponent {
    @Override
    public void enable() {
        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            CustomItemsConverter.register(registrar);

            registrar.register(LostItemCommandsRegistration.builder(), new LostItemCommands());
        });
    }
}
