/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.MirageArena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import gg.packetloss.grindstone.city.engine.area.AreaListener;
import gg.packetloss.grindstone.events.apocalypse.GemOfLifeUsageEvent;
import gg.packetloss.grindstone.events.custom.item.SpecialAttackEvent;
import gg.packetloss.grindstone.items.specialattack.SpecialAttack;
import gg.packetloss.grindstone.items.specialattack.attacks.ranged.fear.Disarm;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ItemCondenser;
import gg.packetloss.grindstone.util.extractor.entity.CombatantExtractor;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.player.PlayerState;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class MirageArenaListener extends AreaListener<MirageArena> {

  private static Set<Class> blacklistedSpecs = new HashSet<>();
  private static CombatantExtractor<Player, Player, Projectile> extractor = new CombatantExtractor<>(
      Player.class,
      Player.class,
      Projectile.class
  );
  private static ItemCondenser goldCondenser = new ItemCondenser();

  static {
    blacklistedSpecs.add(Disarm.class);
  }

  static {
    goldCondenser.addSupport(new ItemStack(ItemID.GOLD_NUGGET, 9), new ItemStack(ItemID.GOLD_BAR, 1));
    goldCondenser.addSupport(new ItemStack(ItemID.GOLD_BAR, 9), new ItemStack(BlockID.GOLD_BLOCK, 1));
  }

  private final CommandBook inst = CommandBook.inst();
  private final Logger log = CommandBook.logger();
  private final Server server = CommandBook.server();

  public MirageArenaListener(MirageArena parent) {
    super(parent);
  }

  @EventHandler(ignoreCancelled = true)
  public void onSpecialAttack(SpecialAttackEvent event) {

    SpecialAttack attack = event.getSpec();

    if (!parent.contains(attack.getLocation())) {
      return;
    }

    if (blacklistedSpecs.contains(attack.getClass())) {

      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onGemOfLifeUsage(GemOfLifeUsageEvent event) {

    if (parent.contains(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockChangePreLog(BlockChangePreLogEvent event) {

    if (parent.contains(event.getLocation())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    if (event instanceof EntityDamageByEntityEvent) {
      onPvP((EntityDamageByEntityEvent) event);
      return;
    }

    if (parent.editing) {
      event.setCancelled(true);
    }
  }

  public void onPvP(EntityDamageByEntityEvent event) {
    CombatantPair<Player, Player, Projectile> result = extractor.extractFrom(event);

    if (result == null) {
      return;
    }

    if (!parent.scope.checkFor(result.getAttacker(), result.getDefender())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void dropGold(EntityDamageByEntityEvent event) {
    CombatantPair<Player, Player, Projectile> result = extractor.extractFrom(event);

    if (result == null || !parent.contains(result.getDefender())) {
      return;
    }

    MirageArenaConfig config = parent.getConfig();
    MirageSession session = parent.sessions.getSession(MirageSession.class, result.getDefender());
    session.addDamage(Math.min(config.goldCap, ChanceUtil.getRandom(event.getFinalDamage())));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onItemPickup(PlayerPickupItemEvent event) {

    final Player player = event.getPlayer();
    ItemStack itemStack = event.getItem().getItemStack();

    if (itemStack.getTypeId() == ItemID.GOLD_BAR || itemStack.getTypeId() == ItemID.GOLD_NUGGET) {

      if (!parent.contains(player)) {
        return;
      }

      server.getScheduler().runTaskLater(inst, () -> {
        ItemStack[] result = goldCondenser.operate(player.getInventory().getContents(), true);
        if (result != null) {
          player.getInventory().setContents(result);
          //noinspection deprecation
          player.updateInventory();
        }
      }, 1);
    }
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {

    HashMap<String, PlayerState> playerState = parent.playerState;

    Player player = event.getEntity();
    if (!playerState.containsKey(player.getName()) && parent.contains(player) && !parent.admin.isAdmin(player)) {

      Location loc = player.getLocation();
      World world = loc.getWorld();
      MirageArenaConfig config = parent.getConfig();
      MirageSession session = parent.sessions.getSession(MirageSession.class, player);

      int lowEnd, highEnd;

      if (ModifierComponent.getModifierCenter().isActive(ModifierType.NONUPLE_MIRAGE_GOLD)) {
        lowEnd = ItemID.GOLD_BAR;
        highEnd = BlockID.GOLD_BLOCK;
      } else {
        lowEnd = ItemID.GOLD_NUGGET;
        highEnd = ItemID.GOLD_BAR;
      }

      for (double i = session.getDamage(); i > 0; --i) {
        if (ChanceUtil.getChance(config.goldBarChance)) {
          world.dropItem(loc, new ItemStack(highEnd));
        }
        world.dropItem(loc, new ItemStack(lowEnd));
      }

      session.resetDamage();

      Player killer = player.getKiller();
      if (killer != null) {
        killer.setHealth(killer.getMaxHealth());
      }

      playerState.put(player.getName(), new PlayerState(player.getName(),
          player.getInventory().getContents(),
          player.getInventory().getArmorContents(),
          player.getLevel(),
          player.getExp()));
      event.getDrops().clear();
      event.setDroppedExp(0);
      event.setKeepLevel(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerRespawn(PlayerRespawnEvent event) {

    HashMap<String, PlayerState> playerState = parent.playerState;

    Player player = event.getPlayer();
    final Location fallBack = event.getRespawnLocation();

    // Restore their inventory if they have one stored
    if (playerState.containsKey(player.getName()) && !parent.admin.isAdmin(player)) {

      try {
        PlayerState identity = playerState.get(player.getName());

        // Restore the contents
        player.getInventory().setArmorContents(identity.getArmourContents());
        player.getInventory().setContents(identity.getInventoryContents());
        player.setLevel(identity.getLevel());
        player.setExp(identity.getExperience());
      } catch (Exception e) {
        e.printStackTrace();
        event.setRespawnLocation(fallBack);
      } finally {
        playerState.remove(player.getName());
      }
    }
  }
}
