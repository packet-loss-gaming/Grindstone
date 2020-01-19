package gg.packetloss.grindstone.click;

public class ClickRecord {
    private long[] lastClickOfType = new long[ClickType.values().length];
    private long lastAttack = 0;
    private long lastInteraction = 0;

    private boolean isDoubleClick(long currentClickTime, ClickType clickType) {
        boolean isDoubleClick = currentClickTime - lastClickOfType[clickType.ordinal()] < 500;
        lastClickOfType[clickType.ordinal()] = currentClickTime;
        return isDoubleClick;
    }

    public boolean isDoubleLeftClick() {
        long currentLeftClick = System.currentTimeMillis();

        // Abort if recently punched something
        if (currentLeftClick - lastAttack < 1250) {
            return false;
        }

        return isDoubleClick(currentLeftClick, ClickType.LEFT);
    }

    public boolean isDoubleRightClick() {
        long currentRightClick = System.currentTimeMillis();

        // Abort if recently punched something
        if (currentRightClick - lastInteraction < 1250) {
            return false;
        }

        return isDoubleClick(currentRightClick, ClickType.RIGHT);
    }

    public void recordAttack() {
        lastAttack = System.currentTimeMillis();
    }

    public void recordInteraction() {
        lastInteraction = System.currentTimeMillis();
    }
}
