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
