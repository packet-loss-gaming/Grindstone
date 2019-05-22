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
import gg.packetloss.grindstone.bosses.instruction.ExplosiveUnbind;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.logging.Logger;

public class ThunderZombie {

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = inst.getLogger();
  private final Server server = CommandBook.server();

  private BukkitBossDeclaration<GenericDetail> thunderZombie;

  public ThunderZombie() {
    thunderZombie = new BukkitBossDeclaration<GenericDetail>(inst, new SimpleInstructionDispatch<>()) {
      @Override
      public boolean matchesBind(LocalEntity entity) {
        return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Thor Zombie");
      }
    };
    setupThunderZombie();
  }

  public void bind(Damageable entity) {
    thunderZombie.bind(new BukkitBoss<>(entity, new GenericDetail()));
  }

  private void setupThunderZombie() {
    List<BindInstruction<GenericDetail>> bindInstructions = thunderZombie.bindInstructions;
    bindInstructions.add(new BindInstruction<GenericDetail>() {
      @Override
      public InstructionResult<GenericDetail, BindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
        Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
        if (anEntity instanceof LivingEntity) {
          anEntity.setCustomName("Thor Zombie");
          ((LivingEntity) anEntity).setMaxHealth(500);
          ((LivingEntity) anEntity).setHealth(500);
        }
        return null;
      }
    });

    List<UnbindInstruction<GenericDetail>> unbindInstructions = thunderZombie.unbindInstructions;
    unbindInstructions.add(new ExplosiveUnbind<GenericDetail>(false, false) {
      @Override
      public float getExplosionStrength(GenericDetail genericDetail) {
        return 4F;
      }
    });
    unbindInstructions.add(new UnbindInstruction<GenericDetail>() {
      @Override
      public InstructionResult<GenericDetail, UnbindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
        Entity boss = BukkitUtil.getBukkitEntity(controllable);
        Location target = boss.getLocation();
        for (int i = ChanceUtil.getRangedRandom(12, 150); i > 0; --i) {
          target.getWorld().dropItem(target, new ItemStack(Material.GOLD_INGOT));
        }
        return null;
      }
    });

    List<DamageInstruction<GenericDetail>> damageInstructions = thunderZombie.damageInstructions;
    damageInstructions.add(new DamageInstruction<GenericDetail>() {
      @Override
      public InstructionResult<GenericDetail, DamageInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable, LocalEntity entity, AttackDamage damage) {
        Entity boss = BukkitUtil.getBukkitEntity(controllable);
        final Entity toHit = BukkitUtil.getBukkitEntity(entity);
        toHit.setVelocity(boss.getLocation().getDirection().multiply(2));

        server.getScheduler().runTaskLater(inst, () -> {
          final Location targetLocation = toHit.getLocation();
          server.getScheduler().runTaskLater(inst, () -> {
            targetLocation.getWorld().strikeLightning(targetLocation);
          }, 15);
        }, 30);
        return null;
      }
    });

    List<DamagedInstruction<GenericDetail>> damagedInstructions = thunderZombie.damagedInstructions;
    damagedInstructions.add(new HealthPrint<>());
  }
}
