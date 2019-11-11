package gg.packetloss.grindstone.buff;

public enum  BuffCategory {
    APOCALYPSE("Apocalypse");

    private final String friendlyName;
    private int buffsUsingOrdinal;

    private BuffCategory(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public int getNumBuffs() {
        return buffsUsingOrdinal;
    }

    static {
        int[] counters = new int[BuffCategory.values().length];

        for (Buff buff : Buff.values()) {
            BuffCategory.values()[buff.getCategory().ordinal()].buffsUsingOrdinal++;
        }
    }
}
