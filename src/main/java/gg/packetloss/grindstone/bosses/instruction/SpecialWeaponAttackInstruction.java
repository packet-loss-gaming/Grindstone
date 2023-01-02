/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.instruction;

import gg.packetloss.grindstone.items.generic.weapons.SpecWeaponImpl;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.EntityDetail;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.DamageInstruction;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.util.AttackDamage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class SpecialWeaponAttackInstruction<T extends EntityDetail> extends DamageInstruction<T> {
    private final SpecWeaponImpl weapon;
    private final ItemStack weaponItem;

    public SpecialWeaponAttackInstruction(SpecWeaponImpl weapon, ItemStack weaponItem) {
        this.weapon = weapon;
        this.weaponItem = weaponItem;
    }

    public SpecWeaponImpl getWeapon() {
        return weapon;
    }

    public SpecialAttack getSpec(LivingEntity owner, LivingEntity target) {
        return weapon.getSpecial(owner, this.weaponItem, target);
    }

    public void activateSpecial(SpecialAttack spec) {
        spec.activate();
    }

    @Override
    public InstructionResult<T, DamageInstruction<T>> process(LocalControllable<T> controllable, LocalEntity entity, AttackDamage attackDamage) {
        LivingEntity attacker = (LivingEntity) BukkitUtil.getBukkitEntity(controllable);
        LivingEntity defender = (LivingEntity) BukkitUtil.getBukkitEntity(entity);

        activateSpecial(getSpec(attacker, defender));
        return null;
    }
}
