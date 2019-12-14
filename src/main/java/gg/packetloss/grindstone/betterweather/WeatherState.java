package gg.packetloss.grindstone.betterweather;

import java.util.*;

public class WeatherState {
    private Deque<WeatherEvent> weatherQueue = new LinkedList<>();
    private WeatherType currentWeather = WeatherType.CLEAR;
    private transient boolean dirty = false;

    public boolean isDirty() {
        return dirty;
    }

    public void resetDirtyFlag() {
        dirty = false;
    }

    public Optional<WeatherEvent> getNewWeatherEvent() {
        if (!weatherQueue.peek().shouldActivate()) {
            return Optional.empty();
        }

        WeatherEvent currentEvent = weatherQueue.poll();

        currentWeather = currentEvent.getWeatherType();
        dirty = true;

        return Optional.of(currentEvent);
    }

    public WeatherType getCurrentWeather() {
        return currentWeather == null ? WeatherType.CLEAR : currentWeather;
    }

    public int getQueueSize() {
        return weatherQueue.size();
    }

    public void clearQueue() {
        weatherQueue.clear();
        dirty = true;
    }

    public void addToQueue(WeatherEvent event) {
        weatherQueue.add(event);
        dirty = true;
    }

    public List<WeatherEvent> getCopiedQueue() {
        return new ArrayList<>(weatherQueue);
    }

    public long getLastWeatherEvent() {
        if (weatherQueue.isEmpty()) {
            return System.currentTimeMillis();
        }

        return weatherQueue.getLast().getActivationTime();
    }
}
