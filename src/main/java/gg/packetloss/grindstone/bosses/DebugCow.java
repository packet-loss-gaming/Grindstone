package gg.packetloss.grindstone.bosses;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.EntityDetail;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.BindInstruction;
import com.skelril.OSBL.instruction.DamagedInstruction;
import com.skelril.OSBL.instruction.InstructionResult;
import com.skelril.OSBL.instruction.SimpleInstructionDispatch;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Server;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.logging.Logger;

public class DebugCow {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private BukkitBossDeclaration<CowDetail> debugCow;

    public DebugCow() {
        debugCow = new BukkitBossDeclaration<>(inst, new SimpleInstructionDispatch<>()) {
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

