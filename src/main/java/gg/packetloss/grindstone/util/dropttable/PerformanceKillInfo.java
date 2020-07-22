/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

public interface PerformanceKillInfo extends KillInfo {
    public LivingEntity getKilled();

    public double getTotalDamage();
    public Optional<Double> getDamageDone(Player player);
    public Optional<Float> getPercentDamageDone(Player player);

    public Collection<Player> getDamagers();
    default public Optional<Player> getTopDamager() {
        Iterator<Player> it = getDamagers().iterator();
        if (it.hasNext()) {
            return Optional.of(it.next());
        }

        return Optional.empty();
    }
}
