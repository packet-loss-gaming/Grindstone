/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.parser;

import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class HelpTextParser {
    private ChatColor primaryMessageColor;

    public HelpTextParser(ChatColor primaryMessageColor) {
        this.primaryMessageColor = primaryMessageColor;
    }

    private void finishTerm(ParseState state) {
        String builtString = state.builder.toString();

        if (state.insideCommand) {
            state.objects.add(Text.of(ChatColor.BLUE, builtString, TextAction.Click.suggestCommand(builtString)));
        } else {
            state.objects.add(Text.of(primaryMessageColor, builtString));
        }

        state.insideCommand = !state.insideCommand;
        state.builder.setLength(0);
    }

    public Text parse(String input) {
        ParseState state = new ParseState();

        for (int i = 0; i < input.length(); ++i) {
            char curChar = input.charAt(i);
            if (curChar == '`') {
                finishTerm(state);
                continue;
            }

            state.builder.append(curChar);
        }

        finishTerm(state);

        return Text.of(state.objects);
    }

    private static class ParseState {
        public List<Text> objects = new ArrayList<>();
        public StringBuilder builder = new StringBuilder();

        public boolean insideCommand = false;
    };
}
