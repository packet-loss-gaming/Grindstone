/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.dropttable.BoundDropSpawner;
import gg.packetloss.grindstone.util.dropttable.OSBLKillInfo;
import gg.packetloss.grindstone.util.dropttable.PerformanceDropTable;
import gg.packetloss.grindstone.util.dropttable.PerformanceKillInfo;
import gg.packetloss.openboss.bukkit.BukkitBossDeclaration;
import gg.packetloss.openboss.bukkit.entity.BukkitBoss;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.instruction.SimpleInstructionDispatch;
import gg.packetloss.openboss.instruction.UnbindInstruction;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import static gg.packetloss.grindstone.world.type.range.worldlevel.DemonicRuneListener.DemonicRuneState.fromMonsterKill;

public class GenericRangedWorldMonster {
    private static class GenericMonsterHandler extends BukkitBossDeclaration<GenericDetail> {
        private final WorldLevelComponent worldLevelComponent;

        public GenericMonsterHandler(WorldLevelComponent worldLevelComponent) {
            super(CommandBook.inst(), new SimpleInstructionDispatch<>());
            this.worldLevelComponent = worldLevelComponent;
        }

        public void bind(Mob entity) {
            bind(new BukkitBoss<>(entity, new GenericDetail()));
        }

        @Override
        public boolean matchesBind(LocalEntity entity) {
            Entity boss = BukkitUtil.getBukkitEntity(entity);
            return worldLevelComponent.hasScaledHealth(boss);
        }

        @Override
        public LocalControllable<GenericDetail> tryRebind(LocalEntity entity) {
            var boss = new BukkitBoss<>((Mob) BukkitUtil.getBukkitEntity(entity), new GenericDetail());
            silentBind(boss);
            return boss;
        }
    }

    private final PerformanceDropTable dropTable = new PerformanceDropTable();
    private final WorldLevelComponent worldLevelComponent;
    private final GenericMonsterHandler genericEntity;

    public GenericRangedWorldMonster(WorldLevelComponent worldLevelComponent) {
        this.worldLevelComponent = worldLevelComponent;
        this.genericEntity = new GenericMonsterHandler(worldLevelComponent);

        setupDropTable();
        setupGenericRangedWorldMob();
    }

    private double getModifierByType(EntityType type) {
        WorldLevelConfig config = worldLevelComponent.getConfig();
        return switch (type) {
            case CREEPER -> config.mobsDropTableTypeModifiersCreeper;
            case ENDERMITE -> config.mobsDropTableTypeModifiersEndermite;
            case SILVERFISH -> config.mobsDropTableTypeModifiersSilverfish;
            case WITHER -> config.mobsDropTableTypeModifiersWither;
            default -> config.mobsDropTableTypeModifiersDefault;
        };
    }

    private void setupDropTable() {
        dropTable.registerSlicedDrop(
            (info) -> 1,
            (info, consumer) -> {
                Player player = info.getPlayer();

                int level = worldLevelComponent.getWorldLevel(player);
                if (level == 1) {
                    return;
                }

                EntityType killedEntityType =  info.getKillInfo().getKilled().getType();
                double typeModifier = getModifierByType(killedEntityType);

                PerformanceKillInfo killInfo = info.getKillInfo();
                float percentDamageDone = killInfo.getPercentDamageDone(player).orElseThrow();

                // Handle "live dropped" drops
                if (level > 10) {
                    int dropCountModifier = worldLevelComponent.getDropCountModifier(level, typeModifier, percentDamageDone);
                    double dropValueModifier = worldLevelComponent.getDropValueModifier(level, typeModifier, percentDamageDone);

                    for (int i = 0; i < dropCountModifier; ++i) {
                        if (ChanceUtil.getChance(2000 / dropValueModifier)) {
                            consumer.accept(CustomItemCenter.build(CustomItems.SCROLL_OF_SUMMATION));
                        }
                    }
                }

                ItemStack demonicRune = CustomItemCenter.build(CustomItems.DEMONIC_RUNE);
                DemonicRuneListener.setRuneState(demonicRune, fromMonsterKill(level, typeModifier, percentDamageDone));
                consumer.accept(demonicRune);
            }
        );
    }

    private void setupGenericRangedWorldMob() {
        genericEntity.unbindInstructions.add(new UnbindInstruction<GenericDetail>() {
            @Override
            public InstructionResult<GenericDetail, UnbindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                LivingEntity boss = (LivingEntity) BukkitUtil.getBukkitEntity(controllable);
                new BoundDropSpawner(boss::getLocation).provide(dropTable, new OSBLKillInfo(controllable));

                return null;
            }
        });
    }

    public void bind(Mob entity) {
        genericEntity.bind(entity);
    }
}
