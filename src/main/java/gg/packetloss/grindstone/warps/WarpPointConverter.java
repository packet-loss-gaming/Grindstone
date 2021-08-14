/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WarpPointConverter implements ArgumentConverter<WarpPoint> {
    private WarpsComponent component;

    public WarpPointConverter(WarpsComponent component) {
        this.component = component;
    }

    public static void register(ComponentCommandRegistrar.Registrar registrar, WarpsComponent component) {
        registrar.registerConverter(Key.of(WarpPoint.class), new WarpPointConverter(component));
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
            optWarp = component.getWarpManager().lookupWarp(sender, parts[0], parts[1]);
        } else {
            return FailedConversion.from(new IllegalArgumentException("Invalid warp format"));
        }

        if (optWarp.isPresent()) {
            return SuccessfulConversion.fromSingle(optWarp.get());
        } else {
            return FailedConversion.from(new WarpNotFoundException());
        }
    }

    private CommandSender getSender(InjectedValueAccess context) {
        return context.injectedValue(Key.of(CommandSender.class)).orElse(Bukkit.getConsoleSender());
    }

    private Stream<WarpPoint> getWarpPointStream(CommandSender sender, UUID namespaceOverride) {
        if (namespaceOverride != null) {
            return component.getWarpManager().getWarpsForNamespace(namespaceOverride).stream();
        } else if (sender instanceof Player) {
            return component.getWarpManager().getWarpsForPlayer((Player) sender).stream();
        } else {
            return component.getWarpManager().getGlobalWarps().stream();
        }
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        CommandSender sender = getSender(context);

        if (argument.startsWith("#") && !argument.contains(":")) {
            String nameFilter = argument.substring(1).toUpperCase();
            return Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                    .filter(p -> p.getName() != null)
                    .filter(p -> p.getName().toUpperCase().contains(nameFilter))
                    .filter(p -> WarpPermissionCheck.hasAccessToNamespace(sender, p.getUniqueId()))
                    .map(p -> "#" + p.getName() + ":")
                    .collect(Collectors.toList());
        }

        boolean endsWithColon = argument.endsWith(":");
        String[] parts = endsWithColon ? new String[] { argument.substring(0, argument.length() - 1), "" }
                                       : argument.split(":");

        UUID namespaceOverride = null;
        String filterText = argument;

        if (parts.length == 2) {
            UUID namespace = GeneralPlayerUtil.resolveMacroNamespace(sender, parts[0]);
            if (namespace == null) {
                return new ArrayList<>();
            }

            if (!WarpPermissionCheck.hasAccessToNamespace(sender, namespace)) {
                return new ArrayList<>();
            }

            namespaceOverride = namespace;
            filterText = parts[1];
        }

        String finalFilterText = filterText.toUpperCase();
        return getWarpPointStream(sender, namespaceOverride)
                .map(WarpPoint::getQualifiedName)
                .map(WarpQualifiedName::getDisplayName)
                .filter((s) -> s.contains(finalFilterText))
                .map(name -> parts.length == 1 ? name : parts[0] + ":" + name)
                .collect(Collectors.toList());
    }
}
