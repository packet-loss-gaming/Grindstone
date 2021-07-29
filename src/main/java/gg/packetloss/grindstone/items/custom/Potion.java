/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.custom;

import org.bukkit.potion.PotionEffectType;

public class Potion {
    private PotionEffectType type;
    private long time;
    private int level;

    public Potion(PotionEffectType type, long time, int level) {
        this.type = type;
        this.time = time;
        this.level = level;
    }

    public PotionEffectType getType() {
        return type;
    }

    public long getTime() {
        return time;
    }

    public int getLevel() {
        return level;
    }
}
