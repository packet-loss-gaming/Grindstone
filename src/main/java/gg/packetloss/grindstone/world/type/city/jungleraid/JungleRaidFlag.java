package gg.packetloss.grindstone.world.type.city.jungleraid;

import gg.packetloss.grindstone.util.flag.BooleanFlag;

public enum JungleRaidFlag implements BooleanFlag {
    TRAMPOLINE,
    SUPER,
    TITAN_MODE,
    NO_CHILL,
    EXPLOSIVE_ARROWS,
    GRENADES,
    TORMENT_ARROWS,
    DEATH_TOUCH,
    END_OF_DAYS,
    POTION_PLUMMET,
    NO_FIRE_SPREAD,
    NO_MINING,
    NO_BLOCK_BREAK,
    NO_TIME_LIMIT,
    ALLOW_GUILDS,
    RANDOM_ROCKETS,
    ENHANCED_COMPASS(true),
    DEATH_ROCKETS(true);

    private final boolean enabledByDefault;

    private JungleRaidFlag() {
        this(false);
    }

    private JungleRaidFlag(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    @Override
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }
}