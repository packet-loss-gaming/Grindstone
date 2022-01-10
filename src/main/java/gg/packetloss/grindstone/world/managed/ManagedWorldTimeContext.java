/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.managed;

public enum ManagedWorldTimeContext {
    V_1_15,
    LATEST_ARCHIVED, /* Add archive worlds above this point. */
    V_1_18,
    LATEST; /* Add the non-archieve world above this point. */

    public static ManagedWorldTimeContext getLatest() {
        return ManagedWorldTimeContext.values()[ManagedWorldTimeContext.LATEST.ordinal() - 1];
    }

    public static ManagedWorldTimeContext getLatestArchive() {
        return ManagedWorldTimeContext.values()[ManagedWorldTimeContext.LATEST_ARCHIVED.ordinal() - 1];
    }
}
