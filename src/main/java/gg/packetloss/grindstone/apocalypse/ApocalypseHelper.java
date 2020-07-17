package gg.packetloss.grindstone.apocalypse;

import gg.packetloss.grindstone.bosses.manager.apocalypse.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;

public class ApocalypseHelper {
    private ApocalypseHelper() { }

    public static boolean checkEntity(Entity e) {
        if (e.getCustomName() == null) {
            return false;
        }

        if (!(e instanceof Zombie)) {
            return false;
        }

        String customName = e.getCustomName();
        if (customName.equals("Apocalyptic Zombie")) {
            return true;
        }

        if (customName.equals(ThorZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(ZapperZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(MercilessZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(StickyZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(ChuckerZombie.BOUND_NAME)) {
            return true;
        }

        return false;
    }

    private static boolean suppressDrops = false;

    public static boolean areDropsSuppressed() {
        return suppressDrops;
    }

    public static void suppressDrops(Runnable op) {
        boolean prevValue = suppressDrops;

        suppressDrops = true;
        try {
            op.run();
        } finally {
            suppressDrops = prevValue;
        }
    }
}
