package gg.packetloss.grindstone.state.player.config;

import gg.packetloss.grindstone.state.player.PlayerStateAttribute;

import java.util.List;

public interface PlayerStateTypeConfigImpl {
    public boolean isTemporary();
    public boolean allowUseWithTemporaryState();

    public boolean shouldSwapOnDuplicate();

    default public List<PlayerStateAttribute> getAttributes() {
        return List.of(
                PlayerStateAttribute.VITALS,
                PlayerStateAttribute.EXP,
                PlayerStateAttribute.INVENTORY,
                PlayerStateAttribute.GUILD
        );
    }
}
