package gg.packetloss.grindstone.state;

public class PlayerExperience {
    private final float exp;
    private final int level;

    public PlayerExperience(float exp, int level) {
        this.exp = exp;
        this.level = level;
    }

    public float getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }
}
