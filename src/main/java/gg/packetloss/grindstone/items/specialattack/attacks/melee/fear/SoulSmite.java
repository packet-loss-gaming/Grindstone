/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.specialattack.attacks.melee.fear;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.items.specialattack.EntityAttack;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.melee.MeleeSpecial;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class SoulSmite extends EntityAttack implements MeleeSpecial {

    public SoulSmite(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        super(owner, usedItem, target);
    }

    private SpecialAttack getSubSpec() {
        return ChanceUtil.supplyRandom(
            () -> new ChainLightning(owner, usedItem, target),
            () -> new Decimate(owner, usedItem, target),
            () -> new FearBlaze(owner, usedItem, target)
        );
    }

    @Override
    public void activate() {
        inform(ChatColor.DARK_RED + "The fury of hallow flows through your sword!");
        for (int i = Math.min(8, Math.max(2, (int) (EntityUtil.getHealth(owner, target) / 250))); i > 0; --i) {
            Bukkit.getScheduler().runTaskLater(CommandBook.inst(), () -> {
                getSubSpec().activate();
            }, i * 15);
        }
    }
}
