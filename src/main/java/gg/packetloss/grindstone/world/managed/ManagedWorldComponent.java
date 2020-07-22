/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.managed;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ComponentInformation(friendlyName = "Managed World", desc = "Managed world lookup.")
public class ManagedWorldComponent extends BukkitComponent {
    private static final String CITY_WORLD = "City";
    private static final String SKY_WORLD = "Astaria";

    private static final RangeWorldList RANGE_WORLD_LIST = new RangeWorldList();

    @Override
    public void enable() { }

    private Optional<World> getWorld(String worldName) {
        World world = CommandBook.server().getWorld(worldName);
        if (world == null) {
            CommandBook.logger().warning(worldName + " not found.");
            return Optional.empty();
        }

        return Optional.of(world);
    }

    public boolean is(ManagedWorldIsQuery query, World world) {
        String worldName = world.getName();
        switch (query) {
            case CITY:
                return worldName.equals(CITY_WORLD);
            case RANGE_OVERWORLD:
                return RANGE_WORLD_LIST.isRangedOverworld(worldName);
            case RANGE_NETHER:
                return RANGE_WORLD_LIST.isRangedNether(worldName);
            case ANY_RANGE:
                return RANGE_WORLD_LIST.isRangedWorld(worldName);
            case SKY:
                return worldName.equals(SKY_WORLD);
            case ANY_BUIDABLE:
                return is(ManagedWorldIsQuery.ANY_RANGE, world) || is(ManagedWorldIsQuery.SKY, world);
            case ANY_ENVIRONMENTALLY_CONTROLLED:
                return getAll(ManagedWorldMassQuery.ENVIRONMENTALLY_CONTROLLED).contains(world);
        }

        throw new UnsupportedOperationException();
    }

    public World get(ManagedWorldGetQuery query, ManagedWorldTimeContext timeContext) {
        switch (query) {
            case CITY:
                return getWorld(CITY_WORLD).orElseThrow();
            case RANGE_OVERWORLD:
                return getWorld(RANGE_WORLD_LIST.getOverworld(timeContext)).orElseThrow();
            case RANGE_NETHER:
                return getWorld(RANGE_WORLD_LIST.getNether(timeContext)).orElseThrow();
            case SKY:
                return getWorld(SKY_WORLD).orElseThrow();
        }

        throw new UnsupportedOperationException();
    }

    public World get(ManagedWorldGetQuery query) {
        return get(query, ManagedWorldTimeContext.LATEST);
    }

    public List<World> getAll(ManagedWorldMassQuery query) {
        switch (query) {
            case RANGE_OVERWORLDS: {
                List<World> worlds = new ArrayList<>();

                for (String worldName : RANGE_WORLD_LIST.getOverworlds()) {
                    getWorld(worldName).ifPresent(worlds::add);
                }
                
                return worlds;
            }
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

    public ManagedWorldTimeContext getTimeContextFor(World world) {
        if (is(ManagedWorldIsQuery.ANY_RANGE, world)) {
            return RANGE_WORLD_LIST.getTimeContext(world.getName());
        }

        return ManagedWorldTimeContext.LATEST;
    }
}
