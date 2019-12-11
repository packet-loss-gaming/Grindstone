package gg.packetloss.grindstone.city.engine.skywars;

import gg.packetloss.grindstone.util.flag.BooleanFlag;

public enum SkyWarsFlag implements BooleanFlag {
    REGEN_ENABLED,
    CHICKEN_PLUS_PLUS;

    private final boolean enabledByDefault;

    private SkyWarsFlag() {
        this(false);
    }

    private SkyWarsFlag(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    @Override
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }
}