/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild;

import gg.packetloss.grindstone.guild.powers.GuildPower;
import gg.packetloss.grindstone.guild.powers.NinjaPower;
import gg.packetloss.grindstone.guild.powers.RoguePower;

public enum GuildType {
    NINJA,
    ROGUE;

    public GuildPower[] getPowers() {
        switch (this) {
            case NINJA:
                return NinjaPower.values();
            case ROGUE:
                return RoguePower.values();
        }

        throw new UnsupportedOperationException();
    }
}
