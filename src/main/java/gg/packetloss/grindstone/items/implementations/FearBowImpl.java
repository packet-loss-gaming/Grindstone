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
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.fear.Curse;
import gg.packetloss.grindstone.items.specialattack.attacks.hybrid.fear.HellCano;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.*;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import static gg.packetloss.grindstone.ProjectileWatchingComponent.getSpawningItem;

public class FearBowImpl extends AbstractItemFeatureImpl implements SpecWeaponImpl {
    @Override
    public SpecialAttack getSpecial(LivingEntity owner, ItemStack usedItem, LivingEntity target) {
        return ChanceUtil.supplyRandom(
            () -> {
                Disarm disarmSpec = new Disarm(owner, usedItem, target);
                if (disarmSpec.getItemStack() != null) {
                    return disarmSpec;
                }
                return getSpecial(owner, usedItem, target);
            },
            () -> new Curse(owner, usedItem, target),
            () -> new MagicChain(owner, usedItem, target),
            () -> new FearStrike(owner, usedItem, target),
            () -> new SoulReaper(owner, usedItem, target),
            () -> new HellCano(owner, usedItem, target)
        );
    }

    @EventHandler
    public void onArrowLand(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        ProjectileSource source = projectile.getShooter();
        if (source instanceof Player) {
            getSpawningItem(projectile).ifPresent((launcher) -> {
                final Player owner = (Player) source;

                CustomItemSession session = getSession(owner);

                if (!session.canSpec(SpecType.RANGED)) {

                    if (ItemUtil.isItem(launcher, CustomItems.FEAR_BOW)) {
                        SpecialAttackEvent specEvent = callSpec(owner, SpecType.PASSIVE, new PassiveLightning(projectile, launcher));
                        if (!specEvent.isCancelled()) {
                            session.updateSpec(specEvent.getContext(), specEvent.getContextCoolDown());
                            specEvent.getSpec().activate();
                        }
                    }
                }
            });
        }
    }
}
