/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.manager.apocalypse;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.bosses.impl.SimpleRebindableBoss;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.bosses.manager.apocalypse.instruction.ApocalypseDropTableInstruction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.openboss.bukkit.entity.BukkitBoss;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.*;
import gg.packetloss.openboss.util.AttackDamage;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class StickyZombie {
    private SimpleRebindableBoss<Zombie> stickyZombie;

    public static final String BOUND_NAME = "Sticky Zombie";

    private PerformanceDropTable dropTable = new PerformanceDropTable();

    public StickyZombie() {
        stickyZombie = new SimpleRebindableBoss<>(BOUND_NAME, Zombie.class, CommandBook.inst(), new SimpleInstructionDispatch<>());
        setupDropTable();
        setupStickyZombie();
    }

    public void bind(Damageable entity) {
        stickyZombie.bind(new BukkitBoss<>(entity, new GenericDetail()));
    }

    private void setupDropTable() {
        dropTable.registerTakeAllDrop(1000, () -> CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
    }

    private void setupStickyZombie() {
        List<BindInstruction<GenericDetail>> bindInstructions = stickyZombie.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, BindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Zombie) {
                    anEntity.setCustomName(BOUND_NAME);

                    // Ensure sticky zombies are never babies
                    ((Zombie) anEntity).setBaby(false);

                    // Ensure sticky zombies cannot pickup items, they are OP enough
                    ((Zombie) anEntity).setCanPickupItems(false);

                    // Set health
                    ((LivingEntity) anEntity).setMaxHealth(250);
                    ((LivingEntity) anEntity).setHealth(250);

                    // Gear them up
                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    ItemStack weapon = new ItemStack(Material.SLIME_BALL);
                    weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                    equipment.setItemInMainHand(weapon);
                    equipment.setArmorContents(ItemUtil.GOLD_ARMOR);
                }
                return null;
            }
        });

        List<UnbindInstruction<GenericDetail>> unbindInstructions = stickyZombie.unbindInstructions;
        unbindInstructions.add(new ApocalypseDropTableInstruction<>(dropTable));

        List<DamageInstruction<GenericDetail>> damageInstructions = stickyZombie.damageInstructions;
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, DamageInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable, LocalEntity entity, AttackDamage damage) {
                final Entity toHit = BukkitUtil.getBukkitEntity(entity);
                ((LivingEntity) toHit).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 15 * 20, 2));
                return null;
            }
        });

        List<DamagedInstruction<GenericDetail>> damagedInstructions = stickyZombie.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());
    }
}
