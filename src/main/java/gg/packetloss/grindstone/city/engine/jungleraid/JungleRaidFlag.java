package gg.packetloss.grindstone.city.engine.jungleraid;

public enum JungleRaidFlag {
    TRAMPOLINE(0),
    SUPER(1),
    TITAN_MODE(2),
    RANDOM_ROCKETS(3),
    EXPLOSIVE_ARROWS(4),
    GRENADES(5),
    TORMENT_ARROWS(6),
    DEATH_TOUCH(7),
    END_OF_DAYS(8),
    POTION_PLUMMET(9),
    NO_FIRE_SPREAD(10),
    NO_MINING(11),
    NO_BLOCK_BREAK(12),
    NO_TIME_LIMIT(13),
    ALLOW_GUILDS(14),
    ENHANCED_COMPASS(15, true),
    DEATH_ROCKETS(16, true);


    public final int index;
    public final boolean enabledByDefault;

    JungleRaidFlag(int index) {
        this(index, false);
    }

    JungleRaidFlag(int index, boolean enabledByDefault) {
        this.index = index;
        this.enabledByDefault = enabledByDefault;
    }
}