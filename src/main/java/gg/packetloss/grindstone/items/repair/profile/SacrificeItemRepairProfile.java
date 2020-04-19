package gg.packetloss.grindstone.items.repair.profile;

import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class SacrificeItemRepairProfile implements RepairProfile, ItemRepairProfile {
    private final ItemFamily itemFamily;
    private final ItemStack repairItem;
    private final float repairPercentage;

    public SacrificeItemRepairProfile(ItemFamily itemFamily, ItemStack repairItem, float repairPercentage) {
        this.itemFamily = itemFamily;
        this.repairItem = repairItem;
        this.repairPercentage = repairPercentage;
    }

    public float getRepairPercentage() {
        return repairPercentage;
    }

    @Override
    public ItemStack getRepairItem() {
        return repairItem.clone();
    }

    @Override
    public boolean matches(ItemStack itemStack) {
        return ItemUtil.isInItemFamily(itemStack, itemFamily);
    }

    @Override
    public BaseComponent[] getWarningMessage() {
        return Text.of(
                ChatColor.YELLOW,
                "Sacrifice this item with at least one ",
                Text.of(TextAction.Hover.showItem(repairItem), ItemNameCalculator.getDisplayName(repairItem)),
                " in your inventory."
        ).build();
    }
}
