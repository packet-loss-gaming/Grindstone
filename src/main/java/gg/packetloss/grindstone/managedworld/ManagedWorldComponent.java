package gg.packetloss.grindstone.managedworld;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.World;

@ComponentInformation(friendlyName = "Managed World", desc = "Managed world lookup.")
public class ManagedWorldComponent extends BukkitComponent {
    private static final String CITY_WORLD = "City";
    private static final String PRIMARY_RANGE_WORLD = "Halzeil";
    private static final String SKY_WORLD = "Astaria";
    private static final String WILDERNESS_WORLD = "Wilderness";
    private static final String WILDERNESS_NETHER_WORLD = WILDERNESS_WORLD + "_nether";

    @Override
    public void enable() { }

    public boolean is(ManagedWorldIsQuery query, World world) {
        String worldName = world.getName();
        switch (query) {
            case CITY:
                return worldName.equals(CITY_WORLD);
            case LATEST_BUILD:
            case ANY_BUILD:
                return worldName.equals(PRIMARY_RANGE_WORLD);
            case SKY:
                return worldName.equals(SKY_WORLD);
            case WILDERNESS:
                return worldName.equals(WILDERNESS_WORLD);
            case WILDERNESS_NETHER:
                return worldName.equals(WILDERNESS_NETHER_WORLD);
            case ANY_WILDERNESS:
                return is(ManagedWorldIsQuery.WILDERNESS, world) || is(ManagedWorldIsQuery.WILDERNESS_NETHER, world);
        }

        throw new UnsupportedOperationException();
    }

    public World get(ManagedWorldGetQuery query) {
        switch (query) {
            case CITY:
                return CommandBook.server().getWorld(CITY_WORLD);
            case LATEST_BUILD:
                return CommandBook.server().getWorld(PRIMARY_RANGE_WORLD);
            case SKY:
                return CommandBook.server().getWorld(SKY_WORLD);
            case WILDERNESS:
                return CommandBook.server().getWorld(WILDERNESS_WORLD);
            case WILDERNESS_NETHER:
                return CommandBook.server().getWorld(WILDERNESS_NETHER_WORLD);
        }

        throw new UnsupportedOperationException();
    }
}
