/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player.attribute;

import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.state.player.PlayerGuild;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.state.player.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.player.PlayerStateRecord;
import org.bukkit.entity.Player;

public class GuildAttributeImpl implements PlayerStateAttributeImpl {
    @Override
    public boolean isValidFor(PlayerStateKind kind, PlayerStateRecord record) {
        return record.getGuilds().get(kind) != null;
    }

    @Override
    public AttributeWorker getWorkerFor(PlayerStateKind kind, PlayerStatePersistenceManager persistenceManager) {
        return new GuildAttributeWorker(this, kind, persistenceManager);
    }

    private static class GuildAttributeWorker extends AttributeWorker<PlayerGuild> {
        protected GuildAttributeWorker(PlayerStateAttributeImpl attribute, PlayerStateKind kind,
                                           PlayerStatePersistenceManager persistenceManager) {
            super(attribute, kind, persistenceManager);
        }

        @Override
        public void attach(PlayerStateRecord record, Player player) {
            GuildComponent.inst().getState(player).ifPresent((guildState) -> {
                record.getGuilds().put(kind, new PlayerGuild(guildState.getType(), guildState.isEnabled()));
            });
        }

        @Override
        protected PlayerGuild detach(PlayerStateRecord record, Player player) {
            return record.getGuilds().remove(kind);
        }

        @Override
        protected void remove(PlayerGuild playerGuild, Player player) {
            GuildComponent.inst().getState(player).ifPresent((guildState -> {
                if (playerGuild.arePowersEnabled()) {
                    guildState.enablePowers();
                } else {
                    guildState.disablePowers();
                }
            }));
        }
    }
}
