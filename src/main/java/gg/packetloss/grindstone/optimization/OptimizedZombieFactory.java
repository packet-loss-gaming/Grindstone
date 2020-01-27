package gg.packetloss.grindstone.optimization;

import gg.packetloss.hackbook.entity.HBSimpleZombie;
import org.bukkit.Location;
import org.bukkit.entity.Zombie;

public class OptimizedZombieFactory {
    private OptimizedZombieFactory() { }

    public static Zombie create(Location location) {
        return HBSimpleZombie.spawn(location);
    }
}
