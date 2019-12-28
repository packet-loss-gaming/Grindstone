package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.StringUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;

public class GuildOathImpl extends AbstractItemFeatureImpl {
    private Map<CustomItems, GuildType> tomes = new HashMap<>();

    public void addOath(CustomItems item, GuildType type) {
        tomes.put(item, type);
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            for (Map.Entry<CustomItems, GuildType> entry : tomes.entrySet()) {
                if (ItemUtil.isHoldingItem(player, entry.getKey())) {
                    GuildType guildType = entry.getValue();

                    if (guilds.joinGuild(player, guildType)) {
                        ItemUtil.removeItemOfName(player, CustomItemCenter.build(entry.getKey()), 1, false);
                    } else {
                        ChatUtil.sendWarning(player, "You are already in the " + StringUtil.toTitleCase(guildType.name()) + " Guild.");
                    }

                    return true;
                }
            }
        }

        return false;
    }
}
