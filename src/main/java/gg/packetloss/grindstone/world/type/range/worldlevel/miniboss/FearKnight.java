/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel.miniboss;

import gg.packetloss.grindstone.bosses.instruction.PerformanceDropTableUnbind;
import gg.packetloss.grindstone.bosses.instruction.SpecialWeaponAttackInstruction;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.implementations.FearSwordImpl;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.dropttable.PerformanceKillInfo;
import gg.packetloss.grindstone.world.type.range.worldlevel.*;
import gg.packetloss.grindstone.world.type.range.worldlevel.demonicrune.DemonicRuneState;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.instruction.RangeWorldMinibossBasicDamageInstruction;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.instruction.RangeWorldMinibossBindInstruction;
import gg.packetloss.openboss.bukkit.entity.BukkitEntity;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.instruction.BindInstruction;
import gg.packetloss.openboss.instruction.DamageInstruction;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.instruction.UnbindInstruction;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class FearKnight implements RangeWorldMinibossSpawner {
    private final WorldLevelComponent worldLevelComponent;

    private static final String BOUND_NAME = "Fear Knight";
    private final PerformanceDropTable dropTable = new PerformanceDropTable();
    private final RangeWorldMinibossDeclaration<Zombie> miniBoss;

    public FearKnight(WorldLevelComponent worldLevelComponent) {
        this.worldLevelComponent = worldLevelComponent;
        miniBoss = new RangeWorldMinibossDeclaration<>(worldLevelComponent, BOUND_NAME, Zombie.class);
        setupDropTable();
        setupBoss();
    }

    private WorldLevelConfig getConfig() {
        return worldLevelComponent.getConfig();
    }

    private void setupDropTable() {
        dropTable.registerSlicedDrop(
            (info) -> 1,
            (info, consumer) -> {
                Player player = info.getPlayer();

                PerformanceKillInfo killInfo = info.getKillInfo();
                float percentDamageDone = killInfo.getPercentDamageDone(player).orElseThrow();

                ItemStack demonicRune = CustomItemCenter.build(CustomItems.DEMONIC_RUNE);
                LocalControllable<RangeWorldMinibossDetail> boss = miniBoss.getBound(
                    new BukkitEntity<>(info.getKillInfo().getKilled())
                );
                int worldLevel = boss.getDetail().getLevel();
                DemonicRuneState.makeItemFromRuneState(
                    demonicRune,
                    DemonicRuneState.fromMonsterKill(
                        worldLevel,
                        getConfig().miniBossFearKnightDropTableTypeModifier,
                        percentDamageDone
                    )
                );
                consumer.accept(demonicRune);
            }
        );
    }

    private void setupBoss() {
        List<BindInstruction<RangeWorldMinibossDetail>> bindInstructions = miniBoss.bindInstructions;
        bindInstructions.add(new RangeWorldMinibossBindInstruction<>(BOUND_NAME, () -> 20D * 30));
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<RangeWorldMinibossDetail, BindInstruction<RangeWorldMinibossDetail>> process(LocalControllable<RangeWorldMinibossDetail> controllable) {
                Zombie anEntity = (Zombie) BukkitUtil.getBukkitEntity(controllable);

                EntityEquipment equipment = anEntity.getEquipment();
                equipment.setHelmet(CustomItemCenter.build(CustomItems.GOD_HELMET));
                equipment.setHelmetDropChance(.25F);
                equipment.setChestplate(CustomItemCenter.build(CustomItems.GOD_CHESTPLATE));
                equipment.setChestplateDropChance(.25F);
                equipment.setLeggings(CustomItemCenter.build(CustomItems.GOD_LEGGINGS));
                equipment.setLeggingsDropChance(.25F);
                equipment.setBoots(CustomItemCenter.build(CustomItems.GOD_BOOTS));
                equipment.setBootsDropChance(.25F);

                equipment.setItemInMainHand(CustomItemCenter.build(CustomItems.FEAR_SWORD));
                equipment.setItemInMainHandDropChance(.001F);

                return null;
            }
        });

        List<UnbindInstruction<RangeWorldMinibossDetail>> unbindInstructions = miniBoss.unbindInstructions;
        unbindInstructions.add(new PerformanceDropTableUnbind<>(dropTable));

        List<DamageInstruction<RangeWorldMinibossDetail>> damageInstructions = miniBoss.damageInstructions;
        damageInstructions.add(new RangeWorldMinibossBasicDamageInstruction<>());
        damageInstructions.add(new SpecialWeaponAttackInstruction<>(
            new FearSwordImpl(),
            CustomItemCenter.build(CustomItems.FEAR_SWORD)
        ));
    }

    @Override
    public void spawnBoss(Location spawnLoc, Player target, int worldLevel) {
        Zombie entity = spawnLoc.getWorld().spawn(spawnLoc, Zombie.class);
        miniBoss.bind(entity, worldLevel);
        entity.setTarget(target);
    }
}

