package gg.packetloss.grindstone.util.dropttable;

import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Supplier;

class ChanceEntry {
    private final int chance;
    private final Supplier<ItemStack> supplier;

    ChanceEntry(int chance, Supplier<ItemStack> supplier) {
        this.chance = chance;
        this.supplier = supplier;
    }

    public Optional<ItemStack> get(int chanceModifier) {
        if (ChanceUtil.getChance(chance / chanceModifier)) {
            return Optional.of(supplier.get());
        }

        return Optional.empty();
    }
}
