/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.setting;

import gg.packetloss.bukkittext.Text;

@Deprecated
public class DummyGuildSetting implements GuildSetting {
    private String name;

    public DummyGuildSetting(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Text getValueAsText() {
        return null;
    }

    @Override
    public String getDefaultUpdateString() {
        return null;
    }

    @Override
    public boolean isValidValue(String value) {
        return false;
    }
}
