package gg.packetloss.grindstone.buff;

import org.apache.commons.lang3.Validate;

public class BuffSet {
    private BuffCategory category;
    private int[] levels;

    public BuffSet(BuffCategory category) {
        this.category = category;
        this.levels = new int[category.getNumBuffs()];
    }

    public int getLevel(Buff buff) {
        Validate.isTrue(buff.getCategory() == category);

        return levels[buff.categoryOrdinal()];
    }

    public boolean adjustLevel(Buff buff, int amt) {
        Validate.isTrue(buff.getCategory() == category);

        int current = getLevel(buff);
        int proposed = current + amt;

        if (proposed < 0 || proposed > buff.getMaxLevel()) {
            return false;
        }

        levels[buff.categoryOrdinal()] = proposed;

        return true;
    }

    public boolean increase(Buff buff) {
        return adjustLevel(buff, 1);
    }

    public boolean decrease(Buff buff) {
        return adjustLevel(buff, -1);
    }
}
