package us.arrowcraft.aurora.city.engine.arena;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.util.LocationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public abstract class AbstractRegionedArena {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private World world;
    private ProtectedRegion region;

    public AbstractRegionedArena(World world, ProtectedRegion region) {

        this.world = world;
        this.region = region;
    }

    public Player[] getContainedPlayers() {


        return getContainedPlayers(0);
    }

    public Player[] getContainedPlayers(int parentsUp) {

        List<Player> returnedList = new ArrayList<>();

        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();

        for (Player player : server.getOnlinePlayers()) {

            if (LocationUtil.isInRegion(world, r, player)) returnedList.add(player);
        }
        return returnedList.toArray(new Player[returnedList.size()]);
    }

    public boolean isEmpty() {

        for (Player player : server.getOnlinePlayers()) {

            if (contains(player)) return false;
        }
        return true;
    }

    public boolean contains(Entity entity) {

        return contains(entity.getLocation());
    }

    public boolean contains(Block block) {

        return contains(block.getLocation());
    }

    public boolean contains(Location location) {

        return LocationUtil.isInRegion(world, region, location);
    }

    public ProtectedRegion getRegion() {

        return region;
    }

    public World getWorld() {

        return world;
    }
}
