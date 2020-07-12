package gg.packetloss.grindstone.jail;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.List;
import java.util.stream.Collectors;

public class PrisonIdentifierConverter implements ArgumentConverter<PrisonIdentifier> {
    private JailComponent component;

    public PrisonIdentifierConverter(JailComponent component) {
        this.component = component;
    }

    public static void register(CommandManager commandManager, JailComponent component) {
        commandManager.registerConverter(Key.of(PrisonIdentifier.class), new PrisonIdentifierConverter(component));
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any warp");
    }

    private List<String> getPrisons() {
        return component.getJailCellDatabase().getPrisons();
    }

    @Override
    public ConversionResult<PrisonIdentifier> convert(String argument, InjectedValueAccess context) {
        for (String prison : getPrisons()) {
            if (argument.equalsIgnoreCase(prison)) {
                return SuccessfulConversion.fromSingle(new PrisonIdentifier(prison));
            }
        }

        return FailedConversion.from(new IllegalArgumentException("Invalid prison name"));
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return getPrisons().stream()
                .map(String::toUpperCase)
                .filter((s) -> s.contains(argument.toUpperCase()))
                .collect(Collectors.toList());
    }
}
