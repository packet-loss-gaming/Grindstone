package gg.packetloss.grindstone.city.engine.skywars;

public enum SkyWarsFlag {
    REGEN_ENABLED,
    CHICKEN_PLUS_PLUS;

    private final boolean enabledByDefault;

    private SkyWarsFlag() {
        this(false);
    }

    private SkyWarsFlag(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }
}