/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

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
import java.util.Optional;
import java.util.stream.Collectors;

public class ScoreTypeConverter implements ArgumentConverter<AnnotatedScoreType> {
    private final HighScoresComponent component;

    public ScoreTypeConverter(HighScoresComponent component) {
        this.component = component;
    }

    public static void register(ComponentCommandRegistrar.Registrar registrar, HighScoresComponent component) {
        registrar.registerConverter(Key.of(AnnotatedScoreType.class), new ScoreTypeConverter(component));
    }

    @Override
    public Component describeAcceptableArguments() {
        return TextComponent.of("any high score table");
    }

    @Override
    public ConversionResult<AnnotatedScoreType> convert(String argument, InjectedValueAccess context) {
        Optional<AnnotatedScoreType> scoreType = component.getScoreTypes().stream().filter(
            (st) -> argument.equalsIgnoreCase(st.getLookupName())
        ).findFirst();
        if (scoreType.isEmpty()) {
            return FailedConversion.from(new IllegalArgumentException("Unknown high score table."));
        }

        return SuccessfulConversion.fromSingle(scoreType.get());
    }

    @Override
    public List<String> getSuggestions(String argument, InjectedValueAccess context) {
        return component.getScoreTypes().stream().map(AnnotatedScoreType::getLookupName).filter(
            (name) -> name.contains(argument.toUpperCase())
        ).collect(Collectors.toList());
    }
}