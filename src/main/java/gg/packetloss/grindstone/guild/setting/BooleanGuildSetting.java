/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.setting;

import gg.packetloss.bukkittext.Text;
import org.bukkit.ChatColor;

import java.util.function.Supplier;

public class BooleanGuildSetting implements GuildSetting {
    private String name;
    private Supplier<Boolean> valueSupplier;

    public BooleanGuildSetting(String name, Supplier<Boolean> valueSupplier) {
        this.name = name;
        this.valueSupplier = valueSupplier;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Text getValueAsText() {
        if (valueSupplier.get()) {
            return Text.of(ChatColor.DARK_GREEN, "ENABLED");
        } else {
            return Text.of(ChatColor.RED, "DISABLED");
        }
    }

    @Override
    public String getDefaultUpdateString() {
        return getKey() + "=" + !valueSupplier.get();
    }

    @Override
    public boolean isValidValue(String value) {
        value = value.toLowerCase();
        return value.equals("true") || value.equals("false");
    }
}
