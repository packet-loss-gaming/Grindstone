package gg.packetloss.grindstone.events;

import gg.packetloss.grindstone.betterweather.WeatherType;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.WorldEvent;

public class BetterWeatherChangeEvent extends WorldEvent {
    private static final HandlerList handlers = new HandlerList();

    private final WeatherType oldWeatherType;
    private final WeatherType newWeatherType;

    public BetterWeatherChangeEvent(World world, WeatherType oldWeatherType, WeatherType newWeatherType) {
        super(world);
        this.oldWeatherType = oldWeatherType;
        this.newWeatherType = newWeatherType;
    }

    public WeatherType getOldWeatherType() {
        return oldWeatherType;
    }

    public WeatherType getNewWeatherType() {
        return newWeatherType;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
