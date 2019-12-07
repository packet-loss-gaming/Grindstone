package gg.packetloss.grindstone.sacrifice;

enum SacrificeCommonality {
    JUNK(0),
    NORMAL(0),
    RARE_1(.8),
    RARE_2(1.2),
    RARE_3(2),
    RARE_4(2.75),
    RARE_5(3),
    RARE_6(5),
    UBER_RARE(10000);

    private final double additionalChance;

    private SacrificeCommonality(double additionalChance) {
        this.additionalChance = additionalChance;
    }

    public double getAdditionalChance() {
        return additionalChance;
    }
}
