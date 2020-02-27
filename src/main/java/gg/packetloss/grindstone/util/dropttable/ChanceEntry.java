package gg.packetloss.grindstone.util.dropttable;

import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

class ChanceEntry<T> {
    private final Function<T, Integer> chanceSupplier;
    private final Supplier<ItemStack> supplier;

    ChanceEntry(Function<T, Integer> chanceSupplier, Supplier<ItemStack> supplier) {
        this.chanceSupplier = chanceSupplier;
        this.supplier = supplier;
    }

    public Optional<ItemStack> get(T info) {
        if (ChanceUtil.getChance(chanceSupplier.apply(info))) {
            return Optional.of(supplier.get());
        }

        return Optional.empty();
    }
}
