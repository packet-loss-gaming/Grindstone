/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.mobai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class SimpleAttackNearestPlayer implements Goal<Monster> {
    private static final GoalKey<Monster> KEY = GoalKey.of(
        Monster.class,
        new NamespacedKey(CommandBook.inst(), "attack_nearest_player")
    );

    private final Monster owner;

    public SimpleAttackNearestPlayer(Monster owner) {
        this.owner = owner;
    }

    @Override
    public boolean shouldActivate() {
        return true;
    }

    private Player getNearestPlayer() {
        Location loc = owner.getLocation();

        Player closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Player potentialTarget : loc.getNearbyEntitiesByType(Player.class, EntityUtil.getFollowRange(owner))) {
            if (potentialTarget.getGameMode() != GameMode.SURVIVAL) {
                continue;
            }

            double distToTarget = LocationUtil.distanceSquared2D(loc, potentialTarget.getLocation());
            if (distToTarget < closestDist && owner.hasLineOfSight(potentialTarget)) {
                closest = potentialTarget;
                closestDist = distToTarget;
            }
        }

        return closest;
    }

    @Override
    public void start() {
        owner.setTarget(getNearestPlayer());
    }

    @Override
    public void tick() {
        if (!ChanceUtil.getChance(20)) {
            return;
        }

        owner.setTarget(getNearestPlayer());
    }

    @Override
    public @NotNull GoalKey<Monster> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.TARGET);
    }
}
