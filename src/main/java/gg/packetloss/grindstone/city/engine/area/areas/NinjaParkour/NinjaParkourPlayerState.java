package gg.packetloss.grindstone.city.engine.area.areas.NinjaParkour;

import com.sk89q.worldedit.math.BlockVector2;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class NinjaParkourPlayerState {
    private long startTime = 0;
    private List<BlockVector2> columnVectors = new ArrayList<>();
    private BlockVector2 lastSurvivor = null;

    public long getStartTime() {
        return startTime;
    }

    public List<BlockVector2> getColumnVectors() {
        return columnVectors;
    }

    public BlockVector2 getLastSurvivor() {
        return lastSurvivor;
    }

    private boolean playerIsOnPoint(Player player, BlockVector2 point) {
        Location location = player.getLocation();
        return point.getBlockX() == location.getBlockX() && point.getBlockZ() == location.getBlockZ();
    }

    public boolean isOnStableColumn(Player player) {
        if (lastSurvivor == null) {
            return false;
        }

        return playerIsOnPoint(player, lastSurvivor);
    }

    public void cleanupPoints(Player player, Consumer<BlockVector2> callback) {
        Iterator<BlockVector2> it = columnVectors.iterator();
        while (it.hasNext()) {
            BlockVector2 pt = it.next();
            if (playerIsOnPoint(player, pt)) {
                continue;
            }

            callback.accept(pt);

            it.remove();
        }

        if (!columnVectors.isEmpty()) {
            lastSurvivor = columnVectors.get(0);
            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
        } else {
            lastSurvivor = WorldEditBridge.toBlockVec2(player);
            startTime = 0;
        }
    }
}
