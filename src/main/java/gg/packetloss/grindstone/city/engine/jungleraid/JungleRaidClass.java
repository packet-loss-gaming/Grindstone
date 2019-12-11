package gg.packetloss.grindstone.city.engine.jungleraid;

public enum JungleRaidClass {
    BALANCED(128),
    ARCHER(128),
    MELEE(0),
    LUMBERJACK(0, false, true, false, 0),
    SNIPER(32, false, true, true, 0),
    ENGINEER(0, true, true, true, 348);

    private int arrowAmt;
    private int tntAmt;

    private boolean flintAndSteel;
    private boolean shears;
    private boolean axe;

    JungleRaidClass(int arrowAmt) {
        this(arrowAmt, true, true, true, 96);
    }

    JungleRaidClass(int arrowAmt, boolean flintAndSteel, boolean shears, boolean axe, int tntAmt) {
        this.arrowAmt = arrowAmt;
        this.flintAndSteel = flintAndSteel;
        this.shears = shears;
        this.axe = axe;
        this.tntAmt = tntAmt;
    }

    public int getArrowAmount() {
        return arrowAmt;
    }

    public boolean hasFlintAndSteel() {
        return flintAndSteel;
    }

    public boolean hasShears() {
        return shears;
    }

    public boolean hasAxe() {
        return axe;
    }

    public int getTNTAmount() {
        return tntAmt;
    }
}