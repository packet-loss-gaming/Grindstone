/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.enchantments.Enchantment;

public class Enchant {

    private Enchantment enchant;
    private int level;

    public Enchant(Enchantment enchant, int level) {
        this.enchant = enchant;
        this.level = level;
    }

    public org.bukkit.enchantments.Enchantment getEnchant() {
        return enchant;
    }

    public int getLevel() {
        return level;
    }
}
