package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;

import java.util.List;

public class NinjaStateSettings implements StateSettings {
    @Override
    public List<GuildSetting> getAll() {
        return List.of();
    }

    @Override
    public boolean updateSetting(GuildSettingUpdate setting) {
        return false;
    }
}
