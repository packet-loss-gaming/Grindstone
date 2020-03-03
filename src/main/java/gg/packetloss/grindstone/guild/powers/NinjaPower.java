package gg.packetloss.grindstone.guild.powers;

public enum NinjaPower implements GuildPower {
    EXTRA_HEALTH(3),
    GRACEFUL_FALLING(3),
    PITFALL_SNEAK(5),
    FIREPROOF(7),
    MULTI_ARROW_BOMBS(12),
    POTION_ARROW_BOMBS(16),
    VAMPIRIC_SMOKE_BOMB(18),
    HEALING_ARROWS(21),
    IGNITION_SPECIAL(25),
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
