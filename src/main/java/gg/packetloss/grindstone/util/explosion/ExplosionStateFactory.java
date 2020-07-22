/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.explosion;

import gg.packetloss.grindstone.util.tracker.CauseStack;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public class ExplosionStateFactory {
    private static CauseStack<Player> explosionCauseStack = new CauseStack<>();

    private static void createTrackedExplosion(Player cause, Location loc, float power, boolean setFire, boolean breakBlocks) {
        explosionCauseStack.executeOnStackWithCause(cause, () -> {
            loc.createExplosion(power, setFire, breakBlocks);
        });
    }

    public static void createPvPExplosion(Player attacker, Location loc, float power, boolean setFire, boolean breakBlocks) {
        createTrackedExplosion(attacker, loc, power, setFire, breakBlocks);
    }

    public static void createExplosion(Location loc, float power, boolean setFire, boolean breakBlocks) {
        createTrackedExplosion(null, loc, power, setFire, breakBlocks);
    }

    public static void createFakeExplosion(Location loc) {
        createTrackedExplosion(null, loc, 0, false, false);
    }

    public static Optional<Player> getExplosionCreator() {
        return explosionCauseStack.getCurCause();
    }
}
