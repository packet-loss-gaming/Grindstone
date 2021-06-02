/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import com.destroystokyo.paper.Title;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.util.RomanNumeralUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ExecutionerAxeImpl extends AbstractItemFeatureImpl {

    private static EDBEExtractor<Player, LivingEntity, Projectile> extractor = new EDBEExtractor<>(
        Player.class,
        LivingEntity.class,
        Projectile.class
    );

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        CombatantPair<Player, LivingEntity, Projectile> result = extractor.extractFrom(event);

        if (result == null || result.hasProjectile()) {
            return;
        }

        Player attacker = result.getAttacker();
        if (!ItemUtil.isHoldingItem(attacker, CustomItems.EXECUTIONER_AXE)) {
            return;
        }

        if (attacker.getAttackCooldown() < .999) {
            return;
        }

        int zombieCount = (int) attacker.getLocation().getNearbyEntitiesByType(Zombie.class, 7).stream()
            .filter(e -> !e.isDead())
            .count();
        event.setDamage(event.getDamage() * zombieCount * 10);

        if (zombieCount == 0) {
            return;
        }

        attacker.sendTitle(Title.builder()
            .title(Text.of(ChatColor.DARK_RED, "Executioner's Rage").build())
            .subtitle(Text.of(ChatColor.DARK_RED, RomanNumeralUtil.toRoman(zombieCount)).build())
            .fadeIn(10)
            .stay(20 * 3)
            .fadeOut(10)
            .build());
    }
}
