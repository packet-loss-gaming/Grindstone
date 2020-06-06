package gg.packetloss.grindstone.guild.base;

import org.bukkit.Location;
import org.bukkit.World;

public class RogueBase implements GuildBase {
    private World city;

    public RogueBase(World city) {
        this.city = city;
    }

    @Override
    public Location getLocation() {
        return new Location(city, -337.5, 60, -263.5, 90, 0);
    }
}
