package gg.packetloss.grindstone.state.player;

import gg.packetloss.grindstone.state.player.attribute.TypedPlayerStateAttribute;
import gg.packetloss.grindstone.state.player.config.*;

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
    PUZZLE_PRISON(new WalkInNoGuildPlayerStateTypeConfigImpl()),
    GOLD_RUSH(new WalkInNoGuildPlayerStateTypeConfigImpl()),
    JUNGLE_RAID(new TempPlayerStateTypeConfigImpl()),
    SKY_WARS(new TempPlayerStateTypeConfigImpl()),
    SPLEEF(new TempPlayerStateTypeConfigImpl()),

    FREAKY_FOUR_SPECTATOR(new SpectatorPlayerStateTypeConfigImpl()),
    FROSTBORN_SPECTATOR(new SpectatorPlayerStateTypeConfigImpl()),
    SHNUGGLES_PRIME_SPECTATOR(new SpectatorPlayerStateTypeConfigImpl()),
    GRAVE_YARD_SPECTATOR(new SpectatorPlayerStateTypeConfigImpl()),
    PATIENT_X_SPECTATOR(new SpectatorPlayerStateTypeConfigImpl()),
    JUNGLE_RAID_SPECTATOR(new SpectatorPlayerStateTypeConfigImpl()),
    SKY_WARS_SPECTATOR(new SpectatorPlayerStateTypeConfigImpl());

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
