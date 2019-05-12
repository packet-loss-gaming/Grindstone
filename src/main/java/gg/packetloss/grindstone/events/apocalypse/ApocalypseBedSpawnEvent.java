/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.apocalypse;

import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ApocalypseBedSpawnEvent extends ApocalypsePlayerEvent {
    private int numberOfZombies;

    public ApocalypseBedSpawnEvent(Player player, Location spawnLocation, int numberOfZombies) {
        super(player, spawnLocation);
        this.numberOfZombies = numberOfZombies;
    }

    public int getNumberOfZombies() {
        return numberOfZombies;
    }

    public void setNumberOfZombies(int numberOfZombies) {
        Validate.isTrue(numberOfZombies > 0);
        this.numberOfZombies = numberOfZombies;
    }
}
