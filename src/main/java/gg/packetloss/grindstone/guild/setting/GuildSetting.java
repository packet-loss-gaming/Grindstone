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
