/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.macro;

import java.util.Set;
import java.util.regex.Pattern;

public abstract class AbstractMacro {
    protected final Pattern pattern;

    public AbstractMacro(String macroRegex) {
        pattern = Pattern.compile(macroRegex, Pattern.CASE_INSENSITIVE);
    }

    public boolean matches(String input) {
        return pattern.matcher(input).find();
    }

    public abstract Set<String> expand(String input);
}
