package gg.packetloss.grindstone.pixieitems.manager;

import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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

    public Set<String> getSinksAtLocation(Location location) {
        return destinationToItem.getOrDefault(location, Set.of());
    }

    private List<Location> randomizedCopy(List<Location> sourceList) {
        List<Location> newList = new ArrayList<>(sourceList.size());
        sourceList.forEach(loc -> newList.add(loc.clone()));
        Collections.shuffle(newList, ThreadLocalRandom.current());
        return newList;
    }

    public List<Location> getSinksForItem(String itemName) {
        List<Location> targeted = randomizedCopy(itemToDestination.getOrDefault(itemName, new ArrayList<>()));
        List<Location> misc = randomizedCopy(anyItemDestinations);

        List<Location> combined = new ArrayList<>(targeted.size() + misc.size());
        combined.addAll(targeted);
        combined.addAll(misc);
        return combined;
    }
}
