/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.flag;

public class BooleanFlagState<T extends Enum<T>> {
    private final T flag;
    private boolean isEnabled;

    public BooleanFlagState(T flag, boolean isEnabled) {
        this.flag = flag;
        this.isEnabled = isEnabled;
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
