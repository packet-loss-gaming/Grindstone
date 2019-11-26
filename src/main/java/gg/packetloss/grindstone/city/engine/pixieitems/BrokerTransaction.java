package gg.packetloss.grindstone.city.engine.pixieitems;

import org.bukkit.inventory.ItemStack;

public interface BrokerTransaction {
    public boolean isAuthorized();
    public void complete(ItemStack unused);
}
