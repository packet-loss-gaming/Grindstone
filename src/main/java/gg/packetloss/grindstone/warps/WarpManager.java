package gg.packetloss.grindstone.warps;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class WarpManager {
    private WarpDatabase warpData;

    public WarpManager(WarpDatabase warpDatabase) {
        this.warpData = warpDatabase;
    }

    public List<WarpPoint> getWarpsForPlayer(Player player) {
        List<WarpPoint> warps = warpData.getWarpsInNamespace(player.getUniqueId());
        warps.addAll(warpData.getGlobalWarps());
        return warps;
    }

    public List<WarpPoint> getGlobalWarps() {
        return warpData.getGlobalWarps();
    }

    public Optional<WarpPoint> getExactWarp(WarpQualifiedName qualifiedName) {
        return warpData.getWarp(qualifiedName);
    }

    public Optional<WarpPoint> lookupWarp(String qualifier, String name) {
        // If the qualifier is equal to global, do a lookup in the global namespace
        if (qualifier.toLowerCase().equals("global")) {
            return getExactWarp(new WarpQualifiedName(name));
        }

        // Try and use the qualifier as a player name
        OfflinePlayer player = CommandBook.server().getOfflinePlayer(qualifier);
        if (player == null) {
            return Optional.empty();
        }

        return getExactWarp(new WarpQualifiedName(player.getUniqueId(), name));
    }

    public Optional<WarpPoint> lookupWarp(Player requester, String name) {
        // Try finding a warp with that name in the player's namespace
        Optional<WarpPoint> result = warpData.getWarp(new WarpQualifiedName(requester.getUniqueId(), name));
        if (result.isPresent()) {
            return result;
        }

        // Try finding a warp with that name in the global namespace
        return warpData.getWarp(new WarpQualifiedName(name));
    }

    public Optional<WarpPoint> setWarp(WarpQualifiedName qualifiedName, Location loc) {
        Optional<WarpPoint> warp = warpData.setWarp(qualifiedName, loc);
        warpData.save();

        return warp;
    }

    private WarpQualifiedName getHomeNameFor(Player player) {
        return new WarpQualifiedName(player.getUniqueId(), "home");
    }

    public Optional<WarpPoint> getHomeFor(Player player) {
        return warpData.getWarp(getHomeNameFor(player));
    }

    public void setPlayerHomeAndNotify(Player player, Location loc) {
        boolean isUpdate = warpData.setWarp(getHomeNameFor(player), loc).isPresent();
        warpData.save();

        if (isUpdate) {
            ChatUtil.sendNotice(player, "Your bed location has been updated.");
        } else {
            ChatUtil.sendNotice(player, "Your bed location has been set.");
        }
    }

    public boolean destroyWarp(WarpQualifiedName qualifiedName) {
        boolean destroyed = warpData.destroyWarp(qualifiedName).isPresent();
        if (destroyed) {
            warpData.save();
        }

        return destroyed;
    }
}
