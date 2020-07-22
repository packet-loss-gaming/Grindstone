/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player.attribute;

import gg.packetloss.grindstone.state.player.PlayerExperience;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.state.player.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.player.PlayerStateRecord;
import org.bukkit.entity.Player;

public class ExperienceAttributeImpl implements PlayerStateAttributeImpl {
    @Override
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record) {
        return record.getExperience().get(kind) != null;
    }

    @Override
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        return new ExperienceAttributeWorker(this, kind, persistenceManager);
    }

    private static class ExperienceAttributeWorker extends AttributeWorker<PlayerExperience> {
        protected ExperienceAttributeWorker(PlayerStateAttributeImpl attribute, PlayerStateKind kind,
                                            PlayerStatePersistenceManager persistenceManager) {
            super(attribute, kind, persistenceManager);
        }

        @Override
        public void attach(PlayerStateRecord record, Player player) {
            record.getExperience().put(kind, new PlayerExperience(player.getExp(), player.getLevel()));
        }

        @Override
        protected PlayerExperience detach(PlayerStateRecord record, Player player) {
            return record.getExperience().remove(kind);
        }

        @Override
        protected void remove(PlayerExperience oldState, Player player) {
            player.setExp(oldState.getExp());
            player.setLevel(oldState.getLevel());
        }
    }
}
