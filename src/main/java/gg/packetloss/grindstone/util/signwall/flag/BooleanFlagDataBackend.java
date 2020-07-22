/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.signwall.flag;

import gg.packetloss.grindstone.util.flag.BooleanFlag;
import gg.packetloss.grindstone.util.flag.BooleanFlagState;
import gg.packetloss.grindstone.util.signwall.SignWallDataBackend;

public class BooleanFlagDataBackend<T extends Enum<T> & BooleanFlag> implements SignWallDataBackend<BooleanFlagState<T>> {
    private T[] enumConstants;
    private boolean[] state;

    public BooleanFlagDataBackend(Class<T> flagEnum) {
        enumConstants = flagEnum.getEnumConstants();
        state = new boolean[enumConstants.length];

        for (int i = 0; i < enumConstants.length; ++i) {
            state[i] = enumConstants[i].isEnabledByDefault();
        }
    }

    public boolean isEnabled(T flag) {
        return state[flag.ordinal()];
    }

    @Override
    public BooleanFlagState<T> get(int index) {
        return new BooleanFlagState<>(enumConstants[index], state[index]);
    }

    @Override
    public void set(int index, BooleanFlagState<T> value) {
        state[index] = value.isEnabled();
    }

    @Override
    public int size() {
        return state.length;
    }
}
