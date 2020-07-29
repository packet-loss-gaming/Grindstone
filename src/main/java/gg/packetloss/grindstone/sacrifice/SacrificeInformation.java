/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.sacrifice;

import org.bukkit.command.CommandSender;

public class SacrificeInformation {
    private final boolean hasSacrificeTome;
    private final boolean hasCleanlyTome;
    private final int maxItems;
    private final double value;
    private final int modifier;

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
        this.value = value * .9;
        this.modifier = (int) (Math.sqrt(value) * 1.5);
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
        return value;
    }

    public int getModifier() {
        return modifier;
    }
}
