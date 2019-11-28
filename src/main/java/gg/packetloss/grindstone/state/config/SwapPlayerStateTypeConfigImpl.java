package gg.packetloss.grindstone.state.config;

public class SwapPlayerStateTypeConfigImpl implements PlayerStateTypeConfigImpl {
    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public boolean allowUseWithTemporaryState() {
        return false;
    }

    @Override
    public boolean shouldSwapOnDuplicate() {
        return true;
    }
}
