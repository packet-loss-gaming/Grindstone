package gg.packetloss.grindstone.util.bridge;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.config.WorldConfiguration;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;

public class WorldGuardBridge {
    private WorldGuardBridge() { }

    public static WorldConfiguration getWorldConfig(World world) {
        return WorldGuard.getInstance().getPlatform().getGlobalStateManager().get(new BukkitWorld(world));
    }

    public static RegionManager getManagerFor(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(world));
    }

    public static boolean canBuildAt(Player player, Block block) {
        ApplicableRegionSet regionSet = getManagerFor(block.getWorld()).getApplicableRegions(toBlockVec3(block));
        return regionSet.queryState(wrap(player), Flags.BUILD) == StateFlag.State.ALLOW;
    }

    public static LocalPlayer wrap(Player player) {
        return WorldGuardPlugin.inst().wrapPlayer(player);
    }

    public static LocalPlayer wrap(OfflinePlayer player) {
        return WorldGuardPlugin.inst().wrapOfflinePlayer(player);
    }

    public static boolean hasRegionsAt(Location l) {
        return WorldGuardBridge.getManagerFor(l.getWorld()).getApplicableRegions(toBlockVec3(l)).size() > 0;
    }
}
