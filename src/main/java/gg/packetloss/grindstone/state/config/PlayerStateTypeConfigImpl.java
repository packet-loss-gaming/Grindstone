package gg.packetloss.grindstone.state.config;

import gg.packetloss.grindstone.state.PlayerStateAttribute;

import java.util.List;

public interface PlayerStateTypeConfigImpl {
    public boolean isTemporary();

    public List<PlayerStateAttribute> getAttributes();
}
