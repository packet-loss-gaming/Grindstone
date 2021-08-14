/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.store.command;

import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.economic.store.MarketItem;
import gg.packetloss.grindstone.economic.store.MarketItemLookupInstance;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static gg.packetloss.grindstone.economic.store.MarketComponent.NOT_AVAILIBLE;

public class MarketItemConverter implements ArgumentConverter<MarketItem> {
    private BasicMarketItemConverter marketItemConverter;

    public MarketItemConverter(MarketComponent component) {
        this.marketItemConverter = new BasicMarketItemConverter(component);
    }

    public static void register(ComponentCommandRegistrar.Registrar registrar, MarketComponent component) {
        registrar.registerConverter(Key.of(MarketItem.class), new MarketItemConverter(component));
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any item");
    }

    @Override
    public ConversionResult<MarketItem> convert(String argument, InjectedValueAccess context) {
        Optional<String> optItemName = ItemNameCalculator.matchItem(argument);
        if (optItemName.isEmpty()) {
            return FailedConversion.from(new IllegalArgumentException(NOT_AVAILIBLE));
        }

        String itemName = optItemName.get();
        MarketItemLookupInstance lookupInstance = MarketComponent.getLookupInstance(Set.of(itemName));

        Optional<MarketItem> optItem = lookupInstance.getItemDetails(itemName);
        if (optItem.isEmpty()) {
            return FailedConversion.from(new IllegalArgumentException(NOT_AVAILIBLE));
        }

        return SuccessfulConversion.fromSingle(optItem.get());
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return marketItemConverter.getSuggestions(argument, context);
    }
}