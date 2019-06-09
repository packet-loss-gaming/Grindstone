/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items.implementations;

import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackPreDamageEvent;
import gg.packetloss.grindstone.items.CustomItemSession;
import gg.packetloss.grindstone.items.WeaponType;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.generic.weapons.SpecWeaponImpl;
import gg.packetloss.grindstone.items.specialattack.SpecType;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeaponSysImpl extends AbstractItemFeatureImpl {

    private static int ignoredCounter = 0;

    private static EDBEExtractor<Player, LivingEntity, Projectile> specExtractor = new EDBEExtractor<>(
            Player.class,
            LivingEntity.class,
            Projectile.class
    );

    private List<Map<CustomItems, SpecWeaponImpl>> weaponLookup = new ArrayList<>();


    public WeaponSysImpl() {
        for (int i = 0; i < WeaponType.values().length; ++i) {
            weaponLookup.add(new HashMap<>());
        }
    }

    private Map<CustomItems, SpecWeaponImpl> getMapForType(WeaponType type) {
        return weaponLookup.get(type.ordinal());
    }

    public void add(WeaponType type, CustomItems item, SpecWeaponImpl weapon) {
        getMapForType(type).put(item, weapon);
    }

    public static boolean currentlyDoingSpecialAttackDamage() {
        return ignoredCounter > 0;
    }

    private SpecWeaponImpl getSpecialImplForItem(ItemStack itemStack, WeaponType weaponType) {
        for (Map.Entry<CustomItems, SpecWeaponImpl> entry : getMapForType(weaponType).entrySet()) {
            if (ItemUtil.isItem(itemStack, entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void specAttack(EntityDamageByEntityEvent event) {
        WeaponAttackInfo attackInfo = WeaponAttackExtractor.extractFrom(event);
        if (attackInfo == null) {
            return;
        }

        Player owner = attackInfo.getAttacker();
        LivingEntity target = attackInfo.getDefender();

        if (target != null && owner != target) {

            CustomItemSession session = getSession(owner);

            WeaponType weaponType = attackInfo.wasRangedAttack() ? WeaponType.RANGED : WeaponType.MELEE;

            SpecWeaponImpl specImpl = getSpecialImplForItem(attackInfo.getUsedItem(), weaponType);
            SpecialAttack spec = specImpl == null ? null : specImpl.getSpecial(owner, target);

            SpecType specType = weaponType.getDefaultSpecType();
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
        WeaponAttackInfo attackInfo = WeaponAttackExtractor.extractFrom(event);
        if (attackInfo == null) {
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause.equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
            ItemStack targetItem = attackInfo.getUsedItem();
            Map<String, String> map = ItemUtil.getItemTags(targetItem);

            double modifier = 1;

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
        } else if (cause.equals(EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK)) {
            ItemStack targetItem = attackInfo.getUsedItem();
            if (ItemUtil.isCustomItemKind(targetItem, "Short Sword")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpecialAttackPreDamage(SpecialAttackPreDamageEvent event) {
        ++ignoredCounter;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSpecialAttackPostDamage(EntityDamageByEntityEvent event) {
        if (ignoredCounter > 0) {
            --ignoredCounter;
        }
    }

    private static class WeaponAttackInfo {
        private final Player attacker;
        private final LivingEntity defender;
        private final ItemStack launcher;

        private WeaponAttackInfo(Player attacker, LivingEntity defender, ItemStack launcher) {
            this.attacker = attacker;
            this.defender = defender;
            this.launcher = launcher;
        }

        public Player getAttacker() {
            return attacker;
        }

        public LivingEntity getDefender() {
            return defender;
        }

        public boolean wasRangedAttack() {
            return launcher != null;
        }

        public ItemStack getUsedItem() {
            return wasRangedAttack() ? launcher : this.attacker.getItemInHand();
        }
    }

    private static class WeaponAttackExtractor {
        public static WeaponAttackInfo extractFrom(EntityDamageByEntityEvent event) {
            if (currentlyDoingSpecialAttackDamage()) {
                return null;
            }

            CombatantPair<Player, LivingEntity, Projectile> result = specExtractor.extractFrom(event);
            if (result == null) return null;

            ItemStack launcher = null;
            if (result.hasProjectile()) {
                Projectile projectile = result.getProjectile();
                if (projectile.hasMetadata("launcher")) {
                    Object test = projectile.getMetadata("launcher").get(0).value();
                    if (test instanceof ItemStack) {
                        launcher = (ItemStack) test;
                    }
                }

                if (launcher == null) return null;
            }

            return new WeaponAttackInfo(result.getAttacker(), result.getDefender(), launcher);
        }
    }
}
