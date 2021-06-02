/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.Prayers;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeUnit;

public class GodFishImpl extends AbstractItemFeatureImpl {
    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();
        ItemStack stack = event.getItem();

        if (ItemUtil.isItem(stack, CustomItems.GOD_FISH)) {
            player.chat("The fish flow within me!");
            PrayerComponent.constructPrayer(player, Prayers.HULK, TimeUnit.MINUTES.toMillis(5));
        }
    }
}
