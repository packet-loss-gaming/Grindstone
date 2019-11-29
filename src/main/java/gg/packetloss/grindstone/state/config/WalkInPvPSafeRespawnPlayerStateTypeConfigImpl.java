package gg.packetloss.grindstone.state.config;

import gg.packetloss.grindstone.state.PlayerStateAttribute;

import java.util.List;

public class WalkInPvPSafeRespawnPlayerStateTypeConfigImpl extends TempPlayerStateTypeConfigImpl {
    @Override
    public List<PlayerStateAttribute> getAttributes() {
        return List.of(PlayerStateAttribute.INVENTORY, PlayerStateAttribute.EXP);
    }
}
