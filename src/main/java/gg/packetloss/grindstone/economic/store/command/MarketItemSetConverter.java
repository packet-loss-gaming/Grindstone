/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.command;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItem;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static gg.packetloss.grindstone.economic.store.MarketComponent.NOT_AVAILIBLE;
import static gg.packetloss.grindstone.util.item.ItemNameCalculator.matchItem;

public class MarketItemSetConverter implements ArgumentConverter<MarketItemSet> {
    private BasicMarketItemConverter marketItemConverter;

    public MarketItemSetConverter(MarketComponent component) {
        this.marketItemConverter = new BasicMarketItemConverter(component);
    }

    public static void register(CommandManager commandManager, MarketComponent component) {
        commandManager.registerConverter(Key.of(MarketItemSet.class), new MarketItemSetConverter(component));
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any item");
    }

    private Set<String> getLookupNames(String argument) {
        // Expand item names, and convert to lookup ready item names
        Set<String> expandedNames = ItemNameCalculator.expandNameMacros(argument);
        Set<String> expandedLookupNames = new HashSet<>();
        for (String expandedName : expandedNames) {
            Optional<String> optItemName = matchItem(expandedName);
            if (optItemName.isEmpty()) {
                return Set.of();
            }

            expandedLookupNames.add(optItemName.get());
        }

        return expandedLookupNames;
    }

    @Override
    public ConversionResult<MarketItemSet> convert(String argument, InjectedValueAccess context) {
        Set<String> expandedLookupNames = getLookupNames(argument);
        if (expandedLookupNames.isEmpty()) {
            return FailedConversion.from(new IllegalArgumentException(NOT_AVAILIBLE));
        }

        // Use lookup ready item names to lookup all MarketItems
        MarketItemLookupInstance lookupInstance = MarketComponent.getLookupInstance(expandedLookupNames);

        Set<MarketItem> items = new HashSet<>(expandedLookupNames.size());
        for (String itemName : expandedLookupNames) {
            Optional<MarketItem> optItem = lookupInstance.getItemDetails(itemName);
            if (optItem.isEmpty()) {
                return FailedConversion.from(new IllegalArgumentException(NOT_AVAILIBLE));
            }

            items.add(optItem.get());
        }

        return SuccessfulConversion.fromSingle(new MarketItemSet(items));
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return marketItemConverter.getSuggestions(argument, context);
    }
}