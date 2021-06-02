/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.jail;

public class PrisonIdentifier {
    private final String name;

    public PrisonIdentifier(String name) {
        this.name = name;
    }

    public String get() {
        return name;
    }
}
