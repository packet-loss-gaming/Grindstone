/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.util.EntityUtil;
import gg.packetloss.openboss.bukkit.BukkitBossDeclaration;
import gg.packetloss.openboss.bukkit.entity.BukkitBoss;
import gg.packetloss.openboss.bukkit.util.BukkitUtil;
import gg.packetloss.openboss.entity.EntityDetail;
import gg.packetloss.openboss.entity.LocalControllable;
import gg.packetloss.openboss.entity.LocalEntity;
import gg.packetloss.openboss.instruction.BindInstruction;
import gg.packetloss.openboss.instruction.DamagedInstruction;
import gg.packetloss.openboss.instruction.InstructionResult;
import gg.packetloss.openboss.instruction.SimpleInstructionDispatch;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import java.util.List;

public class DebugCow {
    private BukkitBossDeclaration<CowDetail> debugCow;

    public DebugCow() {
        debugCow = new BukkitBossDeclaration<>(CommandBook.inst(), new SimpleInstructionDispatch<>()) {
            @Override
            public boolean matchesBind(LocalEntity entity) {
                Entity boss = BukkitUtil.getBukkitEntity(entity);
                return boss instanceof Cow && EntityUtil.nameMatches(boss, "Bugsie");
            }
        };
        setupDebugCow();
    }

    public void bind(Damageable entity, double maxHealth) {
        debugCow.bind(new BukkitBoss<>(entity, new CowDetail(maxHealth)));
    }

    private void setupDebugCow() {
        List<BindInstruction<CowDetail>> bindInstructions = debugCow.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<CowDetail, BindInstruction<CowDetail>> process(LocalControllable<CowDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Cow) {
                    anEntity.setCustomName("Bugsie");

                    double maxHealth = controllable.getDetail().getMaxHealth();
                    ((Cow) anEntity).setMaxHealth(maxHealth);
                    ((Cow) anEntity).setHealth(maxHealth);
                }
                return null;
            }
        });

        List<DamagedInstruction<CowDetail>> damagedInstructions = debugCow.damagedInstructions;
        damagedInstructions.add(new HealthPrint<>());
    }

    public static class CowDetail implements EntityDetail {

        private double maxHealth;

        public CowDetail(double maxHealth) {
            this.maxHealth = maxHealth;
        }

        public double getMaxHealth() {
            return maxHealth;
        }
    }
}

