/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;

import java.util.ArrayList;
import java.util.List;

public interface StateSettings {
    public boolean shouldPrintExpVerbose();

    public List<GuildSetting> getAll();
    default public List<GuildSetting> getAllSorted() {
        List<GuildSetting> settings = new ArrayList<>(getAll());
        settings.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return settings;
    }

    public boolean updateSetting(GuildSettingUpdate setting);
}
