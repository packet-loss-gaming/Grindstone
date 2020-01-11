package gg.packetloss.grindstone.guild.setting;

import gg.packetloss.bukkittext.Text;

@Deprecated
public class DummyGuildSetting implements GuildSetting {
    private String name;

    public DummyGuildSetting(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Text getValueAsText() {
        return null;
    }

    @Override
    public String getDefaultUpdateString() {
        return null;
    }

    @Override
    public boolean isValidValue(String value) {
        return false;
    }
}
