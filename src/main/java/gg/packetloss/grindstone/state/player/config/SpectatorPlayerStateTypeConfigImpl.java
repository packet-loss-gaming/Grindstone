package gg.packetloss.grindstone.state.player.config;

import gg.packetloss.grindstone.state.player.PlayerStateAttribute;

import java.util.List;

public class SpectatorPlayerStateTypeConfigImpl extends TempPlayerStateTypeConfigImpl {
    @Override
    public List<PlayerStateAttribute> getAttributes() {
        return List.of(
                PlayerStateAttribute.VITALS,
                PlayerStateAttribute.EXP,
                PlayerStateAttribute.INVENTORY,
                PlayerStateAttribute.GUILD,
                PlayerStateAttribute.GAME_MODE
        );
    }

}
