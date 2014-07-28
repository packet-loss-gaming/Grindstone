package com.skelril.aurora.items.specialattack;

public enum SpecType {

    RED_FEATHER(1000),
    RANGED(3800),
    MELEE(3800),
    ANIMAL_BOW(15000);

    private final long delay;

    private SpecType(long delay) {

        this.delay = delay;
    }

    public long getDelay() {

        return delay;
    }
}