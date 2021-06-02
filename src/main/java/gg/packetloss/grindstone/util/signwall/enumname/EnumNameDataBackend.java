/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.signwall.enumname;

import gg.packetloss.grindstone.util.signwall.SignWallDataBackend;
import org.apache.commons.lang.Validate;

public class EnumNameDataBackend<T extends Enum<T>> implements SignWallDataBackend<T> {
    private final T[] enumValues;

    public EnumNameDataBackend(Class<T> enumClass) {
        this.enumValues = enumClass.getEnumConstants();
    }

    @Override
    public T get(int index) {
        return enumValues[index];
    }

    @Override
    public void set(int index, T value) {
        Validate.isTrue(enumValues[index] == value);
    }

    @Override
    public int size() {
        return enumValues.length;
    }
}
