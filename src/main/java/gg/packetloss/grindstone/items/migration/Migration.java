/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.migration;

import org.bukkit.inventory.ItemStack;

import java.util.function.Function;
import java.util.function.Predicate;

public interface Migration extends Predicate<ItemStack>, Function<ItemStack, ItemStack> {
}
