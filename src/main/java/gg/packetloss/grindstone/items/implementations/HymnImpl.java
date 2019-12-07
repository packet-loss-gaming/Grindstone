/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class HymnImpl extends AbstractItemFeatureImpl {

    private Map<CustomItems, HymnSingEvent.Hymn> hymns = new HashMap<>();

    public void addHymn(CustomItems item, HymnSingEvent.Hymn hymn) {
        hymns.put(item, hymn);
    }

    @Override
    public boolean onItemRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        for (Map.Entry<CustomItems, HymnSingEvent.Hymn> entry : hymns.entrySet()) {
            if (ItemUtil.isHoldingItem(player, entry.getKey())) {
                //noinspection AccessStaticViaInstance
                inst.callEvent(new HymnSingEvent(player, entry.getValue()));
                return true;
            }
        }

        return false;
    }
}
