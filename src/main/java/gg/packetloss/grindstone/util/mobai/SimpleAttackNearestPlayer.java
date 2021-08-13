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
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

import static org.bukkit.event.entity.EntityTargetEvent.TargetReason;

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
            if (GeneralPlayerUtil.hasInvulnerableGamemode(potentialTarget)) {
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

    private void setTarget(Player target) {
        var targetEvent = new EntityTargetLivingEntityEvent(owner, target, TargetReason.CLOSEST_ENTITY);
        CommandBook.callEvent(targetEvent);
        if (targetEvent.isCancelled()) {
            return;
        }

        owner.setTarget(targetEvent.getTarget());
    }

    private void setTargetToNearest() {
        setTarget(getNearestPlayer());
    }

    @Override
    public void start() {
        setTargetToNearest();
    }

    @Override
    public void tick() {
        if (!ChanceUtil.getChance(20)) {
            return;
        }

        setTargetToNearest();
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
