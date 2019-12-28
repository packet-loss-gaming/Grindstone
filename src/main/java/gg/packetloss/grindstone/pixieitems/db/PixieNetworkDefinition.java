package gg.packetloss.grindstone.pixieitems.db;

import java.util.ArrayList;
import java.util.List;

public class PixieNetworkDefinition {
    private final int networkID;
    private final List<PixieChestDefinition> definitions = new ArrayList<>();

    public PixieNetworkDefinition(int networkID) {
        this.networkID = networkID;
    }

    public int getNetworkID() {
        return networkID;
    }

    public List<PixieChestDefinition> getChests() {
        return definitions;
    }
}
