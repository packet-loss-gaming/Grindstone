/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;

public class PlayerWalletUpdate extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final OfflinePlayer player;
    private final BigDecimal newBalance;

    public PlayerWalletUpdate(OfflinePlayer player, BigDecimal newBalance) {
        this.player = player;
        this.newBalance = newBalance;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
