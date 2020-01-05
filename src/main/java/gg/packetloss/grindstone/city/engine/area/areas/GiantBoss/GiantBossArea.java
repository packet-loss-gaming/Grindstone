/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.area.areas.GiantBoss;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.exceptions.UnsupportedPrayerException;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import gg.packetloss.grindstone.util.timer.TimerUtil;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

@ComponentInformation(friendlyName = "Giant Boss", desc = "Giant, and a true boss")
@Depend(components = {AdminComponent.class, PrayerComponent.class, PlayerStateComponent.class}, plugins = {"WorldGuard"})
public class GiantBossArea extends AreaComponent<GiantBossConfig> {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected PrayerComponent prayer;
    @InjectComponent
    protected PlayerStateComponent playerState;

    protected static final int groundLevel = 82;
    protected static final double scalOffst = 3;

    protected ProtectedRegion eastDoor, westDoor;

    protected Giant boss = null;
    protected long lastAttack = 0;
    protected int lastAttackNumber = -1;
    protected long lastDeath = 0;
    protected boolean damageHeals = false;
    protected BukkitTask mobDestroyer;
    protected Random random = new Random();

    protected double toHeal = 0;
    protected List<Location> spawnPts = new ArrayList<>();
    protected List<Location> chestPts = new ArrayList<>();

    @Override
    public void setUp() {
        world = server.getWorlds().get(0);
        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        region = manager.getRegion("vineam-district-giant-boss-area");
        eastDoor = manager.getRegion("vineam-district-giant-boss-east-door");
        westDoor = manager.getRegion("vineam-district-giant-boss-west-door");
        tick = 4 * 20;
        listener = new GiantBossListener(this);
        config = new GiantBossConfig();

        mobDestroyer = server.getScheduler().runTaskTimer(inst, () -> {
            Collection<Entity> contained = getContained(1, Zombie.class, ExperienceOrb.class);
            if (!EnvironmentUtil.hasThunderstorm(getWorld())) removeOutsideZombies(contained);
            if (isBossSpawned()) {
                removeXP(contained);
            }
        }, 0, 20 * 2);
        // First spawn requirement
        probeArea();

        CommandBook.registerEvents(new FlightBlockingListener(admin, this::contains));
    }

    @Override
    public void enable() {
        server.getScheduler().runTaskLater(inst, super::enable, 1);
    }

    @Override
    public void disable() {
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
    }

    @Override
    public Collection<Player> getAudiblePlayers() {
        return getContained(1, Player.class);
    }

    public boolean isBossSpawned() {
        if (!isArenaLoaded()) return true;
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
        return RegionUtil.isLoaded(getWorld(), getRegion());
    }

    public void spawnBoss() {
        Location spawnLoc = RegionUtil.getCenterAt(getWorld(), groundLevel, getRegion());

        boss = getWorld().spawn(spawnLoc, Giant.class);
        boss.setMaxHealth(config.maxHealthNormal);
        boss.setHealth(config.maxHealthNormal);
        boss.setRemoveWhenFarAway(false);

        try {
            AttributeBook.setAttribute(boss, AttributeBook.Attribute.KNOCKBACK_RESISTANCE, 1);
            AttributeBook.setAttribute(boss, AttributeBook.Attribute.FOLLOW_RANGE, 40);
        } catch (UnsupportedFeatureException ex) {
            log.warning("Boss NMS attributes not properly set.");
        }

        setDoor(eastDoor, Material.CHISELED_SANDSTONE);
        setDoor(westDoor, Material.CHISELED_SANDSTONE);

        for (Player player : getAudiblePlayers()) ChatUtil.sendWarning(player, "I live again!");
    }

    public void printBossHealth() {
        int current = (int) Math.ceil(boss.getHealth());
        int max = (int) Math.ceil(boss.getMaxHealth());
        String message = "Boss Health: " + current + " / " + max;
        ChatUtil.sendNotice(getAudiblePlayers(), ChatColor.DARK_AQUA, message);
    }

    public void probeArea() {
        spawnPts.clear();
        chestPts.clear();

        RegionWalker.walk(getRegion().getParent(), (x, y, z) -> {
            BlockState block = getWorld().getBlockAt(x, y, z).getState();
            if (block.getType() == Material.GOLD_BLOCK) {
                spawnPts.add(block.getLocation().add(0, 2, 0));
                return;
            }

            if (block.getType() == Material.CHEST) {
                chestPts.add(block.getLocation());
            }
        });
    }

    public Runnable spawnXP = () -> {
        for (Location pt : spawnPts) {
            if (!ChanceUtil.getChance(6)) continue;
            ThrownExpBottle bottle = getWorld().spawn(pt, ThrownExpBottle.class);
            bottle.setVelocity(new Vector(
                            random.nextDouble() * 1.7 - 1.5,
                            random.nextDouble() * 1.5,
                            random.nextDouble() * 1.7 - 1.5)
            );
        }
    };

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
            for (int i = 0; i < 20; i++) getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
            e.remove();
        });
    }

    public void removeOutsideZombies(Collection<? extends Entity> contained) {
        contained.stream()
                .filter(e -> e instanceof Zombie && ((Zombie) e).isBaby() && !contains(e))
                .forEach(e -> {
                        for (int i = 0; i < 20; i++) getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
                        e.remove();
                    }
                );
    }

    public void equalize() {
        // Equalize Boss
        double oldMaxHealth = boss.getMaxHealth();
        double newMaxHealth = EnvironmentUtil.hasThunderstorm(world)
                ? config.maxHealthThunderstorm : config.maxHealthNormal;

        if (newMaxHealth > oldMaxHealth) {
            boss.setMaxHealth(newMaxHealth);
            boss.setHealth(Math.min(boss.getHealth() + (newMaxHealth - oldMaxHealth), newMaxHealth));
        } else if (newMaxHealth != oldMaxHealth) {
            boss.setHealth(Math.min(boss.getHealth() + (oldMaxHealth - newMaxHealth), newMaxHealth));
            boss.setMaxHealth(newMaxHealth);
        }

        // Equalize Players
        for (Player player : getContainedParticipants()) {
            try {
                if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    player.damage(32, boss);
                }

                if (player.getVehicle() != null) {
                    player.getVehicle().eject();
                    ChatUtil.sendWarning(player, "The boss throws you off!");
                }

                if (Math.abs(groundLevel - player.getLocation().getY()) > 10) runAttack(4);
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    private boolean tryDivineWind(Player player) {
        if (inst.hasPermission(player, "aurora.tome.divinity") && ChanceUtil.getChance(3)) {
            ChatUtil.sendNotice(player, "A divine wind hides you from the boss.");
            return true;
        }

        return false;
    }

    public final int OPTION_COUNT = 9;

    public void runAttack(int attackCase) {
        int delay = ChanceUtil.getRangedRandom(13000, 17000);
        if (lastAttack != 0 && System.currentTimeMillis() - lastAttack <= delay) return;
        Collection<Player> audiblePlayers = getAudiblePlayers();
        Collection<Player> contained = getContainedParticipants();
        if (contained == null || contained.size() <= 0) return;
        if (attackCase < 1 || attackCase > OPTION_COUNT) attackCase = ChanceUtil.getRandom(OPTION_COUNT);

        // AI system
        if ((attackCase == 5 || attackCase == 9) && boss.getHealth() > boss.getMaxHealth() * .7) {
            attackCase = ChanceUtil.getChance(2) ? 8 : 2;
        }

        for (Player player : contained) {
            if (player.getHealth() < 4) {
                attackCase = 2;
                break;
            }
        }

        if (((attackCase == 3 || attackCase == 6) && boss.getHealth() < boss.getMaxHealth() * .3)) {
            runAttack(ChanceUtil.getRandom(OPTION_COUNT));
            return;
        }

        switch (attackCase) {
            case 1:
                ChatUtil.sendWarning(audiblePlayers, "Taste my wrath!");
                for (Player player : contained) {
                    // Call this event to notify AntiCheat
                    server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                    player.setVelocity(new Vector(
                            random.nextDouble() * 3 - 1.5,
                            random.nextDouble() * 1 + 1.3,
                            random.nextDouble() * 3 - 1.5
                    ));
                    player.setFireTicks(20 * 3);
                }
                break;
            case 2:
                ChatUtil.sendWarning(audiblePlayers, "Embrace my corruption!");
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 12, 1));
                }
                break;
            case 3:
                ChatUtil.sendWarning(audiblePlayers, "Are you BLIND? Mwhahahaha!");
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 0));
                }
                break;
            case 4:
                ChatUtil.sendWarning(audiblePlayers, ChatColor.DARK_RED + "Tango time!");
                server.getScheduler().runTaskLater(inst, () -> {
                    if (!isBossSpawned()) return;

                    for (Player player : getContainedParticipants()) {
                        if (tryDivineWind(player)) {
                            continue;
                        }

                        if (boss.hasLineOfSight(player)) {
                            ChatUtil.sendNotice(player, "Come closer...");
                            player.teleport(boss.getLocation());
                            player.damage(100, boss);
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

                    ChatUtil.sendNotice(getAudiblePlayers(), "Now wasn't that fun?");
                }, 20 * 7);
                break;
            case 5:
                if (!damageHeals) {
                    ChatUtil.sendWarning(audiblePlayers, "I am everlasting!");
                    damageHeals = true;
                    server.getScheduler().runTaskLater(inst, () -> {
                        if (damageHeals) {
                            damageHeals = false;
                            if (!isBossSpawned()) return;
                            ChatUtil.sendNotice(getAudiblePlayers(), "Thank you for your assistance.");
                        }
                    }, 20 * 15);
                    break;
                }
                runAttack(ChanceUtil.getRandom(OPTION_COUNT));
                return;
            case 6:
                ChatUtil.sendWarning(audiblePlayers, "Fire is your friend...");
                for (Player player : contained) {
                    player.setFireTicks(20 * 45);
                }
                break;
            case 7:
                ChatUtil.sendWarning(audiblePlayers, ChatColor.DARK_RED + "Bask in my glory!");
                server.getScheduler().runTaskLater(inst, () -> {
                    if (!isBossSpawned()) return;
                    // Set defaults
                    boolean baskInGlory = false;

                    damageHeals = true;

                    // Schedule Reset
                    server.getScheduler().runTaskLater(inst, () -> damageHeals = false, 10);

                    // Check Players
                    for (Player player : getContainedParticipants()) {
                        if (tryDivineWind(player)) {
                            continue;
                        }

                        if (boss.hasLineOfSight(player)) {
                            ChatUtil.sendWarning(player, ChatColor.DARK_RED + "You!");
                            baskInGlory = true;
                        }
                    }

                    // Attack
                    if (baskInGlory) {
                        spawnPts.stream().filter(pt -> ChanceUtil.getChance(12)).forEach(pt -> {
                            ExplosionStateFactory.createExplosion(pt, 10, false, false);
                        });
                        return;
                    }

                    // Notify if avoided
                    ChatUtil.sendNotice(getAudiblePlayers(), "Gah... Afraid are you friends?");
                }, 20 * 7);
                break;
            case 8:
                ChatUtil.sendWarning(audiblePlayers, ChatColor.DARK_RED + "I ask thy lord for aid in this all mighty battle...");
                ChatUtil.sendWarning(audiblePlayers, ChatColor.DARK_RED + "Heed thy warning, or perish!");
                server.getScheduler().runTaskLater(inst, () -> {
                    if (!isBossSpawned()) return;
                    ChatUtil.sendWarning(getAudiblePlayers(), "May those who appose me die a death like no other...");
                    getContainedParticipants().stream().filter(boss::hasLineOfSight).forEach(player -> {
                        ChatUtil.sendWarning(getAudiblePlayers(), "Perish " + player.getName() + "!");
                        try {
                            prayer.influencePlayer(player, PrayerComponent.constructPrayer(player, PrayerType.DOOM, 120000));
                        } catch (UnsupportedPrayerException e) {
                            e.printStackTrace();
                        }
                    });
                }, 20 * 7);
                break;
            case 9:
                ChatUtil.sendNotice(audiblePlayers, ChatColor.DARK_RED, "My minions our time is now!");
                IntegratedRunnable minionEater = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {
                        if (!isBossSpawned()) return true;
                        for (LivingEntity entity : getContained(LivingEntity.class)) {
                            if (entity instanceof Giant || !ChanceUtil.getChance(3)) continue;
                            double realDamage = entity.getHealth() / 2;
                            if (entity instanceof Zombie && ((Zombie) entity).isBaby()) {
                                entity.damage(realDamage);
                            } else if (boss.hasLineOfSight(entity)) {
                                entity.damage(realDamage, boss);
                            }
                            toHeal += realDamage * .27;
                        }
                        if (TimerUtil.matchesFilter(times + 1, -1, 2)) {
                            ChatUtil.sendNotice(getAudiblePlayers(), ChatColor.DARK_AQUA, "The boss has drawn in: " + (int) toHeal + " health.");
                        }
                        return true;
                    }

                    @Override
                    public void end() {
                        if (!isBossSpawned()) return;
                        boss.setHealth(Math.min(toHeal + boss.getHealth(), boss.getMaxHealth()));
                        toHeal = 0;
                        ChatUtil.sendNotice(getAudiblePlayers(), "Thank you my minions!");
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

    public void setDoor(final ProtectedRegion door, Material type) {
        final BlockVector3 min = door.getMinimumPoint();
        final BlockVector3 max = door.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxY = max.getBlockY();
        int maxZ = max.getBlockZ();

        Block sideOne = new Location(world, maxX, maxY, maxZ).getBlock();
        Block sideTwo = new Location(world, minX, maxY, minZ).getBlock();

        doNextDoorBlock(door, sideOne, true, type, 1);
        doNextDoorBlock(door, sideTwo, false, type, 1);
    }

    private BlockFace[] northFaces = new BlockFace[] {
            BlockFace.NORTH, BlockFace.DOWN
    };
    private BlockFace[] southFaces = new BlockFace[] {
            BlockFace.SOUTH, BlockFace.DOWN
    };

    private void doNextDoorBlock(ProtectedRegion limit, Block block, boolean north, Material newType, int depth) {
        if (!LocationUtil.isInRegion(limit, block.getLocation())) return;
        for (BlockFace face : (north ? northFaces : southFaces)) {
            doNextDoorBlock(limit, block.getRelative(face), north, newType, depth + 1);
        }
        server.getScheduler().runTaskLater(inst, () -> block.setType(newType, true), 9 * depth);
    }
}
