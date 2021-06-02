/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player;

import gg.packetloss.grindstone.state.player.attribute.*;

public enum PlayerStateAttribute implements PlayerStateAttributeImpl {
    VITALS(new VitalsAttributeImpl()),
    EXP(new ExperienceAttributeImpl()),
    INVENTORY(new InventoryAttributeImpl()),
    GUILD(new GuildAttributeImpl()),
    GAME_MODE(new GameModeAttributeImpl());

    private final PlayerStateAttributeImpl impl;

    private PlayerStateAttribute(PlayerStateAttributeImpl impl) {
        this.impl = impl;
    }

    @Override
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record) {
        return impl.isValidFor(kind, record);
    }

    @Override
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        return impl.getWorkerFor(kind, persistenceManager);
    }
}
