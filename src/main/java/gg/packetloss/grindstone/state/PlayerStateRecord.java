package gg.packetloss.grindstone.state;

import org.apache.commons.lang.Validate;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerStateRecord {
    private PlayerStateKind tempKind = null;
    private Map<PlayerStateKind, PlayerVitals> vitals = new EnumMap<>(PlayerStateKind.class);
    private Map<PlayerStateKind, UUID> inventories = new EnumMap<>(PlayerStateKind.class);

    public Optional<PlayerStateKind> getTempKind() {
        return Optional.ofNullable(tempKind);
    }

    public void pushTempKind(PlayerStateKind tempKind) throws InvalidTempPlayerStateException {
        if (this.tempKind != null) {
            throw new InvalidTempPlayerStateException(this.tempKind);
        }

        Validate.notNull(tempKind, "New temp kind must not be null");
        this.tempKind = tempKind;
    }

    public void clearTempKind() {
        Validate.notNull(this.tempKind, "No existing temp kind to clear");
        this.tempKind = null;
    }

    public Map<PlayerStateKind, PlayerVitals> getVitals() {
        return vitals;
    }

    public Map<PlayerStateKind, UUID> getInventories() {
        return inventories;
    }

    public boolean isEmpty() {
        return vitals.isEmpty() && inventories.isEmpty();
    }
}
