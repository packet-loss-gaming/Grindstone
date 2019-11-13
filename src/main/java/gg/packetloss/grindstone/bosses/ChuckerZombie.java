/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
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
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.logging.Logger;

public class ChuckerZombie {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<GenericDetail> chuckerZombie;

    public static final String BOUND_NAME = "Chucker Zombie";

    public ChuckerZombie() {
        chuckerZombie = new BukkitBossDeclaration<>(inst, new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), BOUND_NAME);
            }
        };
        setupChuckerZombie();
        server.getScheduler().runTaskTimer(
                inst,
                () -> {
                    Lists.newArrayList(chuckerZombie.controlled.values()).forEach((ce) -> chuckerZombie.process(ce));
                },
                20 * 10,
                20 * 4
        );
    }

    public void bind(Damageable entity) {
        chuckerZombie.bind(new BukkitBoss<>(entity, new GenericDetail()));
    }

    private boolean isThrowableZombie(Entity entity) {
        String customName = entity.getCustomName();
        if (customName == null) {
            return false;
        }

        if (!(entity instanceof Zombie)) {
            return false;
        }

        if (customName.equals("Apocalyptic Zombie")) {
            return true;
        }

        if (customName.equals("Grave Zombie")) {
            return true;
        }

        if (customName.equals(ThorZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(ZapperZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(MercilessZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(StickyZombie.BOUND_NAME)) {
            return true;
        }

        if (customName.equals(ChuckerZombie.BOUND_NAME)) {
            return true;
        }

        return false;
    }

    private void setupChuckerZombie() {
        List<BindInstruction<GenericDetail>> bindInstructions = chuckerZombie.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, BindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Zombie) {
                    anEntity.setCustomName(BOUND_NAME);

                    // Ensure chucker zombies are never babies
                    ((Zombie) anEntity).setBaby(false);

                    // Ensure chucker zombies cannot pickup items, they are OP enough
                    ((Zombie) anEntity).setCanPickupItems(false);

                    // Set health
                    ((LivingEntity) anEntity).setMaxHealth(500);
                    ((LivingEntity) anEntity).setHealth(500);

                    // Gear them up
                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    ItemStack weapon = new ItemStack(Material.FEATHER);
                    weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
                    equipment.setItemInMainHand(weapon);
                    equipment.setArmorContents(ItemUtil.GOLD_ARMOR);
                }
                return null;
            }
        });

        List<UnbindInstruction<GenericDetail>> unbindInstructions = chuckerZombie.unbindInstructions;
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

        List<DamagedInstruction<GenericDetail>> damagedInstructions = chuckerZombie.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());

        List<PassiveInstruction<GenericDetail>> passiveInstructions = chuckerZombie.passiveInstructions;
        passiveInstructions.add(new PassiveInstruction<GenericDetail>() {
            @Override
            public InstructionResult<GenericDetail, PassiveInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);

                // FIXME: This needs fixed in open boss, this just works around the issue
                if (!boss.isValid()) {
                    chuckerZombie.silentUnbind(controllable);
                    return null;
                }

                Player closestPlayer = null;
                double curDist = Double.MAX_VALUE;
                for (Player player : boss.getWorld().getPlayers()) {
                    double dist = player.getLocation().distanceSquared(boss.getLocation());
                    if (dist < curDist) {
                        closestPlayer = player;
                        curDist = dist;
                    }
                }

                if (closestPlayer == null) {
                    return null;
                }

                Vector nearestPlayerVel = closestPlayer.getLocation().subtract(boss.getLocation()).toVector();
                nearestPlayerVel.normalize();
                nearestPlayerVel.multiply(2);
                nearestPlayerVel.setY(0);
                nearestPlayerVel.add(new Vector(0, ChanceUtil.getRangedRandom(.4, .8), 0));

                for (Entity entity : ((Zombie) boss).getNearbyEntities(4, 4, 4)) {
                    if (isThrowableZombie(entity) && ChanceUtil.getChance(5)) {
                        entity.setVelocity(nearestPlayerVel);
                    }
                }

                return null;
            }
        });
    }
}
