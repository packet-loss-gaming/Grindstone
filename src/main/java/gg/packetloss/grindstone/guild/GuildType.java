package gg.packetloss.grindstone.guild;

import gg.packetloss.grindstone.guild.powers.GuildPower;
import gg.packetloss.grindstone.guild.powers.NinjaPower;
import gg.packetloss.grindstone.guild.powers.RoguePower;

public enum GuildType {
    NINJA,
    ROGUE;

    public GuildPower[] getPowers() {
        switch (this) {
            case NINJA:
                return NinjaPower.values();
            case ROGUE:
                return RoguePower.values();
        }

        throw new UnsupportedOperationException();
    }
}
