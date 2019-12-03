package gg.packetloss.grindstone.state.config;

import gg.packetloss.grindstone.state.PlayerStateAttribute;

import java.util.List;

public class WalkInNoGuildPlayerStateTypeConfigImpl extends TempPlayerStateTypeConfigImpl {
    @Override
    public List<PlayerStateAttribute> getAttributes() {
        return List.of(PlayerStateAttribute.GUILD);
    }
}
