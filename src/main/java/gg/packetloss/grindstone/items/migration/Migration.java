package gg.packetloss.grindstone.items.migration;

import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface Migration extends Predicate<ItemStack>, BiFunction<ItemStack, String, ItemStack> {
    Set<String> getValidOptions();
}
