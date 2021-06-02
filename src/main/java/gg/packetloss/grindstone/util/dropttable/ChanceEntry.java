/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
