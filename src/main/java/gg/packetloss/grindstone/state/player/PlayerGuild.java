/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player;

import gg.packetloss.grindstone.guild.GuildType;

public class PlayerGuild {
    private GuildType guildType;
    private boolean powersEnabled;

    public PlayerGuild(GuildType guildType, boolean powersEnabled) {
        this.guildType = guildType;
        this.powersEnabled = powersEnabled;
    }

    public GuildType getGuildType() {
        return guildType;
    }

    public boolean arePowersEnabled() {
        return powersEnabled;
    }
}
