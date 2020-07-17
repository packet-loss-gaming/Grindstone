/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.manager.apocalypse;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.instruction.*;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.bosses.impl.SimpleRebindableBoss;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.bosses.manager.apocalypse.instruction.ApocalypseDropTableInstruction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.item.ItemUtil;
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

    private SimpleRebindableBoss<Zombie> chuckerZombie;

    public static final String BOUND_NAME = "Chucker Zombie";

    private PerformanceDropTable dropTable = new PerformanceDropTable();

    public ChuckerZombie() {
        chuckerZombie = new SimpleRebindableBoss<>(BOUND_NAME, Zombie.class, inst, new SimpleInstructionDispatch<>());
        setupDropTable();
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

    private void setupDropTable() {
        dropTable.registerTakeAllDrop(1000, () -> CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
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
        unbindInstructions.add(new ApocalypseDropTableInstruction<>(dropTable));

        List<DamagedInstruction<GenericDetail>> damagedInstructions = chuckerZombie.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());

        List<PassiveInstruction<GenericDetail>> passiveInstructions = chuckerZombie.passiveInstructions;
        passiveInstructions.add(new PassiveInstruction<GenericDetail>() {
            @Override
            public InstructionResult<GenericDetail, PassiveInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);

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
                nearestPlayerVel.setY(ChanceUtil.getRangedRandom(.4, .8));

                for (Entity entity : boss.getLocation().getNearbyEntitiesByType(Zombie.class, 4)) {
                    if (ChanceUtil.getChance(5)) {
                        entity.setVelocity(nearestPlayerVel);
                    }
                }

                return null;
            }
        });
    }
}
