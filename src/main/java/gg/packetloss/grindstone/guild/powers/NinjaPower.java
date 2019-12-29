package gg.packetloss.grindstone.guild.powers;

public enum NinjaPower implements GuildPower {
    EXTRA_HEALTH(3),
    MASTER_CLIMBER(30);

    private final int level;

    private NinjaPower(int level) {
        this.level = level;
    }

    @Override
    public int getUnlockLevel() {
        return level;
    }
}
