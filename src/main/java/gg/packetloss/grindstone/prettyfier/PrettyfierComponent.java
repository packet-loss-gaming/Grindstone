/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.prettyfier;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;

import java.util.ArrayList;
import java.util.List;

@ComponentInformation(friendlyName = "Prettyfier", desc = "Keeping things neat and orderly")
public class PrettyfierComponent extends BukkitComponent {
    private List<Prettyfier> prettyfiers = new ArrayList<>();

    @Override
    public void enable() {
        AutoCloser autoCloser = new AutoCloser();
        CommandBook.registerEvents(autoCloser);
        prettyfiers.add(autoCloser);
    }

    @Override
    public void disable() {
        prettyfiers.forEach(Prettyfier::forceFinish);
    }
}
