/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;


import java.util.Optional;

public enum District {

    GLOBAL("City"),
    CARPE_DIEM("Carpe Diem"),
    GLACIES_MARE("Glacies Mare"),
    OBLITUS("Oblitus"),
    VINEAM("Vineam");

    private final String properName;

    District(String properName) {
        this.properName = properName;
    }

    public String toProperName() {
        return properName;
    }

    public static Optional<District> fromName(String name) {
        try {
            return Optional.of(valueOf(name.replace(' ', '_').replace('-', '_').toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
