/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Date;
import java.util.List;

import static gg.packetloss.grindstone.util.item.ItemUtil.EXPIRY_DATE_START;
import static gg.packetloss.grindstone.util.item.ItemUtil.ITEM_DATE_FORMAT;

public class CustomExpiringGift extends CustomGiftItem {
    private final long duration;

    public CustomExpiringGift(CustomItems item, CustomItem baseItem, long duration) {
        super(item, baseItem);
        Validate.isTrue(item.hasExpiration());
        this.duration = duration;
    }

    @Override
    protected ItemStack build(CustomItems identity) {
        ItemStack itemStack = super.build(identity);
        ItemMeta meta = itemStack.getItemMeta();
        long currentTime = System.currentTimeMillis();
        meta.setLore(List.of(EXPIRY_DATE_START + ITEM_DATE_FORMAT.format(new Date(duration + currentTime))));
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
