/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.macro;

import java.util.List;
import java.util.Set;

public class MacroExpander {
    private List<AbstractMacro> macros;

    public MacroExpander(List<AbstractMacro> macros) {
        this.macros = macros;
    }

    public Set<String> expand(String input) {
        for (AbstractMacro macro : macros) {
            if (macro.matches(input)) {
                return macro.expand(input);
            }
        }
        return Set.of(input);
    }
}
