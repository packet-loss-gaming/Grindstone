/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.items.CustomItemSession;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.generic.weapons.SpecWeaponImpl;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.util.DamageUtil;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class WeaponSysImpl extends AbstractItemFeatureImpl {

    private static Queue<EntityDamageByEntityEvent> attackQueue = new LinkedList<>();

    private static EDBEExtractor<Player, LivingEntity, Projectile> specExtractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Projectile.class
    );

    private Map<CustomItems, SpecWeaponImpl> rangedWeapons = new HashMap<>();
    private Map<CustomItems, SpecWeaponImpl> meleeWeapons = new HashMap<>();

    public void addRanged(CustomItems item, SpecWeaponImpl weapon) {
        rangedWeapons.put(item, weapon);
    }

    public void addMelee(CustomItems item, SpecWeaponImpl weapon) {
        meleeWeapons.put(item, weapon);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void specAttack(EntityDamageByEntityEvent event) {

        // Handle cancellation here so that we don't end up with a memory leak from the queue
        if (attackQueue.poll() != null || event.isCancelled()) return;

        CombatantPair<Player, LivingEntity, Projectile> result = specExtractor.extractFrom(event);

        if (result == null) return;

        ItemStack launcher = null;
        if (result.hasProjectile()) {
            Projectile projectile = result.getProjectile();
            if (projectile.hasMetadata("launcher")) {
                Object test = projectile.getMetadata("launcher").get(0).value();
                if (test instanceof ItemStack) {
                    launcher = (ItemStack) test;
                }
            }

            if (launcher == null) return;
        }

        Player owner = result.getAttacker();
        LivingEntity target = result.getDefender();

        if (target != null && owner != target) {

            CustomItemSession session = getSession(owner);

            SpecType specType = null;
            SpecialAttack spec = null;

            if (launcher != null) {

                specType = SpecType.RANGED;

                for (Map.Entry<CustomItems, SpecWeaponImpl> entry : rangedWeapons.entrySet()) {
                    if (ItemUtil.isItem(launcher, entry.getKey())) {
                        spec = entry.getValue().getSpecial(owner, target);
                        break;
                    }
                }
            } else {

                specType = SpecType.MELEE;

                for (Map.Entry<CustomItems, SpecWeaponImpl> entry : meleeWeapons.entrySet()) {
                    if (ItemUtil.isHoldingItem(owner, entry.getKey())) {
                        spec = entry.getValue().getSpecial(owner, target);
                        break;
                    }
                }
            }

            if (spec != null && session.canSpec(specType)) {

                SpecialAttackEvent specEvent = callSpec(owner, specType, spec);

                if (!specEvent.isCancelled()) {
                    session.updateSpec(specType, specEvent.getContextCoolDown());
                    specEvent.getSpec().activate();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void damageModifier(EntityDamageByEntityEvent event) {

        if (DamageUtil.remove(event.getDamager(), event.getEntity())) {
            attackQueue.add(event);
            return;
        }

        CombatantPair<Player, LivingEntity, Projectile> result = specExtractor.extractFrom(event);

        if (result == null) return;

        ItemStack launcher = null;
        if (result.hasProjectile()) {
            Projectile projectile = result.getProjectile();
            if (projectile.hasMetadata("launcher")) {
                Object test = projectile.getMetadata("launcher").get(0).value();
                if (test instanceof ItemStack) {
                    launcher = (ItemStack) test;
                }
            }

            if (launcher == null) return;
        }

        Player owner = result.getAttacker();

        double modifier = 1;

        ItemStack targetItem = launcher;

        if (targetItem == null) {
            targetItem = owner.getItemInHand();
        }

        Map<String, String> map = ItemUtil.getItemTags(targetItem);

        if (map != null) {
            String modifierString = map.get(ChatColor.RED + "Damage Modifier");
            if (modifierString != null) {
                try {
                    modifier = Double.parseDouble(modifierString);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        event.setDamage(event.getDamage() * modifier);
    }
}
