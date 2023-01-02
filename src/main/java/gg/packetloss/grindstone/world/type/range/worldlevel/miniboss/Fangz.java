/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel.miniboss;

import gg.packetloss.grindstone.bosses.instruction.PerformanceDropTableUnbind;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.dropttable.PerformanceKillInfo;
import gg.packetloss.grindstone.world.type.range.worldlevel.*;
import gg.packetloss.grindstone.world.type.range.worldlevel.demonicrune.DemonicRuneState;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.instruction.RangeWorldMinibossBasicDamageInstruction;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.instruction.RangeWorldMinibossBindInstruction;
import gg.packetloss.openboss.bukkit.entity.BukkitEntity;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.BindInstruction;
import gg.packetloss.openboss.instruction.DamageInstruction;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.instruction.UnbindInstruction;
import gg.packetloss.openboss.util.AttackDamage;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class Fangz implements RangeWorldMinibossSpawner {
    private final WorldLevelComponent worldLevelComponent;

    private static final String BOUND_NAME = "Fangz";
    private final PerformanceDropTable dropTable = new PerformanceDropTable();
    private final RangeWorldMinibossDeclaration<Spider> miniBoss;

    public Fangz(WorldLevelComponent worldLevelComponent) {
        this.worldLevelComponent = worldLevelComponent;
        miniBoss = new RangeWorldMinibossDeclaration<>(worldLevelComponent, BOUND_NAME, Spider.class);
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
                        getConfig().miniBossFangzDropTableTypeModifier,
                        percentDamageDone
                    )
                );
                consumer.accept(demonicRune);
            }
        );
    }

    private void setupBoss() {
        List<BindInstruction<RangeWorldMinibossDetail>> bindInstructions = miniBoss.bindInstructions;
        bindInstructions.add(new RangeWorldMinibossBindInstruction<>(BOUND_NAME, () -> 20D * 50));

        List<UnbindInstruction<RangeWorldMinibossDetail>> unbindInstructions = miniBoss.unbindInstructions;
        unbindInstructions.add(new PerformanceDropTableUnbind<>(dropTable));
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<RangeWorldMinibossDetail, UnbindInstruction<RangeWorldMinibossDetail>> process(LocalControllable<RangeWorldMinibossDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                for (Entity aEntity : boss.getNearbyEntities(7, 4, 7)) {
                    if (!(aEntity instanceof LivingEntity)) continue;
                    ((LivingEntity) aEntity).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 30, 1));
                    ((LivingEntity) aEntity).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 30, 1));
                }
                return null;
            }
        });

        List<DamageInstruction<RangeWorldMinibossDetail>> damageInstructions = miniBoss.damageInstructions;
        damageInstructions.add(new RangeWorldMinibossBasicDamageInstruction<>());
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<RangeWorldMinibossDetail, DamageInstruction<RangeWorldMinibossDetail>> process(LocalControllable<RangeWorldMinibossDetail> controllable, LocalEntity entity, AttackDamage attackDamage) {
                LivingEntity boss = (LivingEntity) BukkitUtil.getBukkitEntity(controllable);
                EntityUtil.heal(boss, attackDamage.getDamage());

                LivingEntity attacked = (LivingEntity) BukkitUtil.getBukkitEntity(entity);
                attacked.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 15, 0));
                attacked.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 15, 0));
                return null;
            }
        });
    }

    @Override
    public void spawnBoss(Location spawnLoc, Player target, int worldLevel) {
        Spider entity = spawnLoc.getWorld().spawn(spawnLoc, Spider.class);
        miniBoss.bind(entity, worldLevel);
        entity.setTarget(target);
    }
}

