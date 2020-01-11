package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.setting.BooleanGuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;

import java.util.List;

public class RogueStateSettings implements StateSettings {
    private boolean blipWhileSneaking = true;

    public boolean shouldBlipWhileSneaking() {
        return blipWhileSneaking;
    }

    // Wrappers
    private transient BooleanGuildSetting blipWhileSneakingWrapper = new BooleanGuildSetting(
            "Blip While Sneaking", () -> blipWhileSneaking
    );

    @Override
    public List<GuildSetting> getAll() {
        return List.of(
                blipWhileSneakingWrapper
        );
    }

    @Override
    public boolean updateSetting(GuildSettingUpdate setting) {
        if (setting.getSetting().getKey().equals(blipWhileSneakingWrapper.getKey())) {
            blipWhileSneaking = Boolean.parseBoolean(setting.getNewValue());
            return true;
        }

        return false;
    }
}
