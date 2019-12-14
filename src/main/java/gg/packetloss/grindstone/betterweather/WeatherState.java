package gg.packetloss.grindstone.betterweather;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

public class WeatherState {
    private Deque<WeatherEvent> weatherQueue = new LinkedList<>();
    private WeatherType currentWeather = WeatherType.CLEAR;

    public Optional<WeatherEvent> getNewWeatherEvent() {
        if (weatherQueue.size() < 2) {
            return Optional.empty();
        }

        if (!weatherQueue.peek().shouldActivate()) {
            return Optional.empty();
        }

        WeatherEvent currentEvent = weatherQueue.poll();
        currentWeather = currentEvent.getWeatherType();
        return Optional.of(currentEvent);
    }

    public WeatherType getCurrentWeather() {
        return currentWeather == null ? WeatherType.CLEAR : currentWeather;
    }

    public Deque<WeatherEvent> getQueue() {
        return weatherQueue;
    }

    public long getLastWeatherEvent() {
        if (weatherQueue.isEmpty()) {
            return System.currentTimeMillis();
        }

        return weatherQueue.getLast().getActivationTime();
    }
}
