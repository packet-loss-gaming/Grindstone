/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel.miniboss;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.bosses.instruction.PerformanceDropTableUnbind;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.dropttable.PerformanceKillInfo;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.world.type.range.worldlevel.*;
import gg.packetloss.grindstone.world.type.range.worldlevel.demonicrune.DemonicRuneState;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.instruction.RangeWorldMinibossBasicDamageInstruction;
import gg.packetloss.grindstone.world.type.range.worldlevel.miniboss.instruction.RangeWorldMinibossBindInstruction;
import gg.packetloss.openboss.bukkit.entity.BukkitEntity;
import gg.packetloss.openboss.bukkit.util.BukkitAttackDamage;
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
import org.bukkit.entity.Skeleton;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class StormBringer implements RangeWorldMinibossSpawner {
    private final WorldLevelComponent worldLevelComponent;

    private static final String BOUND_NAME = "Storm Bringer";
    private final PerformanceDropTable dropTable = new PerformanceDropTable();
    private final RangeWorldMinibossDeclaration<Skeleton> miniBoss;

    public StormBringer(WorldLevelComponent worldLevelComponent) {
        this.worldLevelComponent = worldLevelComponent;
        miniBoss = new RangeWorldMinibossDeclaration<>(worldLevelComponent, BOUND_NAME, Skeleton.class);
        setupDropTable();
        setupBoss();
    }

    private EntityDamageEvent getEvent(AttackDamage damage) {
        if (damage instanceof BukkitAttackDamage) {
            return ((BukkitAttackDamage) damage).getBukkitEvent();
        }
        return null;
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
                        getConfig().miniBossStormBringerDropTableTypeModifier,
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

        List<UnbindInstruction<RangeWorldMinibossDetail>> unbindInstructions = miniBoss.unbindInstructions;
        unbindInstructions.add(new PerformanceDropTableUnbind<>(dropTable));

        List<DamageInstruction<RangeWorldMinibossDetail>> damageInstructions = miniBoss.damageInstructions;
        damageInstructions.add(new RangeWorldMinibossBasicDamageInstruction<>());
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<RangeWorldMinibossDetail, DamageInstruction<RangeWorldMinibossDetail>> process(LocalControllable<RangeWorldMinibossDetail> controllable, LocalEntity entity, AttackDamage attackDamage) {
                Entity eToHit = BukkitUtil.getBukkitEntity(entity);
                if (!(eToHit instanceof LivingEntity toHit)) {
                    return null;
                }

                EntityDamageEvent event = getEvent(attackDamage);
                if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
                    return null;
                }

                Location target = toHit.getLocation();
                for (int i = controllable.getDetail().getLevel() * ChanceUtil.getRangedRandom(1, 10); i >= 0; --i) {
                    CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                        // Simulate a lightning strike
                        target.getWorld().strikeLightningEffect(target);
                        for (Player p : target.getNearbyEntitiesByType(Player.class, 2, 4, 2)) {
                            if (!GeneralPlayerUtil.hasDamageableGamemode(p)) {
                                continue;
                            }

                            toHit.setHealth((toHit.getHealth() / 8));
                        }
                    }, (5L * (6 + i)));
                }
                return null;
            }
        });
    }

    @Override
    public void spawnBoss(Location spawnLoc, Player target, int worldLevel) {
        Skeleton skel = spawnLoc.getWorld().spawn(spawnLoc, Skeleton.class);
        miniBoss.bind(skel, worldLevel);
        skel.setTarget(target);
    }
}
