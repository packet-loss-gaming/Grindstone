/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.state.player;

public class PlayerVitals {
    private final double health;
    private final int hunger;
    private final float saturation;
    private final float exhaustion;

    public PlayerVitals(double health, int hunger, float saturation, float exhaustion) {
        this.health = health;
        this.hunger = hunger;
        this.saturation = saturation;
        this.exhaustion = exhaustion;
    }

    public double getHealth() {
        return health;
    }

    public int getHunger() {
        return hunger;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getExhaustion() {
        return exhaustion;
    }
}
