/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.generic;

import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public abstract class BloodLustArmor extends AbstractItemFeatureImpl {

    public abstract boolean hasArmor(Player player);

    private static EDBEExtractor<Player, LivingEntity, Projectile> extractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Projectile.class
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        CombatantPair<Player, LivingEntity, Projectile> result = extractor.extractFrom(event);
        if (result == null) return;

        Player player = result.getAttacker();

        if (hasArmor(player)) {
            ItemStack[] armour = player.getInventory().getArmorContents();
            ItemStack itemStack = CollectionUtil.getElement(armour);

            int baseDamage = (int) event.getDamage();
            int damageToHeal = ChanceUtil.getRangedRandom(baseDamage, baseDamage * 3);
            if (damageToHeal > itemStack.getDurability()) {
                itemStack.setDurability((short) 0);
            } else {
                itemStack.setDurability((short) (itemStack.getDurability() - damageToHeal));
            }

            player.getInventory().setArmorContents(armour);
        }
    }
}
