/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory;

import gg.packetloss.grindstone.util.item.ItemNameDeserializer;
import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class FactoryJob {
    private final UUID playerID;
    private final String itemName;
    private final ItemStack itemStack;
    private int amountRemaining;

    public FactoryJob(UUID playerID, String itemName, int amountRemaining) {
        this.playerID = playerID;
        this.itemName = itemName;
        this.itemStack = ItemNameDeserializer.getBaseStack(itemName);
        this.amountRemaining = amountRemaining;
    }

    public UUID getPlayerID() {
        return playerID;
    }

    public String getItemName() {
        return itemName;
    }

    public boolean isIncomplete() {
        return !isComplete();
    }
    
    public boolean isComplete() {
        return amountRemaining == 0;
    }

    public int getItemsRemaining() {
        return amountRemaining;
    }

    public void increaseProduction(int amount) {
        Validate.isTrue(amount > 0);
        amountRemaining += amount;
    }

    public ItemStack produceItem() {
        Validate.isTrue(amountRemaining > 0);
        --amountRemaining;
        return itemStack.clone();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FactoryJob oJob && oJob.itemName.equals(itemName) && oJob.playerID.equals(playerID);
    }
}
