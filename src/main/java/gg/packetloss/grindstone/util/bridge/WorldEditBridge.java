package gg.packetloss.grindstone.util.bridge;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitConfiguration;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class WorldEditBridge {
    private WorldEditBridge() { }

    public static EditSession getSystemEditSessionFor(World world) {
        return WorldEdit.getInstance().getEditSessionFactory().getEditSession(new BukkitWorld(world), -1);
    }

    public static LocalSession getSession(Player player) {
        return WorldEdit.getInstance().getSessionManager().getIfPresent(new BukkitPlayer(player));
    }

    public static Region getSelectionFor(Player player) {
        LocalSession session = getSession(player);
        if (session == null) {
            return null;
        }

        var selectionWorld = session.getSelectionWorld();
        if (!session.isSelectionDefined(selectionWorld)) {
            return null;
        }


        try {
            return session.getSelection(selectionWorld);
        } catch (IncompleteRegionException e) {
            throw new IllegalStateException(e);
        }
    }

    public static BlockVector3 toBlockVec3(Location location) {
        return BlockVector3.at(location.getX(), location.getY(), location.getZ());
    }

    public static BlockVector3 toBlockVec3(Block block) {
        return BlockVector3.at(block.getX(), block.getY(), block.getZ());
    }

    public static BukkitPlayer wrap(Player player) {
        return new BukkitPlayer(player);
    }

    public static BukkitConfiguration getLocalConfiguration() {
        return (BukkitConfiguration) WorldEdit.getInstance().getConfiguration();
    }
}
