/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item.custom;

public enum ItemSource {
    ENCHANTED_FOREST("Enchanted Forest"),
    DROP_PARTY("Drop Party"),
    VINEAM_PRISON("Vineam Prison"),
    GIANT_BOSS("Giant Boss"),
    WILDERNESS_MOBS("Wilderness Mobs"),
    PATIENT_X("Patient X"),
    SACRIFICIAL_PIT("Sacrificial Pit",
            ENCHANTED_FOREST,
            DROP_PARTY,
            VINEAM_PRISON,
            GIANT_BOSS,
            WILDERNESS_MOBS,
            PATIENT_X
    ),
    MARKET("Market"),
    NINJA_GUILD("Ninja Guild"),
    ARROW_FISHING("Arrow Fishing"),
    GOLD_RUSH("Gold Rush"),
    GRAVE_YARD("Grave Yard");

    private String friendlyName;
    private ItemSource[] subSources;
    private ItemSource(String friendlyName, ItemSource... itemSource) {
        this.friendlyName = friendlyName;
        subSources = itemSource;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public ItemSource[] getSubSources() {
        return subSources;
    }
}
