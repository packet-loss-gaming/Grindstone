/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.hybrid.phantom;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.RangedSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PhantomFortune  extends EntityAttack implements MeleeSpecial, RangedSpecial {
    private static final int INTERVAL_TICKS = 20 * 3;
    private final int numberOfRuns = ChanceUtil.getRangedRandom(2, 5);

    private double targetLastHealth;

    public PhantomFortune(Player owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
        this.targetLastHealth = target.getHealth();
    }

    private Material getMaterial() {
        if (ChanceUtil.getChance(1000)) {
            return Material.GOLD_BLOCK;
        }

        if (ChanceUtil.getChance(100)) {
            return Material.GOLD_INGOT;
        }

        return Material.GOLD_NUGGET;
    }

    private void dropLoot(int lostHealth) {
        int numBars = (lostHealth / 5) + 1;
        while (numBars > 0) {
            int numItems = Math.min((int) numBars, 64);
            EntityUtil.spawnProtectedItem(
                new ItemStack(getMaterial(), numItems),
                (Player) owner,
                target.getLocation()
            );
            numBars -= numItems;
        }
    }

    @Override
    public void activate() {
        final Location targeted = target.getLocation();

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(INTERVAL_TICKS);
        taskBuilder.setNumberOfRuns(numberOfRuns);

        taskBuilder.setAction((times) -> {
            double targetNewHealth = target.getHealth();
            int targetLostHealth = (int) (targetLastHealth - targetNewHealth);
            if (targetLostHealth > 1) {
                dropLoot(targetLostHealth);
            }
            targetLastHealth = targetNewHealth;
            return true;
        });

        taskBuilder.build();

        inform("Your weapon suddenly is quite shinny.");
    }

    @Override
    public long getCoolDown(SpecType context) {
        return context.getDelay() + TimeUtil.convertTicksToMills(INTERVAL_TICKS * numberOfRuns);
    }
}
