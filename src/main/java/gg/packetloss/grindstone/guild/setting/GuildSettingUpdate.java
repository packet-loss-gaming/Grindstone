/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.setting;

public class GuildSettingUpdate {
    private final GuildSetting setting;
    private final String newValue;

    public GuildSettingUpdate(GuildSetting setting, String newValue) {
        this.setting = setting;
        this.newValue = newValue;
    }

    public GuildSetting getSetting() {
        return setting;
    }

    public String getNewValue() {
        return newValue;
    }
}
