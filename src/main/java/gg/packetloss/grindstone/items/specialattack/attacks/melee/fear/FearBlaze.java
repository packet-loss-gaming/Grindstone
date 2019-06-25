/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.fear;

import com.destroystokyo.paper.ParticleBuilder;
import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.anticheat.RapidHitEvent;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.DamageUtil;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FearBlaze extends EntityAttack implements MeleeSpecial {

    public FearBlaze(LivingEntity owner, LivingEntity target) {
        super(owner, target);
    }

    private void ignite(LivingEntity target, int maxRuns, double power) {
        CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
            if (owner.isDead()) {
                return;
            }

            if (!owner.hasLineOfSight(target)) {
                return;
            }

            if (owner instanceof Player) {
                server.getPluginManager().callEvent(new RapidHitEvent((Player) owner));
            }

            double newPower = power;
            if (ChanceUtil.getChance(maxRuns)) {
                newPower *= 4;
                ChatUtil.sendNotice(owner, "The strength of the blaze intensifies!");
            }

            if (!DamageUtil.damageWithSpecialAttack(owner, target, this, newPower)) {
                return;
            }

            new ParticleBuilder(Particle.LAVA).count(40).location(target.getLocation()).allPlayers().spawn();

            int runsRemaining = maxRuns - 1;
            if (runsRemaining > 0) {
                ignite(target, runsRemaining, newPower);
            }
        }, 10);
    }

    @Override
    public void activate() {
        ignite(target, ChanceUtil.getRangedRandom(4, 30), 2);

        inform("Your eyes ignite with an infernal blaze...");
        inform("... your opponent will suffer!");
    }
}
