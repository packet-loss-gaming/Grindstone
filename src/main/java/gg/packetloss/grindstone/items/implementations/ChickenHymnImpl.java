/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;

public class ChickenHymnImpl extends AbstractItemFeatureImpl {
    @EventHandler(ignoreCancelled = true)
    public void onHymnSing(HymnSingEvent event) {
        Player player = event.getPlayer();
        HymnSingEvent.Hymn hymn = event.getHymn();

        if (hymn != HymnSingEvent.Hymn.CHICKEN) return;
        player.getNearbyEntities(4, 4, 4).stream()
                .filter(e -> (e instanceof Item || e instanceof Chicken)).limit(30).forEach(e -> {
            Location l = e.getLocation();
            if (e instanceof Item) {
                for (int i = 0; i < 3; i++) {
                    Chicken chicken = l.getWorld().spawn(l, Chicken.class);
                    chicken.setRemoveWhenFarAway(true);
                }
                e.remove();
                ChatUtil.sendNotice(player, "The item transforms into chickens!");
            } else if (((Chicken) e).getRemoveWhenFarAway()) {
                if (ChanceUtil.getChance(3)) {
                    l.getWorld().dropItem(l, new ItemStack(Material.COOKED_CHICKEN));
                }
                e.remove();
            }
        });
    }
}
