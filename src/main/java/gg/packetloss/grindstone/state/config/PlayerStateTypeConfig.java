package gg.packetloss.grindstone.state.config;

import gg.packetloss.grindstone.state.attribute.TypedPlayerStateAttribute;

import java.util.List;

public interface PlayerStateTypeConfig {
    public boolean isTemporary();

    public List<TypedPlayerStateAttribute> getAttributes();
}
