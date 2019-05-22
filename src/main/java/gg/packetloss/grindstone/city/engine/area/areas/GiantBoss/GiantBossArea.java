/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.GiantBoss;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.city.engine.area.PersistentArena;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.exceptions.UnsupportedPrayerException;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.util.APIUtil;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.player.PlayerState;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import gg.packetloss.grindstone.util.timer.TimerUtil;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@ComponentInformation(friendlyName = "Giant Boss", desc = "Giant, and a true boss")
@Depend(components = {AdminComponent.class, PrayerComponent.class}, plugins = {"WorldGuard"})
public class GiantBossArea extends AreaComponent<GiantBossConfig> implements PersistentArena {

  protected static final int groundLevel = 82;
  protected static final double scalOffst = 3;
  public final int OPTION_COUNT = 9;
  @InjectComponent
  protected AdminComponent admin;
  @InjectComponent
  protected PrayerComponent prayer;
  protected ProtectedRegion eastDoor, westDoor;
  protected Giant boss = null;
  protected long lastAttack = 0;
  protected int lastAttackNumber = -1;
  protected long lastDeath = 0;
  protected boolean damageHeals = false;
  protected BukkitTask mobDestroyer;
  protected Random random = new Random();
  protected boolean flagged = false;
  protected double toHeal = 0;
  protected int difficulty = Difficulty.HARD.getValue();
  protected List<Location> spawnPts = new ArrayList<>();
  public Runnable spawnXP = () -> {
    for (Location pt : spawnPts) {
      if (!ChanceUtil.getChance(6)) {
        continue;
      }
      ThrownExpBottle bottle = getWorld().spawn(pt, ThrownExpBottle.class);
      bottle.setVelocity(new Vector(
          random.nextDouble() * 1.7 - 1.5,
          random.nextDouble() * 1.5,
          random.nextDouble() * 1.7 - 1.5)
      );
    }
  };
  protected List<Location> chestPts = new ArrayList<>();
  protected HashMap<String, PlayerState> playerState = new HashMap<>();
  private BlockFace[] northFaces = new BlockFace[] {
      BlockFace.NORTH, BlockFace.DOWN
  };
  private BlockFace[] southFaces = new BlockFace[] {
      BlockFace.SOUTH, BlockFace.DOWN
  };

  @Override
  public void setUp() {
    try {
      WorldGuardPlugin WG = APIUtil.getWorldGuard();
      world = server.getWorlds().get(0);
      RegionManager manager = WG.getRegionManager(world);
      region = manager.getRegion("vineam-district-giant-boss-area");
      eastDoor = manager.getRegion("vineam-district-giant-boss-east-door");
      westDoor = manager.getRegion("vineam-district-giant-boss-west-door");
      tick = 4 * 20;
      listener = new GiantBossListener(this);
      config = new GiantBossConfig();

      mobDestroyer = server.getScheduler().runTaskTimer(inst, () -> {
        Collection<Entity> contained = getContained(1, Zombie.class, ExperienceOrb.class);
        if (!getWorld().isThundering()) {
          removeOutsideZombies(contained);
        }
        if (isBossSpawned()) {
          buffBabies(contained);
          removeXP(contained);
        }
      }, 0, 20 * 2);
      // First spawn requirement
      probeArea();
      reloadData();
      // Set difficulty
      difficulty = getWorld().getDifficulty().getValue();
    } catch (UnknownPluginException e) {
      log.info("WorldGuard could not be found!");
    }
  }

  @Override
  public void enable() {
    server.getScheduler().runTaskLater(inst, super::enable, 1);
  }

  @Override
  public void disable() {
    writeData(false);
    removeMobs();
  }

  @Override
  public void run() {
    if (!isBossSpawned()) {
      if (lastDeath == 0 || System.currentTimeMillis() - lastDeath >= 1000 * 60 * 3) {
        removeMobs();
        spawnBoss();
      }
    } else if (!isEmpty()) {
      equalize();
      runAttack(ChanceUtil.getRandom(OPTION_COUNT));
    }
    writeData(true);
  }

  public boolean isBossSpawned() {
    if (!isArenaLoaded()) {
      return true;
    }
    boolean found = false;
    boolean second = false;
    for (Giant e : getContained(Giant.class)) {
      if (e.isValid()) {
        if (!found) {
          boss = e;
          found = true;
        } else if (e.getHealth() < boss.getHealth()) {
          boss = e;
          second = true;
        } else {
          e.remove();
        }
      }
    }
    if (second) {
      getContained(Giant.class).stream().filter(e -> e.isValid() && !e.equals(boss)).forEach(Entity::remove);
    }
    return boss != null && boss.isValid();
  }

  public boolean isArenaLoaded() {
    BlockVector min = getRegion().getMinimumPoint();
    BlockVector max = getRegion().getMaximumPoint();
    Region region = new CuboidRegion(min, max);
    return BukkitUtil.toLocation(getWorld(), region.getCenter()).getChunk().isLoaded();
  }

  public void spawnBoss() {
    BlockVector min = getRegion().getMinimumPoint();
    BlockVector max = getRegion().getMaximumPoint();
    Region region = new CuboidRegion(min, max);
    Location l = BukkitUtil.toLocation(getWorld(), region.getCenter().setY(groundLevel));
    boss = getWorld().spawn(l, Giant.class);
    boss.setMaxHealth(510 + (difficulty * 80));
    boss.setHealth(510 + (difficulty * 80));
    boss.setRemoveWhenFarAway(false);
    try {
      AttributeBook.setAttribute(boss, AttributeBook.Attribute.KNOCKBACK_RESISTANCE, 1);
      AttributeBook.setAttribute(boss, AttributeBook.Attribute.FOLLOW_RANGE, 40);
    } catch (UnsupportedFeatureException ex) {
      log.warning("Boss NMS attributes not properly set.");
    }
    setDoor(eastDoor, BlockID.SANDSTONE, 1);
    setDoor(westDoor, BlockID.SANDSTONE, 1);
    for (Player player : getContained(1, Player.class)) {
      ChatUtil.sendWarning(player, "I live again!");
    }
  }

  public void printBossHealth() {
    int current = (int) Math.ceil(boss.getHealth());
    int max = (int) Math.ceil(boss.getMaxHealth());
    String message = "Boss Health: " + current + " / " + max;
    ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_AQUA, message);
  }

  public void probeArea() {
    spawnPts.clear();
    chestPts.clear();
    BlockVector min = getRegion().getParent().getMinimumPoint();
    BlockVector max = getRegion().getParent().getMaximumPoint();
    int minX = min.getBlockX();
    int minZ = min.getBlockZ();
    int minY = min.getBlockY();
    int maxX = max.getBlockX();
    int maxZ = max.getBlockZ();
    int maxY = max.getBlockY();
    BlockState block;
    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        for (int y = maxY; y >= minY; --y) {
          block = getWorld().getBlockAt(x, y, z).getState();
          if (!block.getChunk().isLoaded()) {
            block.getChunk().load();
          }
          if (block.getTypeId() == BlockID.GOLD_BLOCK) {
            spawnPts.add(block.getLocation().add(0, 2, 0));
            continue;
          }
          if (block.getTypeId() == BlockID.CHEST) {
            chestPts.add(block.getLocation());
          }
        }
      }
    }
  }

  public void buffBabies(Collection<? extends Entity> contained) {
    List<Zombie> que = contained.stream()
        .filter(entity -> entity.isValid() && entity instanceof Zombie && ((Zombie) entity).isBaby())
        .map(entity -> (Zombie) entity)
        .collect(Collectors.toList());

    if (que.size() < 45) {
      flagged = false;
      return;
    }
    flagged = true;
    if (que.size() < 250) {
      return;
    }
    for (Zombie zombie : que) {
      zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 20, 3));
    }
  }

  public void removeXP(Collection<? extends Entity> contained) {
    removeXP(contained, false);
  }

  public void removeXP(Collection<? extends Entity> contained, boolean force) {
    contained.stream()
        .filter(e -> e.isValid() && e instanceof ExperienceOrb && (force || e.getTicksLived() > 20 * 13))
        .forEach(Entity::remove);
  }

  public void removeMobs() {
    getContained(1, Monster.class).forEach(e -> {
      for (int i = 0; i < 20; i++) {
        getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
      }
      e.remove();
    });
  }

  public void removeOutsideZombies(Collection<? extends Entity> contained) {
    contained.stream()
        .filter(e -> e instanceof Zombie && ((Zombie) e).isBaby() && !contains(e))
        .forEach(e -> {
          for (int i = 0; i < 20; i++) {
            getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
          }
              e.remove();
            }
        );
  }

  public void equalize() {
    // Equalize Boss
    int diff = getWorld().getDifficulty().getValue();
    if (getWorld().isThundering()) {
      difficulty = diff + diff;
    } else {
      difficulty = diff;
    }
    double oldMaxHealth = boss.getMaxHealth();
    double newMaxHealth = 510 + (difficulty * 80);
    if (newMaxHealth > oldMaxHealth) {
      boss.setMaxHealth(newMaxHealth);
      boss.setHealth(Math.min(boss.getHealth() + (newMaxHealth - oldMaxHealth), newMaxHealth));
    } else if (newMaxHealth != oldMaxHealth) {
      boss.setHealth(Math.min(boss.getHealth() + (oldMaxHealth - newMaxHealth), newMaxHealth));
      boss.setMaxHealth(newMaxHealth);
    }
    // Equalize Players
    for (Player player : getContained(Player.class)) {
      try {
        admin.standardizePlayer(player);
        if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
          player.damage(32, boss);
        }
        if (player.getVehicle() != null) {
          player.getVehicle().eject();
          ChatUtil.sendWarning(player, "The boss throws you off!");
        }
        if (Math.abs(groundLevel - player.getLocation().getY()) > 10) {
          runAttack(4);
        }
      } catch (Exception e) {
        log.warning("The player: " + player.getName() + " may have an unfair advantage.");
      }
    }
  }

  public void runAttack(int attackCase) {
    int delay = ChanceUtil.getRangedRandom(13000, 17000);
    if (lastAttack != 0 && System.currentTimeMillis() - lastAttack <= delay) {
      return;
    }
    Collection<Player> containedP = getContained(1, Player.class);
    Collection<Player> contained = getContained(Player.class);
    if (contained == null || contained.size() <= 0) {
      return;
    }
    if (attackCase < 1 || attackCase > OPTION_COUNT) {
      attackCase = ChanceUtil.getRandom(OPTION_COUNT);
    }
    // AI system
    if (attackCase != 4) {
      if ((attackCase == 5 || attackCase == 9) && boss.getHealth() > boss.getMaxHealth() * .9) {
        attackCase = ChanceUtil.getChance(2) ? 8 : 2;
      }
      if (flagged && ChanceUtil.getChance(2)) {
        attackCase = ChanceUtil.getChance(2) ? 4 : 7;
      }
      for (Player player : contained) {
        if (player.getHealth() < 4) {
          attackCase = 2;
          break;
        }
      }
      if (boss.getHealth() < boss.getMaxHealth() * .3 && ChanceUtil.getChance(2)) {
        attackCase = 9;
      }
      if (((attackCase == 3 || attackCase == 6) && boss.getHealth() < boss.getMaxHealth() * .3) || (attackCase == 7 && contained.size() < 2)) {
        runAttack(ChanceUtil.getRandom(OPTION_COUNT));
        return;
      }
    }
    switch (attackCase) {
      case 1:
        ChatUtil.sendWarning(containedP, "Taste my wrath!");
        for (Player player : contained) {
          // Call this event to notify AntiCheat
          server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
          player.setVelocity(new Vector(
              random.nextDouble() * 3 - 1.5,
              random.nextDouble() * 1 + 1.3,
              random.nextDouble() * 3 - 1.5
          ));
          player.setFireTicks(difficulty * 18);
        }
        break;
      case 2:
        ChatUtil.sendWarning(containedP, "Embrace my corruption!");
        for (Player player : contained) {
          player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 12, difficulty > 2 ? 1 : 0));
        }
        break;
      case 3:
        ChatUtil.sendWarning(containedP, "Are you BLIND? Mwhahahaha!");
        for (Player player : contained) {
          player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 0));
        }
        break;
      case 4:
        ChatUtil.sendWarning(containedP, ChatColor.DARK_RED + "Tango time!");
        server.getScheduler().runTaskLater(inst, () -> {
          if (!isBossSpawned()) {
            return;
          }
          for (Player player : getContained(Player.class)) {
            if (boss.hasLineOfSight(player)) {
              ChatUtil.sendNotice(player, "Come closer...");
              player.teleport(boss.getLocation());
              player.damage(difficulty * 32, boss);
              // Call this event to notify AntiCheat
              server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
              player.setVelocity(new Vector(
                  random.nextDouble() * 1.7 - 1.5,
                  random.nextDouble() * 2,
                  random.nextDouble() * 1.7 - 1.5
              ));
            } else {
              ChatUtil.sendNotice(player, "Fine... No tango this time...");
            }
          }
          ChatUtil.sendNotice(getContained(1, Player.class), "Now wasn't that fun?");
        }, 20 * (difficulty == 1 ? 14 : 7));
        break;
      case 5:
        if (!damageHeals) {
          ChatUtil.sendWarning(containedP, "I am everlasting!");
          damageHeals = true;
          server.getScheduler().runTaskLater(inst, () -> {
            if (damageHeals) {
              damageHeals = false;
              if (!isBossSpawned()) {
                return;
              }
              ChatUtil.sendNotice(getContained(1, Player.class), "Thank you for your assistance.");
            }
          }, 20 * (difficulty * 5));
          break;
        }
        runAttack(ChanceUtil.getRandom(OPTION_COUNT));
        return;
      case 6:
        ChatUtil.sendWarning(containedP, "Fire is your friend...");
        for (Player player : contained) {
          player.setFireTicks(20 * (difficulty * 15));
        }
        break;
      case 7:
        ChatUtil.sendWarning(containedP, ChatColor.DARK_RED + "Bask in my glory!");
        server.getScheduler().runTaskLater(inst, () -> {
          if (!isBossSpawned()) {
            return;
          }
          // Set defaults
          boolean baskInGlory = getContained(Player.class).size() == 0;
          damageHeals = true;
          // Check Players
          for (Player player : getContained(Player.class)) {
            if (boss.hasLineOfSight(player)) {
              ChatUtil.sendWarning(player, ChatColor.DARK_RED + "You!");
              baskInGlory = true;
            }
          }
          //Attack
          if (baskInGlory) {
            int dmgFact = difficulty * 3 + 1;
            spawnPts.stream().filter(pt -> ChanceUtil.getChance(12)).forEach(pt -> {
              getWorld().createExplosion(pt.getX(), pt.getY(), pt.getZ(), dmgFact, false, false);
            });
            //Schedule Reset
            server.getScheduler().runTaskLater(inst, () -> damageHeals = false, 10);
            return;
          }
          // Notify if avoided
          ChatUtil.sendNotice(getContained(1, Player.class), "Gah... Afraid are you friends?");
        }, 20 * (difficulty == 1 ? 14 : 7));
        break;
      case 8:
        ChatUtil.sendWarning(containedP, ChatColor.DARK_RED + "I ask thy lord for aid in this all mighty battle...");
        ChatUtil.sendWarning(containedP, ChatColor.DARK_RED + "Heed thy warning, or perish!");
        server.getScheduler().runTaskLater(inst, () -> {
          if (!isBossSpawned()) {
            return;
          }
          ChatUtil.sendWarning(getContained(1, Player.class), "May those who appose me die a death like no other...");
          getContained(Player.class).stream().filter(boss::hasLineOfSight).forEach(player -> {
            ChatUtil.sendWarning(getContained(1, Player.class), "Perish " + player.getName() + "!");
            try {
              prayer.influencePlayer(player, PrayerComponent.constructPrayer(player, PrayerType.DOOM, 120000));
            } catch (UnsupportedPrayerException e) {
              e.printStackTrace();
            }
          });
        }, 20 * (difficulty == 1 ? 14 : 7));
        break;
      case 9:
        ChatUtil.sendNotice(containedP, ChatColor.DARK_RED, "My minions our time is now!");
        IntegratedRunnable minionEater = new IntegratedRunnable() {
          @Override
          public boolean run(int times) {
            if (!isBossSpawned()) {
              return true;
            }
            for (LivingEntity entity : getContained(LivingEntity.class)) {
              if (entity instanceof Giant || !ChanceUtil.getChance(5)) {
                continue;
              }
              double realDamage = entity.getHealth();
              if (entity instanceof Zombie && ((Zombie) entity).isBaby()) {
                entity.setHealth(0);
              } else {
                entity.damage(realDamage, boss);
              }
              toHeal += realDamage * difficulty * .09;
            }
            if (TimerUtil.matchesFilter(times + 1, -1, 2)) {
              ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_AQUA, "The boss has drawn in: " + (int) toHeal + " health.");
            }
            return true;
          }

          @Override
          public void end() {
            if (!isBossSpawned()) {
              return;
            }
            boss.setHealth(Math.min(toHeal + boss.getHealth(), boss.getMaxHealth()));
            toHeal = 0;
            ChatUtil.sendNotice(getContained(1, Player.class), "Thank you my minions!");
            printBossHealth();
          }
        };
        TimedRunnable minonEatingTask = new TimedRunnable(minionEater, 20);
        BukkitTask minionEatingTaskExecutor = server.getScheduler().runTaskTimer(inst, minonEatingTask, 0, 10);
        minonEatingTask.setTask(minionEatingTaskExecutor);
        break;
    }
    lastAttack = System.currentTimeMillis();
    lastAttackNumber = attackCase;
  }

  @Override
  public void writeData(boolean doAsync) {
    Runnable run = () -> {
      respawnsFile:
      {
        File playerStateFile = new File(getWorkingDir().getPath() + "/respawns.dat");
        if (playerStateFile.exists()) {
          Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);

          if (playerState.equals(playerStateFileO)) {
            break respawnsFile;
          }
        }
        IOUtil.toBinaryFile(getWorkingDir(), "respawns", playerState);
      }
    };
    if (doAsync) {
      server.getScheduler().runTaskAsynchronously(inst, run);
    } else {
      run.run();
    }
  }

  @Override
  public void reloadData() {
    File playerStateFile = new File(getWorkingDir().getPath() + "/respawns.dat");
    if (playerStateFile.exists()) {
      Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
      if (playerStateFileO instanceof HashMap) {
        //noinspection unchecked
        playerState = (HashMap<String, PlayerState>) playerStateFileO;
        log.info("Loaded: " + playerState.size() + " respawn records for the Giant Boss.");
      } else {
        log.warning("Invalid block record file encountered: " + playerStateFile.getName() + "!");
        log.warning("Attempting to use backup file...");
        playerStateFile = new File(getWorkingDir().getPath() + "/old-" + playerStateFile.getName());
        if (playerStateFile.exists()) {
          playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
          if (playerStateFileO instanceof HashMap) {
            //noinspection unchecked
            playerState = (HashMap<String, PlayerState>) playerStateFileO;
            log.info("Backup file loaded successfully!");
            log.info("Loaded: " + playerState.size() + " respawn records for the Giant Boss.");
          } else {
            log.warning("Backup file failed to load!");
          }
        }
      }
    }
  }

  public void setDoor(final ProtectedRegion door, int type, int data) {
    Block sideOne = BukkitUtil.toLocation(world, door.getMaximumPoint()).getBlock();
    Block sideTwo = BukkitUtil.toLocation(world, door.getMinimumPoint().setY(sideOne.getY())).getBlock();

    doNextDoorBlock(door, sideOne, true, type, data, 1);
    doNextDoorBlock(door, sideTwo, false, type, data, 1);
  }

  private void doNextDoorBlock(ProtectedRegion limit, Block block, boolean north, int newType, int data, int depth) {
    if (!LocationUtil.isInRegion(limit, block.getLocation())) {
      return;
    }
    for (BlockFace face : (north ? northFaces : southFaces)) {
      doNextDoorBlock(limit, block.getRelative(face), north, newType, data, depth + 1);
    }
    server.getScheduler().runTaskLater(inst, () -> block.setTypeIdAndData(newType, (byte) data, true), 9 * depth);
  }
}
