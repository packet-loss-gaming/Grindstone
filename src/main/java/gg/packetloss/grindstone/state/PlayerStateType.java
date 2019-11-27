package gg.packetloss.grindstone.state;

public enum PlayerStateType {
    ADMIN(true, true);

    private final boolean hasVitals;
    private final boolean hasInventory;

    PlayerStateType(boolean hasVitals, boolean hasInventory) {
        this.hasVitals = hasVitals;
        this.hasInventory = hasInventory;
    }

    public boolean hasVitals() {
        return hasVitals;
    }

    public boolean hasInventory() {
        return hasInventory;
    }
}
