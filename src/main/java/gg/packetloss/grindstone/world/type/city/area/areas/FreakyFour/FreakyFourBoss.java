/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.FreakyFour;

import org.bukkit.entity.*;

public enum FreakyFourBoss {
    CHARLOTTE("Charlotte", Spider.class),
    FRIMUS("Frimus", Blaze.class),
    DA_BOMB("Da-Bomb", Creeper.class),
    SNIPEE("Snipee", Skeleton.class);

    private final String name;
    private final Class<? extends Monster> entityClass;

    FreakyFourBoss(String name, Class<? extends Monster> entityClass) {
        this.name = name;
        this.entityClass = entityClass;
    }

    public String getProperName() {
        return name;
    }

    public Class<? extends Monster> getEntityClass() {
        return entityClass;
    }

    public int getNumEssenceForEntry() {
        return 5;
    }

    public int getNumEssenceForCompletion() {
        int totalEssenceNeeded = values().length * getNumEssenceForEntry();
        int essenceAlreadyUsed = ordinal() * getNumEssenceForEntry();
        return totalEssenceNeeded - essenceAlreadyUsed;
    }

    public FreakyFourBoss next() {
        if (ordinal() == values().length - 1) {
            return null;
        }
        return values()[ordinal() + 1];
    }
}
