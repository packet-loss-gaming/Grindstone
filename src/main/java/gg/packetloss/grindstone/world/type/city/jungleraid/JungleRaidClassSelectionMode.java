package gg.packetloss.grindstone.world.type.city.jungleraid;

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
