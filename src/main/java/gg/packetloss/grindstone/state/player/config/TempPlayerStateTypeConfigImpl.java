package gg.packetloss.grindstone.state.player.config;

public class TempPlayerStateTypeConfigImpl implements PlayerStateTypeConfigImpl {
    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public boolean allowUseWithTemporaryState() {
        return false;
    }

    @Override
    public boolean shouldSwapOnDuplicate() {
        return false;
    }
}
