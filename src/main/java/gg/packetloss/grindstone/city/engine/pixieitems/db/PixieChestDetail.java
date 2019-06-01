package gg.packetloss.grindstone.city.engine.pixieitems.db;

import java.util.Set;

public class PixieChestDetail {
    private final int networkID;
    private final Set<String> itemNames;

    public PixieChestDetail(int networkID, Set<String> itemNames) {
        this.networkID = networkID;
        this.itemNames = itemNames;
    }

    public int getNetworkID() {
        return networkID;
    }

    public ChestKind getChestKind() {
        return itemNames == null ? ChestKind.SOURCE : ChestKind.SINK;
    }

    public Set<String> getSinkItems() {
        return itemNames;
    }
}
