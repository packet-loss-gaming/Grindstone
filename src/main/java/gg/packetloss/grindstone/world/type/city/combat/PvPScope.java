/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.combat;

import org.bukkit.entity.Player;

public abstract class PvPScope {
    public abstract boolean isApplicable(Player player);
    public abstract boolean allowed(Player attacker, Player defender);

    public boolean checkFor(Player attacker, Player defender) {
        // noinspection SimplifiableIfStatement
        if (isApplicable(attacker) && isApplicable(defender)) {
            return allowed(attacker, defender);
        }
        return true;
    }
}
