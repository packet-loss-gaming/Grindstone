/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.optimization;

import com.destroystokyo.paper.entity.ai.VanillaGoal;
import gg.packetloss.grindstone.util.mobai.SimpleAttackNearestPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Zombie;

public class OptimizedZombieFactory {
    private OptimizedZombieFactory() { }

    public static Zombie create(Location location) {
        Zombie zombie = location.getWorld().spawn(location, Zombie.class);

        // Remove problematic goals
        Bukkit.getMobGoals().removeGoal(zombie, VanillaGoal.ZOMBIE_ATTACK_TURTLE_EGG);
        Bukkit.getMobGoals().removeGoal(zombie, VanillaGoal.MOVE_THROUGH_VILLAGE);
        Bukkit.getMobGoals().removeGoal(zombie, VanillaGoal.NEAREST_ATTACKABLE);

        // Add back a basic nearest attackable player targeting goal
        Bukkit.getMobGoals().addGoal(zombie, 1, new SimpleAttackNearestPlayer(zombie));

        return zombie;
    }
}
