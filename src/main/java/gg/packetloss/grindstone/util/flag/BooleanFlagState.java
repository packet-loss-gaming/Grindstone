package gg.packetloss.grindstone.util.flag;

public class BooleanFlagState<T extends Enum<T>> {
    private final T flag;
    private boolean isEnabled;

    public BooleanFlagState(T flag, boolean isEnabledDefault) {
        this.flag = flag;
        this.isEnabled = isEnabledDefault;
    }

    public T getFlag() {
        return flag;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void toggle() {
        isEnabled = !isEnabled;
    }
}
