package gg.packetloss.grindstone.city.engine.skywars;

public enum SkyWarsPointEvent {
    GAME_WON(100, "VICTORY!");

    private final int amount;
    private final String caption;

    private SkyWarsPointEvent(int amount, String caption) {
        this.amount = amount;
        this.caption = caption;
    }

    public int getAdjustment() {
        return amount;
    }

    public String getCaption() {
        return caption;
    }
}
