/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.setting;

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

public class GuildSettingConverter implements ArgumentConverter<GuildSettingUpdate> {
    public static void register(ComponentCommandRegistrar.Registrar registrar) {
        registrar.registerConverter(Key.of(GuildSettingUpdate.class), new GuildSettingConverter());
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("a <setting>=<value> pair");
    }

    @Override
    public ConversionResult<GuildSettingUpdate> convert(String argument, InjectedValueAccess context) {
        String[] parts = argument.split("=");
        if (parts.length != 2) {
            return FailedConversion.from(new IllegalArgumentException("Invalid setting format"));
        }

        return SuccessfulConversion.fromSingle(new GuildSettingUpdate(new DummyGuildSetting(parts[0]), parts[1]));
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return List.of();
    }
}
