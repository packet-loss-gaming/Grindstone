package gg.packetloss.grindstone.buff;

public enum Buff {
    APOCALYPSE_OVERLORD("Overlord", BuffCategory.APOCALYPSE, 5),
    APOCALYPSE_MAGIC_SHIELD("Magic Shield", BuffCategory.APOCALYPSE),
    APOCALYPSE_DAMAGE_BOOST("Damage Boost", BuffCategory.APOCALYPSE),
    APOCALYPSE_LIFE_LEACH("Life Leach", BuffCategory.APOCALYPSE);

    private final String friendlyName;
    private final BuffCategory buffCategory;
    private final int maxLevel;
    private int categoryOrdinal = 0;

    private Buff(String friendlyName, BuffCategory buffCategory, int maxLevel) {
        this.friendlyName = friendlyName;
        this.buffCategory = buffCategory;
        this.maxLevel = maxLevel;
    }

    private Buff(String friendlyName, BuffCategory buffCategory) {
        this(friendlyName, buffCategory, Integer.MAX_VALUE);
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public BuffCategory getCategory() {
        return buffCategory;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int categoryOrdinal() {
        return categoryOrdinal;
    }

    static {
        int[] counters = new int[BuffCategory.values().length];

        for (Buff buff : values()) {
            buff.categoryOrdinal = counters[buff.buffCategory.ordinal()]++;
        }
    }
}
