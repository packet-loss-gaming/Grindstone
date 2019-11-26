package gg.packetloss.grindstone.city.engine.jungleraid;

public enum JungleRaidClassSelectionMode {
    SELECTION(true),
    RANDOM(false),
    SURVIVAL(false),
    SCAVENGER(true);

    private boolean allowSelection;

    private JungleRaidClassSelectionMode(boolean allowSelection) {
        this.allowSelection = allowSelection;
    }

    public boolean allowsSelection() {
        return allowSelection;
    }
}
