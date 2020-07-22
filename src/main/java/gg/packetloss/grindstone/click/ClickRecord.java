/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.click;

public class ClickRecord {
    private static final long MILLS_FOR_DOUBLE_CLICK = 500;
    private static final long MILLS_FOR_BUGGED_RIGHT_CLICK = 10;

    // Exposed to allow scheduling past the double click delay
    public static final int TICKS_FOR_DOUBLE_CLICK = (int) MILLS_FOR_DOUBLE_CLICK / 50;

    private long[] lastClickOfType = new long[ClickType.values().length];
    private long lastAttack = 0;
    private long lastInteraction = 0;

    private boolean isDoubleClick(long currentClickTime, ClickType clickType) {
        long clickTimeDiff = currentClickTime - lastClickOfType[clickType.ordinal()];

        // Abort if this looks like a bugged mouse/same instant click
        if (clickTimeDiff < MILLS_FOR_BUGGED_RIGHT_CLICK) {
            return false;
        }

        lastClickOfType[clickType.ordinal()] = currentClickTime;
        return clickTimeDiff < MILLS_FOR_DOUBLE_CLICK;
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
