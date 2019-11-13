/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
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
import java.util.logging.Logger;

public class StickyZombie {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<GenericDetail> stickyZombie;

    public static final String BOUND_NAME = "Sticky Zombie";

    public StickyZombie() {
        stickyZombie = new BukkitBossDeclaration<>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), BOUND_NAME);
            }
        };
        setupStickyZombie();
    }

    public void bind(Damageable entity) {
        stickyZombie.bind(new BukkitBoss<>(entity, new GenericDetail()));
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
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, UnbindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();

                if (ChanceUtil.getChance(1000)) {
                    target.getWorld().dropItem(target, CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
                }

                return null;
            }
        });

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
