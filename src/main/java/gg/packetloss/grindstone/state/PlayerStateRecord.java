package gg.packetloss.grindstone.state;

import org.apache.commons.lang.Validate;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerStateRecord {
    private PlayerStateKind tempKind = null;
    private Map<PlayerStateKind, PlayerVitals> vitals = new EnumMap<>(PlayerStateKind.class);
    private Map<PlayerStateKind, PlayerExperience> experience = new EnumMap<>(PlayerStateKind.class);
    private Map<PlayerStateKind, UUID> inventories = new EnumMap<>(PlayerStateKind.class);
    private Map<PlayerStateKind, PlayerGuild> guilds = new EnumMap<>(PlayerStateKind.class);

    private void validatePush(PlayerStateKind kind) throws ConflictingPlayerStateException {
        Validate.notNull(kind, "New kind must not be null");

        boolean hasTempKind = tempKind != null;
        if (hasTempKind) {
            if (kind.isTemporary()) {
                throw new ConflictingPlayerStateException(kind, tempKind);
            }

            if (!kind.allowUseWithTemporaryState()) {
                throw new ConflictingPlayerStateException(kind, tempKind);
            }
        }
    }

    private void doPush(PlayerStateKind kind) {
        if (kind.isTemporary()) {
            tempKind = kind;
        }
    }

    public void pushKind(PlayerStateKind kind) throws ConflictingPlayerStateException {
        validatePush(kind);
        doPush(kind);
    }

    public void popKind(PlayerStateKind kind) {
        if (kind.isTemporary()) {
            this.tempKind = null;
        }
    }

    public Optional<PlayerStateKind> getTempKind() {
        return Optional.ofNullable(tempKind);
    }

    public Map<PlayerStateKind, PlayerVitals> getVitals() {
        return vitals;
    }

    public Map<PlayerStateKind, PlayerExperience> getExperience() {
        return experience;
    }

    public Map<PlayerStateKind, UUID> getInventories() {
        return inventories;
    }

    public Map<PlayerStateKind, PlayerGuild> getGuilds() {
        return guilds;
    }

    public boolean isEmpty() {
        return vitals.isEmpty() && experience.isEmpty() && inventories.isEmpty() && guilds.isEmpty();
    }
}
