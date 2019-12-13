package gg.packetloss.grindstone.state.player.config;

import gg.packetloss.grindstone.state.player.PlayerStateAttribute;

import java.util.List;

public class WalkInNoGuildPlayerStateTypeConfigImpl extends TempPlayerStateTypeConfigImpl {
    @Override
    public List<PlayerStateAttribute> getAttributes() {
        return List.of(PlayerStateAttribute.GUILD);
    }
}
