package gg.packetloss.grindstone.warps;

import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WarpPointConverter implements ArgumentConverter<WarpPoint> {
    private WarpsComponent component;

    public WarpPointConverter(WarpsComponent component) {
        this.component = component;
    }

    public static void register(ComponentCommandRegistrar registrar, WarpsComponent component) {
        registrar.registerConverter(Key.of(WarpPoint.class), new WarpPointConverter(component));
    }

    public static void register(CommandManager commandManager, WarpsComponent component) {
        commandManager.registerConverter(Key.of(WarpPoint.class), new WarpPointConverter(component));
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any warp");
    }

    @Override
    public ConversionResult<WarpPoint> convert(String argument, InjectedValueAccess context) {
        Optional<CommandSender> optSender = context.injectedValue(Key.of(CommandSender.class));
        if (optSender.isEmpty()){
            return FailedConversion.from(new IllegalStateException("No command sender present"));
        }

        CommandSender sender = optSender.get();

        String[] parts = argument.split(":");

        Optional<WarpPoint> optWarp;
        if (parts.length == 1) {
            try {
                optWarp = component.getWarpManager().lookupWarp(PlayerUtil.checkPlayer(sender), parts[0]);
            } catch (CommandException e) {
                return FailedConversion.from(new IllegalArgumentException(e.getMessage()));
            }
        } else if (parts.length == 2) {
            optWarp = component.getWarpManager().lookupWarp(parts[0], parts[1]);
        } else {
            return FailedConversion.from(new IllegalArgumentException("Invalid warp format"));
        }

        if (optWarp.isPresent()) {
            return SuccessfulConversion.fromSingle(optWarp.get());
        } else {
            return FailedConversion.from(new WarpNotFoundException());
        }
    }

    @Override
    public List<String> getSuggestions(String input) {
        return component.getWarpManager().getGlobalWarps().stream()
                .map(WarpPoint::getQualifiedName)
                .map(WarpQualifiedName::getDisplayName)
                .filter((s) -> s.contains(input.toUpperCase()))
                .collect(Collectors.toList());
    }
}
