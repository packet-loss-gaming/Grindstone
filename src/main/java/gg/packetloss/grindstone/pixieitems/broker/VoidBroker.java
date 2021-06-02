/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems.broker;

import gg.packetloss.grindstone.pixieitems.BrokerTransaction;
import gg.packetloss.grindstone.pixieitems.TransactionBroker;
import org.bukkit.inventory.ItemStack;

public class VoidBroker implements TransactionBroker {
    @Override
    public BrokerTransaction authorizeMovement(ItemStack stack) {
        return new VoidBrokerTransaction();
    }

    @Override
    public void applyCharges() { }

    private static class VoidBrokerTransaction implements BrokerTransaction {
        @Override
        public boolean isAuthorized() {
            return true;
        }

        @Override
        public void complete(ItemStack unused) { }
    }
}
