/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.invite.db;

import java.util.Optional;
import java.util.UUID;

public interface PlayerInviteDatabase {
    InviteResult addInvite(UUID existingPlayer, UUID newPlayer);
    Optional<UUID> getInvitor(UUID newPlayer);
}
