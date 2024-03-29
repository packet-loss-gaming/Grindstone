/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WarpManager {
    private WarpDatabase warpData;

    public WarpManager(WarpDatabase warpDatabase) {
        this.warpData = warpDatabase;
    }

    public List<WarpPoint> getWarpsForNamespace(UUID namespace) {
        return warpData.getWarpsInNamespace(namespace);
    }

    public List<WarpPoint> getWarpsForPlayer(Player player) {
        List<WarpPoint> warps = getWarpsForNamespace(player.getUniqueId());
        warps.addAll(warpData.getGlobalWarps());
        return warps;
    }

    public List<WarpPoint> getGlobalWarps() {
        return warpData.getGlobalWarps();
    }

    public Optional<WarpPoint> getExactWarp(WarpQualifiedName qualifiedName) {
        return warpData.getWarp(qualifiedName);
    }

    public Optional<WarpPoint> lookupWarp(CommandSender requester, String qualifier, String name) {
        // If the qualifier is equal to global, do a lookup in the global namespace
        if (qualifier.equalsIgnoreCase("global")) {
            return getExactWarp(new WarpQualifiedName(name));
        }

        // Try and use the qualifier as a macro player name
        UUID playerId = GeneralPlayerUtil.resolveMacroNamespace(requester, qualifier);
        if (playerId == null) {
            return Optional.empty();
        }

        return getExactWarp(new WarpQualifiedName(playerId, name));
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

    private WarpQualifiedName getLastPortalLocationNameFor(Player player, World world) {
        return new WarpQualifiedName(player.getUniqueId(), world.getName() + "-last-portal");
    }

    public Optional<WarpPoint> getLastPortalLocationFor(Player player, World world) {
        return warpData.getWarp(getLastPortalLocationNameFor(player, world));
    }

    public void setLastPortalLocation(Player player, Location location) {
        warpData.setWarp(getLastPortalLocationNameFor(player, location.getWorld()), location);
        warpData.save();
    }

    public boolean destroyWarp(WarpQualifiedName qualifiedName) {
        boolean destroyed = warpData.destroyWarp(qualifiedName).isPresent();
        if (destroyed) {
            warpData.save();
        }

        return destroyed;
    }
}
