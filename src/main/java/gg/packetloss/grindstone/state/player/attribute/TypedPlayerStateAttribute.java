/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player.attribute;

import gg.packetloss.grindstone.state.player.PlayerStateAttribute;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.state.player.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.player.PlayerStateRecord;

public class TypedPlayerStateAttribute {
    private PlayerStateKind kind;
    private PlayerStateAttribute attribute;

    public TypedPlayerStateAttribute(PlayerStateKind kind, PlayerStateAttribute attribute) {
        this.kind = kind;
        this.attribute = attribute;
    }

    public boolean isValidFor(PlayerStateRecord record) {
        return attribute.isValidFor(kind, record);
    }

    public AttributeWorker getWorkerFor(PlayerStatePersistenceManager persistenceManager) {
        return attribute.getWorkerFor(kind, persistenceManager);
    }
}
