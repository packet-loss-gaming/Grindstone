/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class SplashPotionUtil {
    public static ThrownPotion throwMaxedSplashPotion(Location location, PotionType type) {
        ThrownPotion potion = location.getWorld().spawn(location, ThrownPotion.class);

        ItemStack brewedPotion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta potionMeta = (PotionMeta) brewedPotion.getItemMeta();

        // Prioritize upgradablility over extendability
        boolean isExtended = type.isExtendable() && !type.isUpgradeable();
        boolean isUpgraded = type.isUpgradeable();

        potionMeta.setBasePotionData(new PotionData(type, isExtended, isUpgraded));
        brewedPotion.setItemMeta(potionMeta);

        potion.setItem(brewedPotion);
        return potion;
    }
}
