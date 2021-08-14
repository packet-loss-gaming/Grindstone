/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel.db;

import java.util.Optional;
import java.util.UUID;

public interface PlayerWorldLevelDatabase {
    Optional<Integer> loadWorldLevel(UUID playerID);
    void updateWorldLevel(UUID playerID, int newLevel);
}
