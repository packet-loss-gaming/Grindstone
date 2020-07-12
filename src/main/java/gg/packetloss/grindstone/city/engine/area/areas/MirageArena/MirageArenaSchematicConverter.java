package gg.packetloss.grindstone.city.engine.area.areas.MirageArena;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;


public class MirageArenaSchematicConverter implements ArgumentConverter<MirageArenaSchematic> {
    private MirageArena component;

    public MirageArenaSchematicConverter(MirageArena component) {
        this.component = component;
    }

    public static void register(CommandManager commandManager, MirageArena component) {
        commandManager.registerConverter(Key.of(MirageArenaSchematic.class), new MirageArenaSchematicConverter(component));
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any item");
    }

    @Override
    public ConversionResult<MirageArenaSchematic> convert(String argument, InjectedValueAccess context) {
        Path file = component.getFile(argument.toLowerCase());

        if (!Files.exists(file)) {
            return FailedConversion.from(new IllegalArgumentException("No arena by that name found."));
        }

        return SuccessfulConversion.fromSingle(new MirageArenaSchematic(file));
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return component.getArenas(argument).stream()
                .map(MirageArenaSchematic::getArenaName)
                .collect(Collectors.toList());
    }
}