package gg.packetloss.grindstone.world.type.city.skywars;

import gg.packetloss.grindstone.util.flag.BooleanFlag;

public enum SkyWarsFlag implements BooleanFlag {
    REGEN_ENABLED,
    CHICKEN_PLUS_PLUS,
    FLAMMABLE,
    DOOM,
    ACID_PLUS_PLUS,
    SIXTY_FOUR_CLICK;

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