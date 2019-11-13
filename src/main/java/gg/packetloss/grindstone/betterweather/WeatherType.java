package gg.packetloss.grindstone.betterweather;

public enum WeatherType {
    CLEAR(false, false),
    RAIN(true, false),
    THUNDERSTORM(true, true);

    private boolean storming;
    private boolean thundering;

    private WeatherType(boolean storming, boolean thundering) {
        this.storming = storming;
        this.thundering = thundering;
    }

    public boolean isStorm() {
        return storming;
    }

    public boolean hasThunder() {
        return thundering;
    }
}
