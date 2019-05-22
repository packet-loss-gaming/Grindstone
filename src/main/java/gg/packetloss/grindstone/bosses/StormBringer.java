/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.bosses;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.BukkitBossDeclaration;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitAttackDamage;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import gg.packetloss.grindstone.bosses.detail.WBossDetail;
import gg.packetloss.grindstone.bosses.instruction.HealthPrint;
import gg.packetloss.grindstone.bosses.instruction.WDamageModifier;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.EntityUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StormBringer {
  private final CommandBook inst = CommandBook.inst();
  private final Logger log = inst.getLogger();
  private final Server server = CommandBook.server();

  private BukkitBossDeclaration<WBossDetail> stormBringer;

  public StormBringer() {
    stormBringer = new BukkitBossDeclaration<WBossDetail>(inst, new SimpleInstructionDispatch<>()) {
      @Override
      public boolean matchesBind(LocalEntity entity) {
        return EntityUtil.nameMatches(BukkitUtil.getBukkitEntity(entity), "Storm Bringer");
      }
    };
    setupFangz();
  }

  public void bind(Damageable entity, WBossDetail detail) {
    stormBringer.bind(new BukkitBoss<>(entity, detail));
  }

  private void setupFangz() {
    List<BindInstruction<WBossDetail>> bindInstructions = stormBringer.bindInstructions;
    bindInstructions.add(new BindInstruction<WBossDetail>() {
      @Override
      public InstructionResult<WBossDetail, BindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
        Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
        if (anEntity instanceof LivingEntity) {
          anEntity.setCustomName("Storm Bringer");
          int level = controllable.getDetail().getLevel();
          ((LivingEntity) anEntity).setMaxHealth(20 * 30 * level);
          ((LivingEntity) anEntity).setHealth(20 * 30 * level);
        }
        return null;
      }
    });

    List<UnbindInstruction<WBossDetail>> unbindInstructions = stormBringer.unbindInstructions;
    unbindInstructions.add(new UnbindInstruction<WBossDetail>() {
      @Override
      public InstructionResult<WBossDetail, UnbindInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable) {
        Entity boss = BukkitUtil.getBukkitEntity(controllable);
        Location target = boss.getLocation();
        int baseLevel = controllable.getDetail().getLevel();
        List<ItemStack> itemStacks = new ArrayList<>();
        itemStacks.add(CustomItemCenter.build(CustomItems.BAT_BOW));
        for (int i = baseLevel * ChanceUtil.getRandom(3); i > 0; --i) {
          itemStacks.add(CustomItemCenter.build(CustomItems.BARBARIAN_BONE));
        }
        for (int i = baseLevel * ChanceUtil.getRandom(9); i > 0; --i) {
          itemStacks.add(CustomItemCenter.build(CustomItems.GOD_FISH));
        }
        if (ModifierComponent.getModifierCenter().isActive(ModifierType.DOUBLE_WILD_DROPS)) {
          itemStacks.addAll(itemStacks.stream().map(ItemStack::clone).collect(Collectors.toList()));
        }
        for (ItemStack itemStack : itemStacks) {
          target.getWorld().dropItem(target, itemStack);
        }
        return null;
      }
    });

    List<DamageInstruction<WBossDetail>> damageInstructions = stormBringer.damageInstructions;
    damageInstructions.add(new WDamageModifier());
    damageInstructions.add(new DamageInstruction<WBossDetail>() {
      @Override
      public InstructionResult<WBossDetail, DamageInstruction<WBossDetail>> process(LocalControllable<WBossDetail> controllable, LocalEntity entity, AttackDamage damage) {
        Entity boss = BukkitUtil.getBukkitEntity(controllable);
        Entity eToHit = BukkitUtil.getBukkitEntity(entity);
        if (!(eToHit instanceof LivingEntity) || !getEvent(damage).getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) {
          return null;
        }
        LivingEntity toHit = (LivingEntity) eToHit;

        Location target = toHit.getLocation();
        for (int i = controllable.getDetail().getLevel() * ChanceUtil.getRangedRandom(1, 10); i >= 0; --i) {
          server.getScheduler().runTaskLater(inst, () -> {
            // Simulate a lightning strike
            LightningStrike strike = target.getWorld().strikeLightningEffect(target);
            for (Entity e : strike.getNearbyEntities(2, 4, 2)) {
              if (!e.isValid() || !(e instanceof LivingEntity)) {
                continue;
              }
              // Pig Zombie
              if (e instanceof Pig) {
                e.getWorld().spawn(e.getLocation(), PigZombie.class);
                e.remove();
                continue;
              }
              // Creeper
              if (e instanceof Creeper) {
                ((Creeper) e).setPowered(true);
              }
              ((LivingEntity) e).damage(1, boss);
            }
          }, (5 * (6 + i)));
        }
        return null;
      }
    });

    List<DamagedInstruction<WBossDetail>> damagedInstructions = stormBringer.damagedInstructions;
    damagedInstructions.add(new HealthPrint<>());
  }

  private EntityDamageEvent getEvent(AttackDamage damage) {
    if (damage instanceof BukkitAttackDamage) {
      return ((BukkitAttackDamage) damage).getBukkitEvent();
    }
    return null;
  }
}
