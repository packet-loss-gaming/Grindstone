/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.pixieitems;

import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.ConversionResult;
import org.enginehub.piston.converter.FailedConversion;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueAccess;
import org.enginehub.piston.inject.Key;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PixieSinkCreationModeConverter implements ArgumentConverter<PixieSinkCreationMode> {
    public static void register(ComponentCommandRegistrar.Registrar registrar) {
        registrar.registerConverter(Key.of(PixieSinkCreationMode.class), new PixieSinkCreationModeConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any pixie sink creation mode");
    }

    @Override
    public ConversionResult<PixieSinkCreationMode> convert(String argument, InjectedValueAccess context) {
        try {
            return SuccessfulConversion.fromSingle(PixieSinkCreationMode.valueOf(argument.toUpperCase()));
        } catch (ArrayIndexOutOfBoundsException|IllegalArgumentException ex) {
            return FailedConversion.from(ex);
        }
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return Stream.of(PixieSinkCreationMode.values())
                .map(PixieSinkCreationMode::name)
                .filter((s) -> s.contains(argument.toUpperCase()))
                .collect(Collectors.toList());
    }
}
