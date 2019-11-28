package gg.packetloss.grindstone.state;

public class ConflictingPlayerStateException extends Exception {
    private final PlayerStateKind newKind;
    private final PlayerStateKind existingKind;

    public ConflictingPlayerStateException(PlayerStateKind newKind, PlayerStateKind existingKind) {
        super("Attempted to apply player state: " + newKind +
              " but conflicting state: " + existingKind + " was present");
        this.newKind = newKind;
        this.existingKind = existingKind;
    }

    public PlayerStateKind getNewKind() {
        return newKind;
    }

    public PlayerStateKind getExistingKind() {
        return existingKind;
    }
}