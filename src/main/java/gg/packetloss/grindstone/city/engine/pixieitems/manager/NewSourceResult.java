package gg.packetloss.grindstone.city.engine.pixieitems.manager;

public class NewSourceResult implements NewChestResult {
    private final boolean isNew;

    public NewSourceResult(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
