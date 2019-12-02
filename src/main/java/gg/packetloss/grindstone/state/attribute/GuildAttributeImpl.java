package gg.packetloss.grindstone.state.attribute;

import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.state.PlayerGuild;
import gg.packetloss.grindstone.state.PlayerStateKind;
import gg.packetloss.grindstone.state.PlayerStatePersistenceManager;
import gg.packetloss.grindstone.state.PlayerStateRecord;
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
