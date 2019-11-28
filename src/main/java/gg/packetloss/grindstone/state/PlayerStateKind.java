package gg.packetloss.grindstone.state;

import gg.packetloss.grindstone.state.attribute.TypedPlayerStateAttribute;
import gg.packetloss.grindstone.state.config.PlayerStateTypeConfig;
import gg.packetloss.grindstone.state.config.PlayerStateTypeConfigImpl;
import gg.packetloss.grindstone.state.config.TempPlayerStateTypeConfigImpl;
import gg.packetloss.grindstone.state.config.TogglePlayerStateTypeConfigImpl;

import java.util.List;
import java.util.stream.Collectors;

public enum PlayerStateKind implements PlayerStateTypeConfig {
    ADMIN(new TogglePlayerStateTypeConfigImpl()),
    SPLEEF(new TempPlayerStateTypeConfigImpl());

    private final PlayerStateTypeConfigImpl config;

    private PlayerStateKind(PlayerStateTypeConfigImpl config) {
        this.config = config;
    }

    public boolean isTemporary() {
        return config.isTemporary();
    }

    public List<TypedPlayerStateAttribute> getAttributes() {
        return config.getAttributes().stream()
                .map(a -> new TypedPlayerStateAttribute(this, a))
                .collect(Collectors.toList());
    }
}
