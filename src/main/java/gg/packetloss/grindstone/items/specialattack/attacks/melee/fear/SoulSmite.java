/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.fear;

import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SoulSmite extends EntityAttack implements MeleeSpecial {

    public SoulSmite(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private void attackPlayer() {
        final double targetHP = target.getHealth() / target.getMaxHealth();

        target.setHealth((targetHP / 2) * target.getMaxHealth());
        server.getScheduler().runTaskLater(inst, () -> {
            if (target.isValid()) {
                double newTargetHP = target.getHealth() / target.getMaxHealth();
                if (newTargetHP < targetHP) {
                    target.setHealth(target.getMaxHealth() * targetHP);
                }
            }
            inform("Your sword releases its grasp on its victim.");
        }, 20 * (int) Math.min(20, target.getMaxHealth() / 5 + 1));
        inform("Your sword steals its victims health for a short time.");
    }

    private SpecialAttack getSubSpec() {
        switch (ChanceUtil.getRandom(3)) {
            case 1:
                return new ChainLightning(owner, usedItem, target);
            case 2:
                return new Decimate(owner, usedItem, target);
            default:
                return new FearBlaze(owner, usedItem, target);
        }
    }

    private void attackMob() {
        inform(ChatColor.DARK_RED + "The fury of hallow flows through your sword!");
        for (int i = Math.min(8, Math.max(2, (int) (target.getHealth() / 250))); i > 0; --i) {
            server.getScheduler().runTaskLater(inst, () -> {
                getSubSpec().activate();
            }, i * 15);
        }
    }

    @Override
    public void activate() {
        if (target instanceof Player) {
            attackPlayer();
        } else {
            attackMob();
        }
    }
}
