package gg.packetloss.grindstone.state;

public class PlayerVitals {
    private final double health;
    private final int hunger;
    private final float saturation;
    private final float exhaustion;

    public PlayerVitals(double health, int hunger, float saturation, float exhaustion) {
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
    }

    public double getHealth() {
        return health;
    }

    public int getHunger() {
        return hunger;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getExhaustion() {
        return exhaustion;
    }
}
