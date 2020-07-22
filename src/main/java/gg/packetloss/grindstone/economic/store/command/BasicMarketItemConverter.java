/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.command;

import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class BasicMarketItemConverter {
    private MarketComponent component;

    public BasicMarketItemConverter(MarketComponent component) {
        this.component = component;
    }

    private Stream<MarketItem> getItemStream(String filter, InjectedValueAccess context) {
        CommandSender sender = context.injectedValue(Key.of(CommandSender.class)).orElse(Bukkit.getConsoleSender());
        return component.getItemListFor(sender, filter).stream();
    }

    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return getItemStream(argument, context)
                .map(MarketItem::getLookupName)
                .filter((s) -> argument.isEmpty() || s.contains(argument.toUpperCase()))
                .collect(Collectors.toList());
    }
}
