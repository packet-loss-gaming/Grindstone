package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;

public class PermissionBindingTomeImpl extends AbstractItemFeatureImpl {

    private Map<CustomItems, String> tomes = new HashMap<>();
    private Permission permissionManager = null;

    public void addTome(CustomItems item, String permission) {
        tomes.put(item, permission);
    }

    private boolean checkPermissionsConfigured() {
        RegisteredServiceProvider<Permission> permissionProvider = server.getServicesManager().getRegistration(net
                .milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) permissionManager = permissionProvider.getProvider();

        return (permissionManager != null);
    }

    private boolean tryAddPermission(Player player, String permission) {
        if (permissionManager.has(player, permission)) {
            ChatUtil.sendError(player, "You already have this knowledge.");
            return false;
        }

        return permissionManager.playerAdd(null, player, permission);
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        if (!checkPermissionsConfigured()) {
            return false;
        }

        Player player = event.getPlayer();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            for (Map.Entry<CustomItems, String> entry : tomes.entrySet()) {
                if (ItemUtil.isHoldingItem(player, entry.getKey())) {
                    if (tryAddPermission(player, entry.getValue())) {
                        ItemUtil.removeItemOfName(player, CustomItemCenter.build(entry.getKey()), 1, false);
                        ChatUtil.sendNotice(player, ChatColor.GOLD + "You gain new knowledge.");
                    }
                    return true;
                }
            }
        }

        return false;
    }
}
