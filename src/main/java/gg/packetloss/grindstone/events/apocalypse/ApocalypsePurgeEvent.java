package gg.packetloss.grindstone.events.apocalypse;

import org.bukkit.entity.Zombie;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class ApocalypsePurgeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final List<Zombie> zombies;

    public ApocalypsePurgeEvent(List<Zombie> zombies) {
        this.zombies = zombies;
    }

    public List<Zombie> getZombies() {
        return zombies;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
