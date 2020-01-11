package gg.packetloss.grindstone.guild.state;

import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;

import java.util.List;

public interface StateSettings {
    public List<GuildSetting> getAll();
    public boolean updateSetting(GuildSettingUpdate setting);
}
