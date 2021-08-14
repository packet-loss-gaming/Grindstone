/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.PluginTaskExecutor;

@ComponentInformation(friendlyName = "Plugin Task Executor", desc = "Orderly background job shutdowns.")
public class PluginTaskExecutorComponent extends BukkitComponent {
    @Override
    public void enable() { }

    @Override
    public void disable() {
        try {
            PluginTaskExecutor.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
