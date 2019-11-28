package gg.packetloss.grindstone.state.config;

import gg.packetloss.grindstone.state.PlayerStateAttribute;

import java.util.List;

public class TogglePlayerStateTypeConfigImpl implements PlayerStateTypeConfigImpl {
    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public List<PlayerStateAttribute> getAttributes() {
        return List.of(PlayerStateAttribute.VITALS, PlayerStateAttribute.INVENTORY);
    }
}
