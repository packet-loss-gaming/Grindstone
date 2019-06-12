package gg.packetloss.grindstone.items;

import gg.packetloss.grindstone.items.specialattack.SpecType;

public enum WeaponType {
    MELEE(SpecType.MELEE),
    RANGED(SpecType.RANGED);

    private final SpecType specType;

    WeaponType(SpecType specType) {
        this.specType = specType;
    }

    public SpecType getDefaultSpecType() {
        return specType;
    }
}
