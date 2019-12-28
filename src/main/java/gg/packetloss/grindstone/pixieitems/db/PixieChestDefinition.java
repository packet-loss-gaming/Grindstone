package gg.packetloss.grindstone.pixieitems.db;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Set;

public class PixieChestDefinition {
    private String worldName;
    private int x;
    private int y;
    private int z;
    private Set<String> itemNames;

    public PixieChestDefinition(String worldName, int x, int y, int z, Set<String> itemNames) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.itemNames = itemNames;
    }

    public Location getLocation() {
        World world = Bukkit.getWorld(worldName);
        return new Location(world, x, y, z);
    }

    public ChestKind getChestKind() {
        return itemNames == null ? ChestKind.SOURCE : ChestKind.SINK;
    }

    public Set<String> getSinkItems() {
        return itemNames;
    }
}
