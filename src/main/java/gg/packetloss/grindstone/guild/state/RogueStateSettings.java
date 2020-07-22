/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.setting.BooleanGuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;

import java.util.ArrayList;
import java.util.List;

public class RogueStateSettings extends BaseStateSettings {
    private boolean blipWhileSneaking = false;
    private boolean showBerserkerBuffs = false;

    public boolean shouldBlipWhileSneaking() {
        return blipWhileSneaking;
    }

    public boolean shouldShowBerserkerBuffs() {
        return showBerserkerBuffs;
    }

    // Wrappers
    private transient BooleanGuildSetting blipWhileSneakingWrapper = new BooleanGuildSetting(
            "Blip While Sneaking", () -> blipWhileSneaking
    );

    private transient BooleanGuildSetting showBerserkerBuffsWrapper = new BooleanGuildSetting(
            "Show Berserker Buffs", () -> showBerserkerBuffs
    );

    @Override
    public List<GuildSetting> getAll() {
        List<GuildSetting> baseSetting = new ArrayList<>();

        baseSetting.addAll(super.getAll());
        baseSetting.addAll(List.of(
                blipWhileSneakingWrapper,
                showBerserkerBuffsWrapper
        ));

        return baseSetting;
    }

    @Override
    public boolean updateSetting(GuildSettingUpdate setting) {
        if (super.updateSetting(setting)) {
            return true;
        }

        if (setting.getSetting().getKey().equals(blipWhileSneakingWrapper.getKey())) {
            blipWhileSneaking = Boolean.parseBoolean(setting.getNewValue());
            return true;
        } else if (setting.getSetting().getKey().equals(showBerserkerBuffsWrapper.getKey())) {
            showBerserkerBuffs = Boolean.parseBoolean(setting.getNewValue());
            return true;
        }

        return false;
    }
}
