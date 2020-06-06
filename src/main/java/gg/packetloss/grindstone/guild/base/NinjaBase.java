package gg.packetloss.grindstone.guild.base;

import org.bukkit.Location;
import org.bukkit.World;

public class NinjaBase implements GuildBase {
    private World city;

    public NinjaBase(World city) {
        this.city = city;
    }

    @Override
    public Location getLocation() {
        return new Location(city, 150, 45, -372, 180, 0);
    }
}
