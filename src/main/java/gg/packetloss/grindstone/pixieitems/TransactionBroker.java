package gg.packetloss.grindstone.pixieitems;

import org.bukkit.inventory.ItemStack;

public interface TransactionBroker {
    public BrokerTransaction authorizeMovement(ItemStack stack);
    public void applyCharges();
}
