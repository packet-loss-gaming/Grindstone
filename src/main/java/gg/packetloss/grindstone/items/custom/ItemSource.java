/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

public enum ItemSource {
    ENCHANTED_FOREST("Enchanted Forest"),
    CURSED_MINE("Cursed Mine"),
    DROP_PARTY("Drop Party"),
    VINEAM_PRISON("Vineam Prison"),
    GIANT_BOSS("Giant Boss"),
    PATIENT_X("Patient X"),
    SACRIFICIAL_PIT("Sacrificial Pit",
            ENCHANTED_FOREST,
            DROP_PARTY,
            VINEAM_PRISON,
            GIANT_BOSS,
            PATIENT_X
    ),
    MARKET("Market"),
    NINJA_GUILD("Ninja Guild"),
    ARROW_FISHING("Arrow Fishing"),
    GOLD_RUSH("Gold Rush"),
    GRAVE_YARD("Grave Yard"),
    FROSTBORN("Frostborn"),
    APOCALYPSE("Apocalypse");

    private String friendlyName;
    private ItemSource[] subSources;
    ItemSource(String friendlyName, ItemSource... itemSource) {
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
