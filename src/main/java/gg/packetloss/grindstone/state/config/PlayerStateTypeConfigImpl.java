package gg.packetloss.grindstone.state.config;

import gg.packetloss.grindstone.state.PlayerStateAttribute;

import java.util.List;

public interface PlayerStateTypeConfigImpl {
    public boolean isTemporary();
    public boolean allowUseWithTemporaryState();

    public boolean shouldSwapOnDuplicate();

    default public List<PlayerStateAttribute> getAttributes() {
        return List.of(PlayerStateAttribute.VITALS, PlayerStateAttribute.INVENTORY);
    }
}
