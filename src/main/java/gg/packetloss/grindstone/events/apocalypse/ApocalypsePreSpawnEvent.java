package gg.packetloss.grindstone.events.apocalypse;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.List;

public class ApocalypsePreSpawnEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Location initialStrikePoint;
    private final List<Location> strikePoints = new ArrayList<>();

    public ApocalypsePreSpawnEvent(Location initialStrikePoint) {
        this.initialStrikePoint = initialStrikePoint;

        this.strikePoints.add(getInitialLightningStrikePoint());
    }

    public Location getInitialLightningStrikePoint() {
        return initialStrikePoint.clone();
    }

    public List<Location> getLightningStrikePoints() {
        return strikePoints;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
