package gg.packetloss.grindstone.util.bridge;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.DoubleFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;

public class ExtendedWGFlags {
    public static final DoubleFlag PRICE = register(new DoubleFlag("price"));

    private static <T extends Flag<?>> T register(final T flag) throws FlagConflictException {
        WorldGuard.getInstance().getFlagRegistry().register(flag);
        return flag;
    }
}
