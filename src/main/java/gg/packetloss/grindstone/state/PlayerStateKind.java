package gg.packetloss.grindstone.state;

import gg.packetloss.grindstone.state.attribute.TypedPlayerStateAttribute;
import gg.packetloss.grindstone.state.config.*;

import java.util.List;
import java.util.stream.Collectors;

public enum PlayerStateKind implements PlayerStateTypeConfig {
    ADMIN(new TogglePlayerStateTypeConfigImpl()),
    LEGIT(new SwapPlayerStateTypeConfigImpl()),
    FREAKY_FOUR(new BossSafeRespawnPlayerStateTypeConfigImpl()),
    SHNUGGLES_PRIME(new BossSafeRespawnPlayerStateTypeConfigImpl()),
    GRAVE_YARD(new BossSafeRespawnPlayerStateTypeConfigImpl()),
    PATIENT_X(new BossSafeRespawnPlayerStateTypeConfigImpl()),
    MIRAGE_ARENA(new WalkInPvPSafeRespawnPlayerStateTypeConfigImpl()),
    SAND_ARENA(new WalkInPvPSafeRespawnPlayerStateTypeConfigImpl()),
    JUNGLE_RAID(new TempPlayerStateTypeConfigImpl()),
    SPLEEF(new TempPlayerStateTypeConfigImpl());

    private final PlayerStateTypeConfigImpl config;

    private PlayerStateKind(PlayerStateTypeConfigImpl config) {
        this.config = config;
    }

    public boolean isTemporary() {
        return config.isTemporary();
    }

    @Override
    public boolean shouldSwapOnDuplicate() {
        return config.shouldSwapOnDuplicate();
    }

    @Override
    public boolean allowUseWithTemporaryState() {
        return config.allowUseWithTemporaryState();
    }

    public List<TypedPlayerStateAttribute> getAttributes() {
        return config.getAttributes().stream()
                .map(a -> new TypedPlayerStateAttribute(this, a))
                .collect(Collectors.toList());
    }
}
