/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.migration;

import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MigrationManager {
    private final List<Migration> migrations = new ArrayList<>();

    public void add(Migration migration) {
        migrations.add(migration);
    }

    public Optional<ItemStack> applyUpdates(ItemStack itemStack) {
        if (!ItemUtil.isAuthenticCustomItem(itemStack)) {
            return Optional.empty();
        }

        boolean updated = false;
        for (Migration migration : migrations) {
            if (migration.test(itemStack)) {
                itemStack = migration.apply(itemStack);
                updated = true;
            }
        }

        if (updated) {
            return Optional.of(itemStack);
        } else {
            return Optional.empty();
        }
    }
}
