package gg.packetloss.grindstone.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStateRecord {
    private Map<String, PlayerVitals> vitals = new HashMap<>();
    private Map<String, UUID> inventories = new HashMap<>();

    public Map<String, PlayerVitals> getVitals() {
        return vitals;
    }

    public Map<String, UUID> getInventories() {
        return inventories;
    }

    public boolean isEmpty() {
        return vitals.isEmpty() && inventories.isEmpty();
    }
}
