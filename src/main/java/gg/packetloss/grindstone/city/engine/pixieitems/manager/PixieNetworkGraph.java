package gg.packetloss.grindstone.city.engine.pixieitems.manager;

import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;

public class PixieNetworkGraph {
    private Set<Location> sources = new HashSet<>();

    private Map<String, List<Location>> itemToDestination = new HashMap<>();
    private Map<Location, Set<String>> destinationToItem = new HashMap<>();
    private List<Location> anyItemDestinations = new ArrayList<>();

    public void addSource(Location location) {
        sources.add(location);
    }

    public void removeSource(Location location) {
        sources.remove(location);
    }

    public Set<Location> getSources() {
        return sources.stream().map(Location::clone).collect(Collectors.toSet());
    }

    public void addSink(Set<String> itemNames, Location location) {
        if (itemNames.isEmpty()) {
            anyItemDestinations.add(location);
        } else {
            for (String itemName : itemNames) {
                itemToDestination.compute(itemName, (ignored, values) -> {
                    if (values == null) {
                        values = new ArrayList<>();
                    }
                    values.add(location);
                    return values;
                });
            }
        }

        destinationToItem.compute(location, (ignored, values) -> {
            if (values == null) {
                values = new HashSet<>();
            }
            values.addAll(itemNames);
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

    public void removeChest(Location location) {
        removeSource(location);
        removeSink(location);
    }

    public List<Location> getSinksForItem(String itemName) {
        List<Location> prioritizedList = new ArrayList<>();

        for (Location location : itemToDestination.getOrDefault(itemName, new ArrayList<>())) {
            prioritizedList.add(location.clone());
        }

        for (Location location : anyItemDestinations) {
            prioritizedList.add(location.clone());
        }

        return prioritizedList;
    }
}
