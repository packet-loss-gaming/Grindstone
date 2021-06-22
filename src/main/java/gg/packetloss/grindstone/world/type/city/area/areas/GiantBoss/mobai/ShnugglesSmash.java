/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.GiantBoss.mobai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Giant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Objects;

public class ShnugglesSmash implements Goal<Giant> {
    private static final GoalKey<Giant> KEY = GoalKey.of(
        Giant.class,
        new NamespacedKey(CommandBook.inst(), "shnuggles_smash")
    );

    private static final double DIST = Math.pow(5, 2);

    private final Giant owner;

    public ShnugglesSmash(Giant owner) {
        this.owner = owner;
    }

    @Override
    public boolean shouldActivate() {
        return owner.getTarget() != null;
    }

    @Override
    public void tick() {
        LivingEntity target = Objects.requireNonNull(owner.getTarget());
        Location ownerLoc = owner.getLocation();
        if (LocationUtil.distanceSquared2D(ownerLoc, target.getLocation()) < DIST) {
            for (Player toHurt : ownerLoc.getNearbyEntitiesByType(Player.class, DIST)) {
                if (toHurt.isDead()) {
                    continue;
                }

                toHurt.damage(ChanceUtil.getRangedRandom(5, 40), owner);
            }
        } else {
            owner.getPathfinder().moveTo(target);
        }
    }

    @Override
    public @NotNull GoalKey<Giant> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}