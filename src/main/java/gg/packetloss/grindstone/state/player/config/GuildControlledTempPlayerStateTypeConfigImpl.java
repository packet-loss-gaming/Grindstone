/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player.config;

import gg.packetloss.grindstone.state.player.PlayerStateAttribute;

import java.util.List;

public class GuildControlledTempPlayerStateTypeConfigImpl extends TempPlayerStateTypeConfigImpl {
    @Override
    public List<PlayerStateAttribute> getAttributes() {
        return List.of(PlayerStateAttribute.GUILD);
    }
}
