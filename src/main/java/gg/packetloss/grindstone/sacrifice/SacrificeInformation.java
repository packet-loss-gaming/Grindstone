/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.command.CommandSender;

public class SacrificeInformation {
    private final boolean hasSacrificeTome;
    private final boolean hasCleanlyTome;
    private final int maxItems;
    private final double baseValue;
    private final double totalValueModifier;

    /**
     * This class is used to define a sacrifice based on a numerical value and quantity.
     *
     * @param sender   - The triggering sender
     * @param maxItems - The maximum amount of items to return
     * @param value    - The value put towards the items returned
     */
    public SacrificeInformation(CommandSender sender, int maxItems, double value) {
        this.hasSacrificeTome = sender.hasPermission("aurora.tome.sacrifice");
        this.hasCleanlyTome = sender.hasPermission("aurora.tome.cleanly");
        this.maxItems = maxItems;
        this.baseValue = value;

        this.totalValueModifier = calculateTotalValueModifier();
    }

    private double calculateTotalValueModifier() {
         double randomizedBaseScale = ChanceUtil.getRangedRandom(.8, 1.2);
         double lootBasedScale = Math.min(.9, ((int) (baseValue / 250000)) * .1);
         return randomizedBaseScale - lootBasedScale;
    }

    /**
     * This class is used to define a sacrifice based on a and quantity.
     *
     * @param sender   - The triggering sender
     * @param value    - The value put towards the items returned
     */
    public SacrificeInformation(CommandSender sender, double value) {
        this(sender, -1, value);
    }

    public boolean hasSacrificeTome() {
        return hasSacrificeTome;
    }

    public boolean hasCleanlyTome() {
        return hasCleanlyTome;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public double getValue() {
        return baseValue * totalValueModifier;
    }

    public int getModifierRoll() {
        return ChanceUtil.getRandomNTimes((int) baseValue, 2) - 1;
    }
}
