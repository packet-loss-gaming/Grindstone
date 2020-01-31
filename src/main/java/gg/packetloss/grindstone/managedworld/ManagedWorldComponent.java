package gg.packetloss.grindstone.managedworld;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

@ComponentInformation(friendlyName = "Managed World", desc = "Managed world lookup.")
public class ManagedWorldComponent extends BukkitComponent {
    private static final String CITY_WORLD = "City";
    private static final String PRIMARY_RANGE_WORLD = "Halzeil";
    private static final String PRIMARY_RANGE_WORLD_NETHER = PRIMARY_RANGE_WORLD + "_nether";
    private static final String SKY_WORLD = "Astaria";

    @Override
    public void enable() { }

    public boolean is(ManagedWorldIsQuery query, World world) {
        String worldName = world.getName();
        switch (query) {
            case CITY:
                return worldName.equals(CITY_WORLD);
            case LATEST_RANGE:
                return worldName.equals(PRIMARY_RANGE_WORLD);
            case LATEST_RANGE_NETHER:
                return worldName.equals(PRIMARY_RANGE_WORLD_NETHER);
            case ANY_RANGE:
                return is(ManagedWorldIsQuery.LATEST_RANGE, world) ||
                       is(ManagedWorldIsQuery.LATEST_RANGE_NETHER, world);
            case SKY:
                return worldName.equals(SKY_WORLD);
            case ANY_BUIDABLE:
                return is(ManagedWorldIsQuery.ANY_RANGE, world) || is(ManagedWorldIsQuery.SKY, world);
            case ANY_ENVIRONMENTALLY_CONTROLLED:
                return getAll(ManagedWorldMassQuery.ENVIRONMENTALLY_CONTROLLED).contains(world);
        }

        throw new UnsupportedOperationException();
    }

    public World get(ManagedWorldGetQuery query) {
        switch (query) {
            case CITY:
                return CommandBook.server().getWorld(CITY_WORLD);
            case LATEST_RANGE:
                return CommandBook.server().getWorld(PRIMARY_RANGE_WORLD);
            case LATEST_RANGE_NETHER:
                return CommandBook.server().getWorld(PRIMARY_RANGE_WORLD_NETHER);
            case SKY:
                return CommandBook.server().getWorld(SKY_WORLD);
        }

        throw new UnsupportedOperationException();
    }

    public List<World> getAll(ManagedWorldMassQuery query) {
        switch (query) {
            case RANGE_OVERWORLDS:
                return List.of(get(ManagedWorldGetQuery.LATEST_RANGE));
            case ENVIRONMENTALLY_CONTROLLED: {
                List<World> worlds = new ArrayList<>();

                worlds.add(get(ManagedWorldGetQuery.CITY));
                worlds.addAll(getAll(ManagedWorldMassQuery.RANGE_OVERWORLDS));
                worlds.add(get(ManagedWorldGetQuery.SKY));

                return worlds;
            }
        }

        throw new UnsupportedOperationException();
    }
}
