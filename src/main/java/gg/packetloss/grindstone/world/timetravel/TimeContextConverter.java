package gg.packetloss.grindstone.world.timetravel;

import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import gg.packetloss.grindstone.world.managed.ManagedWorldTimeContext;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TimeContextConverter implements ArgumentConverter<ManagedWorldTimeContext> {
    public static void register(ComponentCommandRegistrar registrar) {
        registrar.registerConverter(Key.of(ManagedWorldTimeContext.class), new TimeContextConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("world version to target");
    }

    @Override
    public ConversionResult<ManagedWorldTimeContext> convert(String argument, InjectedValueAccess context) {
        try {
            return SuccessfulConversion.fromSingle(ManagedWorldTimeContext.valueOf(argument.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return FailedConversion.from(new IllegalArgumentException("No such world version exists!"));

        }
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return Arrays.stream(ManagedWorldTimeContext.values())
                .map(Enum::name)
                .filter(s -> s.contains(argument.toUpperCase()))
                .collect(Collectors.toList());
    }
}
