/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.item;

import com.google.common.base.Joiner;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.Objects;

import static gg.packetloss.grindstone.util.item.ItemNameCalculator.*;

public class ItemNameDeserializer {
    private static String getBaseName(String itemNameString) {
        String[] parts = itemNameString.split(SERIALIZATION_SPLIT);
        return parts[0];
    }

    private static void restoreItemMetadata(String itemNameString, ItemMeta stackMeta) {
        if (stackMeta instanceof PotionMeta) {
            String[] parts = itemNameString.split(POTION_SPLIT);
            if (parts.length < 2) {
                return;
            }

            String metaDataInfo = Joiner.on(POTION_SPLIT).join(Arrays.copyOfRange(parts, 1, parts.length));
            boolean isExtended = false;
            boolean isUpgraded = false;
            if (metaDataInfo.endsWith(EXTENDED_POSTFIX)) {
                isExtended = true;
                metaDataInfo = metaDataInfo.substring(0, metaDataInfo.length() - EXTENDED_POSTFIX.length());
            } else if (metaDataInfo.endsWith(UPGRADED_POSTFIX)) {
                isUpgraded = true;
                metaDataInfo = metaDataInfo.substring(0, metaDataInfo.length() - UPGRADED_POSTFIX.length());
            }
            PotionType baseType = PotionType.valueOf(metaDataInfo.toUpperCase());
            ((PotionMeta) stackMeta).setBasePotionData(new PotionData(baseType, isExtended, isUpgraded));
        }
    }

    private static void restoreItemMetadata(String itemNameString, ItemStack stack) {
        ItemMeta stackMeta = stack.getItemMeta();
        restoreItemMetadata(itemNameString, stackMeta);
        stack.setItemMeta(stackMeta);
    }

    public static ItemStack getBaseStack(String name) {
        if (name.startsWith("grindstone:")) {
            name = name.replaceFirst("grindstone:", "");
            CustomItems item = CustomItems.valueOf(name.toUpperCase());
            return CustomItemCenter.build(item);
        }

        ItemStack stack = new ItemStack(Objects.requireNonNull(Material.matchMaterial(getBaseName(name))), 1);
        restoreItemMetadata(name, stack);
        return stack;
    }
}
