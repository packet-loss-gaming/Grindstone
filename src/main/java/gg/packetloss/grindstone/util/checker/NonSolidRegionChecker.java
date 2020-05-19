package gg.packetloss.grindstone.util.checker;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.util.RegionUtil;
import org.bukkit.Location;
import org.bukkit.World;

public class NonSolidRegionChecker extends RegionChecker {
    private World world;

    public NonSolidRegionChecker(Region region, World world) {
        super(region);
        this.world = world;
    }

    public NonSolidRegionChecker(ProtectedRegion region, World world) {
        this(RegionUtil.convert(region).orElseThrow(), world);
    }

    @Override
    public Boolean evaluate(BlockVector3 v) {
        Location l = new Location(world, v.getX(), v.getY(), v.getZ());
        return super.evaluate(v) && !l.getBlock().getType().isSolid();
    }
}
