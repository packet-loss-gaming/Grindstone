package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.setting.BooleanGuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;

import java.util.List;

public class RogueStateSettings implements StateSettings {
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
        return List.of(
                blipWhileSneakingWrapper,
                showBerserkerBuffsWrapper
        );
    }

    @Override
    public boolean updateSetting(GuildSettingUpdate setting) {
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
