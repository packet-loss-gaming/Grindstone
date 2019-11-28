package gg.packetloss.grindstone.state;

public class InvalidTempPlayerStateException extends Exception {
    private PlayerStateKind existingKind;

    public InvalidTempPlayerStateException(PlayerStateKind existingKind) {
        this.existingKind = existingKind;
    }

    public PlayerStateKind getConflictingKind() {
        return existingKind;
    }
}
