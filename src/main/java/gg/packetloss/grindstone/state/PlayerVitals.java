package gg.packetloss.grindstone.state;

public class PlayerVitals {
    private final double health;
    private final int hunger;
    private final float saturation;
    private final float exhaustion;
    private final int experience;

    public PlayerVitals(double health, int hunger, float saturation, float exhaustion, int experience) {
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.experience = experience;
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

    public int getExperience() {
        return experience;
    }
}
