package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomItemsConverter implements ArgumentConverter<CustomItemBundle> {
    public static void register(ComponentCommandRegistrar registrar) {
        registrar.registerConverter(Key.of(CustomItemBundle.class), new CustomItemsConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any custom item");
    }

    private static CustomItems resolveName(String itemName) {
        try {
            int itemId = Integer.parseInt(itemName);
            return CustomItems.values()[itemId];
        } catch (NumberFormatException ex) {
            return CustomItems.valueOf(itemName.toUpperCase());
        }
    }

    @Override
    public ConversionResult<CustomItemBundle> convert(String argument, InjectedValueAccess context) {
        try {
            // Map all to verify via exceptions first
            List<CustomItems> items = ItemNameCalculator.expandNameMacros(argument).stream()
                    .map(CustomItemsConverter::resolveName)
                    .collect(Collectors.toList());

            return SuccessfulConversion.fromSingle(new CustomItemBundle(items));
        } catch (ArrayIndexOutOfBoundsException|IllegalArgumentException ex) {
            return FailedConversion.from(ex);
        }
    }

    @Override
    public List<String> getSuggestions(String input) {
        return Stream.of(CustomItems.values())
                .map(CustomItems::name)
                .filter((s) -> s.contains(input.toUpperCase()))
                .collect(Collectors.toList());
    }
}
