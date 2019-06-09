package gg.packetloss.grindstone.items.migration.migrations;

import com.google.common.collect.Sets;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.migration.Migration;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class UnleashedSwordMigration implements Migration {
    @Override
    public Set<String> getValidOptions() {
        return Sets.newHashSet("heavy", "light");
    }

    @Override
    public ItemStack apply(ItemStack stack, String s) {
        if (s.equals("heavy")) {
            ItemStack sword = CustomItemCenter.build(CustomItems.UNLEASHED_SWORD);
            sword.setDurability(stack.getDurability());
            ItemUtil.copyEnchancements(stack, sword);
            return sword;
        } else {
            ItemStack sword = CustomItemCenter.build(CustomItems.UNLEASHED_SHORT_SWORD);
            sword.setDurability(stack.getDurability());
            ItemUtil.copyEnchancements(stack, sword);
            return sword;
        }
    }

    @Override
    public boolean test(ItemStack stack) {
        boolean isUnleashedSword = ItemUtil.matchesFilter(stack, CustomItems.UNLEASHED_SWORD.getColoredName());
        if (!isUnleashedSword) {
            return false;
        }

        return ItemUtil.getDamageModifier(stack) == 2.25;
    }
}
