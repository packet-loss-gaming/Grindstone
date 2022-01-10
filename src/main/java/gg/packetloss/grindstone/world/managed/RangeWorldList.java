/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.managed;

import java.util.*;
import java.util.stream.Collectors;

public class RangeWorldList {
    private final List<Entry> rangeWorldEntries = new ArrayList<>();
    private final Map<ManagedWorldTimeContext, Entry> timeContextToEntry = new HashMap<>();
    private final Map<String, ManagedWorldTimeContext> worldToTimeContext = new HashMap<>();

    protected RangeWorldList() {
        register("Halzeil", ManagedWorldTimeContext.V_1_15);
        register("Crezat", ManagedWorldTimeContext.V_1_18);

        completeRegistration();
    }

    private void register(String name, ManagedWorldTimeContext timeContext) {
        Entry entry = new Entry(name, timeContext);
        rangeWorldEntries.add(entry);

        timeContextToEntry.put(timeContext, entry);
    }

    private void completeRegistration() {
        timeContextToEntry.put(
            ManagedWorldTimeContext.LATEST_ARCHIVED,
            timeContextToEntry.get(ManagedWorldTimeContext.getLatestArchive())
        );
        timeContextToEntry.put(
            ManagedWorldTimeContext.LATEST,
            timeContextToEntry.get(ManagedWorldTimeContext.getLatest())
        );

        for (Map.Entry<ManagedWorldTimeContext, Entry> entry : timeContextToEntry.entrySet()) {
            worldToTimeContext.put(entry.getValue().getName(), entry.getKey());
        }
    }

    public boolean isRangedWorld(String worldName) {
        return rangeWorldEntries.stream().anyMatch((entry) -> {
            return entry.getName().equals(worldName) || entry.getNetherName().equals(worldName);
        });
    }

    public boolean isRangedOverworld(String worldName) {
        return rangeWorldEntries.stream().anyMatch((entry) -> entry.getName().equals(worldName));
    }

    public boolean isRangedNether(String worldName) {
        if (!worldName.endsWith("_nether")) {
            return false;
        }

        return isRangedWorld(worldName.replaceFirst("_nether", ""));
    }

    public String getOverworld(ManagedWorldTimeContext timeContext) {
        return timeContextToEntry.get(timeContext).getName();
    }

    public String getNether(ManagedWorldTimeContext timeContext) {
        return timeContextToEntry.get(timeContext).getNetherName();
    }

    public Set<String> getOverworlds() {
        return rangeWorldEntries.stream().map(Entry::getName).collect(Collectors.toSet());
    }

    public Set<String> getNethers() {
        return rangeWorldEntries.stream().map(Entry::getNetherName).collect(Collectors.toSet());
    }

    public ManagedWorldTimeContext getTimeContext(String worldName) {
        if (worldName.endsWith("_nether")) {
            worldName = worldName.replaceFirst("_nether", "");
        }

        return worldToTimeContext.get(worldName);
    }

    public static class Entry {
        private final String name;
        private final ManagedWorldTimeContext timeContext;

        public Entry(String name, ManagedWorldTimeContext timeContext) {
            this.name = name;
            this.timeContext = timeContext;
        }

        public String getName() {
            return name;
        }

        public String getNetherName() {
            return name + "_nether";
        }

        public ManagedWorldTimeContext getTimeContext() {
            return timeContext;
        }
    }
}
