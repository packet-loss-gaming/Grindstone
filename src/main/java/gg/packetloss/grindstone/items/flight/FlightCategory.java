package gg.packetloss.grindstone.items.flight;

public enum FlightCategory {
    MAGIC_BUCKET_FAST(.4f),
    MAGIC_BUCKET_MEDIUM(.2f),
    MAGIC_BUCKET_SLOW(.1f),

    PIXIE_DUST(.6f);

    private final float speed;

    private FlightCategory(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }
}
