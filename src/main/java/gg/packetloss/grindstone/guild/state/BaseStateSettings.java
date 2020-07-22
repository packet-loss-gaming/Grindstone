/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.setting.BooleanGuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;

import java.util.List;

public abstract class BaseStateSettings implements StateSettings {
    private boolean verboseExp = false;

    @Override
    public boolean shouldPrintExpVerbose() { return verboseExp; }

    // Wrappers
    private transient BooleanGuildSetting verboseExpWrapper = new BooleanGuildSetting(
            "Verbose Experience", () -> verboseExp
    );

    @Override
    public List<GuildSetting> getAll() {
        return List.of(
                verboseExpWrapper
        );
    }

    @Override
    public boolean updateSetting(GuildSettingUpdate setting) {
        if (setting.getSetting().getKey().equals(verboseExpWrapper.getKey())) {
            verboseExp = Boolean.parseBoolean(setting.getNewValue());
            return true;
        }

        return false;
    }
}
