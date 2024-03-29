/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses.manager.apocalypse;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.bosses.impl.SimpleRebindableBoss;
import gg.packetloss.grindstone.bosses.manager.apocalypse.instruction.ApocalypseDropTableInstruction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.openboss.bukkit.entity.BukkitBoss;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.*;
import gg.packetloss.openboss.util.AttackDamage;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;

import java.util.List;

public class ZapperZombie {
    private SimpleRebindableBoss<Zombie> zapperZombie;

    public static final String BOUND_NAME = "Zapper Zombie";

    private PerformanceDropTable dropTable = new PerformanceDropTable();

    public ZapperZombie() {
        zapperZombie = new SimpleRebindableBoss<>(BOUND_NAME, Zombie.class, CommandBook.inst(), new SimpleInstructionDispatch<>());
        setupDropTable();
        setupZapperZombie();
    }

    public void bind(Damageable entity) {
        zapperZombie.bind(new BukkitBoss<>(entity, new GenericDetail()));
    }

    private void setupDropTable() {
        dropTable.registerTakeAllDrop(1000, () -> CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
    }

    private void setupZapperZombie() {
        List<BindInstruction<GenericDetail>> bindInstructions = zapperZombie.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, BindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Zombie) {
                    anEntity.setCustomName(BOUND_NAME);

                    // Ensure zapper zombies are sometimes babies
                    ((Zombie) anEntity).setBaby(ChanceUtil.getChance(3));

                    // Ensure thor zombies cannot pickup items, they're just suicide "bombers"
                    ((Zombie) anEntity).setCanPickupItems(false);

                    // Set health
                    ((LivingEntity) anEntity).setMaxHealth(20);
                    ((LivingEntity) anEntity).setHealth(20);

                    // Gear them up
                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    equipment.setArmorContents(ItemUtil.GOLD_ARMOR);
                }
                return null;
            }
        });

        List<UnbindInstruction<GenericDetail>> unbindInstructions = zapperZombie.unbindInstructions;
        unbindInstructions.add(new ApocalypseDropTableInstruction<>(dropTable));

        List<DamageInstruction<GenericDetail>> damageInstructions = zapperZombie.damageInstructions;
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, DamageInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                ((LivingEntity) boss).setHealth(0);

                for (int i =  ((Zombie) boss).isBaby() ? 1 : 5; i > 0; --i) {
                    boss.getWorld().strikeLightning(boss.getLocation());
                }

                zapperZombie.silentUnbind(controllable);
                return null;
            }
        });
    }
}
