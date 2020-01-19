package gg.packetloss.grindstone.guild.powers;

public enum RoguePower implements GuildPower {
    DAMAGE_BUFF(3),
    PITFALL_LEAP(5),
    FALL_DAMAGE_REDIRECTION(6),
    POTION_METABOLIZATION(7),
    NIGHTMARE_SPECIAL(12),
    SNIPER_SNOWBALLS(17),
    SPEEDY_SPECIALS(25),
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
