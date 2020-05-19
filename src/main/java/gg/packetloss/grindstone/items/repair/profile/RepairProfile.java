package gg.packetloss.grindstone.items.repair.profile;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.inventory.ItemStack;

public interface RepairProfile {
    public boolean matches(ItemStack itemStack);
    public BaseComponent[] getWarningMessage();
}
