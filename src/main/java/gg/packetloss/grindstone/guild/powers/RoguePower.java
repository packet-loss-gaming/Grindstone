package gg.packetloss.grindstone.guild.powers;

public enum RoguePower implements GuildPower {
    SUPER_SPEED(30);

    private final int level;

    private RoguePower(int level) {
        this.level = level;
    }

    @Override
    public int getUnlockLevel() {
        return level;
    }
}
