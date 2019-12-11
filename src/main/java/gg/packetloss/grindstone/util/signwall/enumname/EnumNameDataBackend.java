package gg.packetloss.grindstone.util.signwall.enumname;

import gg.packetloss.grindstone.util.signwall.SignWallDataBackend;
import org.apache.commons.lang3.Validate;

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
