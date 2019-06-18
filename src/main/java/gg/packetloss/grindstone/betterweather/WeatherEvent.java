package gg.packetloss.grindstone.betterweather;

public class WeatherEvent {
    private long activationTime;
    private WeatherType weatherType;

    public WeatherEvent(long activationTime, WeatherType weatherType) {
        this.activationTime = activationTime;
        this.weatherType = weatherType;
    }

    public long getActivationTime() {
        return activationTime;
    }

    public boolean shouldActivate() {
        return System.currentTimeMillis() - activationTime >= 0;
    }

    public WeatherType getWeatherType() {
        return weatherType;
    }
}
