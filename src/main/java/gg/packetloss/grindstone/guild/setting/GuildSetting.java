/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.setting;

import gg.packetloss.bukkittext.Text;

public interface GuildSetting {
    public String getName();
    default public String getKey() {
        return getName().replaceAll(" ", "_").toUpperCase();
    }

    public Text getValueAsText();

    public String getDefaultUpdateString();

    public boolean isValidValue(String value);
}
