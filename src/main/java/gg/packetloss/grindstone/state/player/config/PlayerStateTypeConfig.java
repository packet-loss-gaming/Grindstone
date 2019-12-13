package gg.packetloss.grindstone.state.player.config;

import gg.packetloss.grindstone.state.player.attribute.TypedPlayerStateAttribute;

import java.util.List;

public interface PlayerStateTypeConfig {
    public boolean isTemporary();
    public boolean shouldSwapOnDuplicate();
    public boolean allowUseWithTemporaryState();

    public List<TypedPlayerStateAttribute> getAttributes();
}
