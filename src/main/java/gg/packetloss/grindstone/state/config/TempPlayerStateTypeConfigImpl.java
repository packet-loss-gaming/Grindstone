package gg.packetloss.grindstone.state.config;

import gg.packetloss.grindstone.state.PlayerStateAttribute;

import java.util.List;

public class TempPlayerStateTypeConfigImpl implements PlayerStateTypeConfigImpl {
    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public List<PlayerStateAttribute> getAttributes() {
        return List.of(PlayerStateAttribute.VITALS, PlayerStateAttribute.INVENTORY);
    }
}
