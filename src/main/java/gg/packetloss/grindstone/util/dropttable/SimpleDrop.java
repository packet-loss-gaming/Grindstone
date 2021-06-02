/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

class SimpleDrop implements Drop {
    private final Player player;
    private final ItemStack itemStack;

    public SimpleDrop(ItemStack itemStack) {
        this.player = null;
        this.itemStack = itemStack;
    }

    public SimpleDrop(Player player, ItemStack itemStack) {
        this.player = player;
        this.itemStack = itemStack;
    }

    @Override
    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }

    @Override
    public ItemStack getDrop() {
        return itemStack;
    }
}
