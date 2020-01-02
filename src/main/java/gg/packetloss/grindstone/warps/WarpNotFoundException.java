package gg.packetloss.grindstone.warps;

public class WarpNotFoundException extends IllegalArgumentException {
    public WarpNotFoundException() {
        super("No warp could be found by that name!");
    }
}
