package gg.packetloss.grindstone.city.engine.pixieitems.broker;

import gg.packetloss.grindstone.city.engine.pixieitems.BrokerTransaction;
import gg.packetloss.grindstone.city.engine.pixieitems.TransactionBroker;
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
