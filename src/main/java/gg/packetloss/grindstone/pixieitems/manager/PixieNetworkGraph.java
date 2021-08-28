/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.manager;

import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class PixieNetworkGraph {
    private final Set<Location> sources = new HashSet<>();

    private final Map<String, List<Location>> itemToDestination = new HashMap<>();
    private final Map<Location, Set<String>> destinationToItem = new HashMap<>();
    private final Map<Location, Map<String, List<Integer>>> destinationPlacementFilters = new HashMap<>();
    private final List<Location> anyItemDestinations = new ArrayList<>();

    public void addSource(Location location) {
        sources.add(location);
    }

    public void removeSource(Location location) {
        sources.remove(location);
    }

    public Set<Location> getSources() {
        return sources.stream().map(Location::clone).collect(Collectors.toSet());
    }

    public void addSink(Map<String, List<Integer>> itemMapping, Location location) {
        if (itemMapping.isEmpty()) {
            anyItemDestinations.add(location);
        } else {
            boolean hasLocalFilters = false;

            for (Map.Entry<String, List<Integer>> itemMappingEntry : itemMapping.entrySet()) {
                itemToDestination.compute(itemMappingEntry.getKey(), (ignored, values) -> {
                    if (values == null) {
                        values = new ArrayList<>();
                    }
                    values.add(location);
                    return values;
                });

                if (!itemMappingEntry.getValue().isEmpty()) {
                    hasLocalFilters = true;
                }
            }

            if (hasLocalFilters) {
                destinationPlacementFilters.put(location, itemMapping);
            } else {
                destinationPlacementFilters.remove(location);
            }
        }

        destinationToItem.compute(location, (ignored, values) -> {
            if (values == null) {
                values = new HashSet<>();
            }
            values.addAll(itemMapping.keySet());
            return values;
        });
    }

    public void removeSink(Location location) {
        Set<String> items = destinationToItem.remove(location);
        if (items == null) {
            return;
        }

        if (items.isEmpty()) {
            anyItemDestinations.remove(location);
        } else {
            destinationPlacementFilters.remove(location);

            for (String itemName : items) {
                itemToDestination.compute(itemName, (ignored, values) -> {
                    Objects.requireNonNull(values).remove(location);

                    if (values.isEmpty()) {
                        return null;
                    }

                    return values;
                });
            }
        }
    }

    public Set<String> getSinksAtLocation(Location location) {
        return destinationToItem.getOrDefault(location, Set.of());
    }

    private void toPixieSinks(List<List<PixieSink>> sinkLists, String itemName, List<Location> locations) {
        for (Location location : locations) {
            Map<String, List<Integer>> locationsFilters = destinationPlacementFilters.get(location);
            List<Integer> slotFilters = locationsFilters == null ? null : locationsFilters.get(itemName);
            if (slotFilters != null) {
                // High Priority
                sinkLists.get(0).add(new PixieSink(location, slotFilters));
            } else {
                // Medium Priority
                sinkLists.get(1).add(new PixieSink(location, null));
            }
        }
    }

    private List<PixieSink> toPixieSinks(List<Location> locations) {
        List<PixieSink> newList = new ArrayList<>(locations.size());
        for (Location location : locations) {
            newList.add(new PixieSink(location, null));
        }
        return newList;
    }

    public List<PixieSink> getSinksForItem(String itemName) {
        List<List<PixieSink>> prioritizedSinks = List.of(
            new ArrayList<>(),                 // High Priority
            new ArrayList<>(),                 // Medium Priority
            toPixieSinks(anyItemDestinations)  // Low Priority
        );

        toPixieSinks(prioritizedSinks, itemName, itemToDestination.getOrDefault(itemName, new ArrayList<>()));

        List<PixieSink> aggregateList = new ArrayList<>();
        for (List<PixieSink> sinks : prioritizedSinks) {
            Collections.shuffle(sinks, ThreadLocalRandom.current());
            aggregateList.addAll(sinks);
        }
        return aggregateList;
    }
}
