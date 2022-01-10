/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.timetravel;

import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;

@CommandContainer
public class TimeTravelCommands {
    private TimeTravelComponent component;

    public TimeTravelCommands(TimeTravelComponent component) {
        this.component = component;
    }

    @Command(name = "timetravel", desc = "Change the current time travel focus")
    public void timeTravel(Player player, @Arg(desc = "version") ManagedWorldTimeContext version) {
        component.setOverride(player, version);
        ChatUtil.sendNotice(player, "Portal time period set.");
    }
}
