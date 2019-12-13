package gg.packetloss.grindstone.state.player.config;

public class TogglePlayerStateTypeConfigImpl implements PlayerStateTypeConfigImpl {
    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public boolean allowUseWithTemporaryState() {
        return true;
    }

    @Override
    public boolean shouldSwapOnDuplicate() {
        return false;
    }
}
