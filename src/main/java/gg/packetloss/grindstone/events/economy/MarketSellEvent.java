/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.events.economy;

import gg.packetloss.grindstone.economic.store.transaction.MarketTransactionLine;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.math.BigDecimal;
import java.util.List;

public class MarketSellEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();

    private final List<MarketTransactionLine> items;
    private final BigDecimal payment;

    public MarketSellEvent(Player who, List<MarketTransactionLine> items, BigDecimal payment) {
        super(who);
        this.items = items;
        this.payment = payment;
    }

    public List<MarketTransactionLine> getTransactionLines() {
        return items;
    }

    public BigDecimal getPayment() {
        return payment;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
