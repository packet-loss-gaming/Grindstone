package gg.packetloss.grindstone.city.engine.pixieitems.manager;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class NewSinkResult implements NewChestResult {
    private final int numChestsDeleted;
    private final ImmutableSet<String> itemNames;

    public NewSinkResult(Integer numChestsDeleted, Set<String> itemNames) {
        this.numChestsDeleted = numChestsDeleted;
        this.itemNames = ImmutableSet.copyOf(itemNames);
    }

    @Override
    public boolean isNew() {
        return numChestsDeleted == 0;
    }

    public ImmutableSet<String> getItemNames() {
        return itemNames;
    }
}
