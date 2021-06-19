/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.GiantBoss;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.anticheat.ThrowPlayerEvent;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.optimization.OptimizedZombieFactory;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.Prayers;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.dropttable.MassBossDropTable;
import gg.packetloss.grindstone.util.dropttable.MassBossPlayerKillInfo;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.item.BookUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.listener.BossBuggedRespawnListener;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import gg.packetloss.grindstone.util.timer.TimerUtil;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import gg.packetloss.hackbook.entity.HBGiant;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

@ComponentInformation(friendlyName = "Giant Boss", desc = "Giant, and a true boss")
@Depend(components = {
        AdminComponent.class, PrayerComponent.class, PlayerStateComponent.class,
        SpectatorComponent.class, HighScoresComponent.class},
        plugins = {"WorldGuard"})
public class GiantBossArea extends AreaComponent<GiantBossConfig> {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected PrayerComponent prayer;
    @InjectComponent
    protected PlayerStateComponent playerState;
    @InjectComponent
    protected SpectatorComponent spectator;
    @InjectComponent
    protected HighScoresComponent highScores;

    protected static final int groundLevel = 82;

    protected ProtectedRegion eastDoor, westDoor;

    protected Giant boss = null;
    protected long lastAttack = 0;
    protected int lastAttackNumber = -1;
    protected long lastDeath = 0;
    protected boolean damageHeals = false;
    protected Random random = new Random();

    protected List<Location> spawnPts = new ArrayList<>();
    protected List<Location> chestPts = new ArrayList<>();

    protected BossBar healthBar = Bukkit.createBossBar("Shnuggles Prime", BarColor.PURPLE, BarStyle.SEGMENTED_6);

    protected MassBossDropTable dropTable = new MassBossDropTable();
    protected Set<UUID> barbarianBonePlayers = new HashSet<>();
    protected boolean isKillFromBook = false;

    @Override
    public void setUp() {
        spectator.registerSpectatorKind(PlayerStateKind.SHNUGGLES_PRIME_SPECTATOR);

        world = server.getWorlds().get(0);
        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        region = manager.getRegion("vineam-district-giant-boss-area");
        eastDoor = manager.getRegion("vineam-district-giant-boss-east-door");
        westDoor = manager.getRegion("vineam-district-giant-boss-west-door");
        tick = 4 * 20;
        listener = new GiantBossListener(this);
        config = new GiantBossConfig();

        server.getScheduler().runTaskTimer(inst, this::updateBossBarProgress, 0, 5);

        // First spawn requirement
        probeArea();

        CommandBook.registerEvents(new FlightBlockingListener(admin, this::contains));
        CommandBook.registerEvents(new BossBuggedRespawnListener(
                "Shnuggles Prime",
                (e) -> boss != null && boss.equals(e) && isArenaLoaded(),
                (e) -> spawnBossEntity(e.getHealth(), e.getMaxHealth())
        ));

        setupDropTable();

        spectator.registerSpectatedRegion(PlayerStateKind.SHNUGGLES_PRIME_SPECTATOR, region);
        spectator.registerSpectatorSkull(
                PlayerStateKind.SHNUGGLES_PRIME_SPECTATOR,
                new Location(world, 201, 87, 60),
                () -> !isEmpty()
        );
    }

    @Override
    public void run() {
        updateBossBar();

        if (!isBossSpawned()) {
            if (lastDeath == 0 || System.currentTimeMillis() - lastDeath >= 1000 * 60 * 3) {
                clearArenaEntities();
                spawnBoss();
            }
        } else if (!isEmpty()) {
            equalize();
            spawnPassiveBabies();
            runAttack(ChanceUtil.getRandom(OPTION_COUNT));
        }
    }

    private void updateBossBar() {
        if (isBossSpawnedFast()) {
            BossBarUtil.syncWithPlayers(healthBar, getAudiblePlayers());
        } else {
            healthBar.removeAll();
        }
    }

    private void updateBossBarProgress() {
        if (isBossSpawnedFast()) {
            healthBar.setProgress(boss.getHealth() / boss.getMaxHealth());
        }
    }

    @Override
    public Collection<Player> getAudiblePlayers() {
        return getContained(1, Player.class);
    }

    private int getModifier(Player player) {
        int modifier = 1;

        if (EnvironmentUtil.hasThunderstorm(getWorld())) {
            modifier *= 3;
        }

        if (barbarianBonePlayers.contains(player.getUniqueId())) {
            modifier *= 3;
        }

        return modifier;
    }

    private void setupDropTable() {
        dropTable.registerCustomPlayerDrop((info, consumer) -> {
            int modifier = getModifier(info.getPlayer());

            SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), modifier, 400000).forEach(consumer);
            SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), modifier * 5, 15000).forEach(consumer);
            SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), modifier * 16, 4000).forEach(consumer);

            for (int i = 0; i < modifier; i++) {
                consumer.accept(new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRangedRandom(32, 64)));
            }
        });

        dropTable.registerPlayerDrop(() -> CustomItemCenter.build(CustomItems.BARBARIAN_BONE, ChanceUtil.getRandom(9)));

        // Chance modified drops
        NumericPipeline.Builder<MassBossPlayerKillInfo> modifiedChance = NumericPipeline.builder();
        modifiedChance.accept((info, chance) -> chance / getModifier(info.getPlayer()));

        NumericPipeline.Builder<MassBossPlayerKillInfo> bookChance = modifiedChance.fork();
        bookChance.accept((info, chance) -> isKillFromBook ? Integer.MAX_VALUE : chance);
        dropTable.registerPlayerDrop(bookChance.build(27), BookUtil.Lore.Monsters::skelril);

        // Master Weapons
        dropTable.registerPlayerDrop(modifiedChance.build(276), () -> CustomItemCenter.build(CustomItems.MASTER_SWORD));
        dropTable.registerPlayerDrop(modifiedChance.build(276), () -> CustomItemCenter.build(CustomItems.MASTER_SHORT_SWORD));
        dropTable.registerPlayerDrop(modifiedChance.build(138), () -> CustomItemCenter.build(CustomItems.MASTER_BOW));

        dropTable.registerPlayerDrop(modifiedChance.build(200), () -> CustomItemCenter.build(CustomItems.MAGIC_BUCKET));
        dropTable.registerPlayerDrop(modifiedChance.build(2500), () -> CustomItemCenter.build(CustomItems.ANCIENT_CROWN));
    }

    public boolean isBossSpawnedFast() {
        return isArenaLoaded() && boss != null && boss.isValid();
    }

    public boolean isBossSpawned() {
        if (!isArenaLoaded()) return true;
        boolean found = false;
        boolean second = false;
        for (Giant e : getContained(Giant.class)) {
            if (e.isValid() && HBGiant.is(e)) {
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

        return isBossSpawnedFast();
    }

    public boolean isArenaLoaded() {
        return RegionUtil.isLoaded(getWorld(), getRegion());
    }

    public Location getBossSpawnLocation() {
        return RegionUtil.getCenterAt(getWorld(), groundLevel, getRegion());
    }

    private void spawnBossEntity(double currentHealth, double maxHealth) {
        boss = HBGiant.spawn(getBossSpawnLocation());
        boss.setMaxHealth(maxHealth);
        boss.setHealth(currentHealth);
        boss.setRemoveWhenFarAway(false);

        EntityUtil.setKnockbackResistance(boss, 1);
        EntityUtil.setFollowRange(boss, 40);
    }

    public void spawnBoss() {
        spawnBossEntity(config.maxHealthNormal, config.maxHealthNormal);

        setDoor(eastDoor, Material.CHISELED_SANDSTONE);
        setDoor(westDoor, Material.CHISELED_SANDSTONE);

        for (Player player : getAudiblePlayers()) ChatUtil.sendWarning(player, "I live again!");
    }

    public void probeArea() {
        spawnPts.clear();
        chestPts.clear();

        RegionWalker.walk(getRegion().getParent(), (x, y, z) -> {
            BlockState block = getWorld().getBlockAt(x, y, z).getState();
            if (block.getType() == Material.GOLD_BLOCK) {
                spawnPts.add(block.getLocation().add(.5, 2, .5));
                return;
            }

            if (block.getType() == Material.CHEST) {
                chestPts.add(block.getLocation());
            }
        });

        Collections.shuffle(spawnPts);
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

    public void clearArenaEntities() {
        getContained(Monster.class, ExperienceOrb.class).forEach(e -> {
            if (e.getType() != EntityType.EXPERIENCE_ORB) {
                for (int i = 0; i < 20; i++) {
                    getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
                }
            }

            e.remove();
        });
    }

    public void removeBabies() {
        getContained(1, Zombie.class).stream()
                .filter(Zombie::isBaby)
                .forEach(e -> {
                    for (int i = 0; i < 20; i++) {
                        getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
                    }

                    e.remove();
                });
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

    private void spawnBabies(int chancePerSpawn, int numBabies) {
        final ItemStack weapon = new ItemStack(Material.BONE);
        ItemMeta weaponMeta = weapon.getItemMeta();
        weaponMeta.addEnchant(Enchantment.DAMAGE_ALL, 2, true);
        weapon.setItemMeta(weaponMeta);

        List<Player> participants = Lists.newArrayList(getContainedParticipants());
        Collections.shuffle(participants);

        for (int i = 0; i < spawnPts.size(); ++i) {
            if (numBabies++ > config.maxBabies) {
                break;
            }

            Location spawnPt = spawnPts.get(i);
            if (ChanceUtil.getChance(chancePerSpawn)) {
                Zombie z = OptimizedZombieFactory.create(spawnPt);

                // Create the baby with no item pickup
                z.setBaby(true);
                z.setCanPickupItems(false);

                // Add equipment
                EntityEquipment equipment = z.getEquipment();
                equipment.setItemInHand(weapon.clone());
                equipment.setItemInHandDropChance(0F);

                // Set target to (effectively) random player
                if (participants.size() > 0) {
                    z.setTarget(participants.get(i % participants.size()));
                }
            }
        }

    }

    public void spawnBabies(int chancePerSpawn) {
        int numBabies = getContained(Zombie.class).size();
        spawnBabies(chancePerSpawn, numBabies);
    }

    public void spawnPassiveBabies() {
        int numBabies = getContained(Zombie.class).size();
        if (numBabies >= config.minBabies) {
            return;
        }

        spawnBabies(config.babyPassiveSpawnChance, numBabies);
    }

    public void applyBabyPots() {
        Collection<Zombie> containedBabies = getContained(Zombie.class);
        double invertedPercentage = containedBabies.size() / (double) config.maxBabies;
        int potLevel = (int) (invertedPercentage * config.babyMaxPotLevel);

        PotionEffectType[] effectTypes = new PotionEffectType[] {
                PotionEffectType.INCREASE_DAMAGE, PotionEffectType.DAMAGE_RESISTANCE
        };

        for (Zombie baby : containedBabies) {
            for (PotionEffectType effectType : effectTypes) {
                baby.removePotionEffect(effectType);
                baby.addPotionEffect(new PotionEffect(effectType, 20 * config.babyPotTime, potLevel));
            }
        }
    }

    public void handlePlayerSurrender() {
        server.getScheduler().runTask(inst, () -> {
            if (!isBossSpawned()) {
                return;
            }

            if (getContainedParticipants().isEmpty()) {
                boss.setHealth(boss.getMaxHealth());
                removeBabies();
            } else {
                EntityUtil.heal(boss, boss.getMaxHealth() / 3);
            }
        });
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
            case 6: {
                ChatUtil.sendWarning(audiblePlayers, "I think I need to turn up the heat...");

                Map<UUID, Location> playerLastLoc = new HashMap<>();
                IntegratedRunnable integratedRunnable = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {
                        if (!isBossSpawned()) return true;

                        for (Player player : getContainedParticipants()) {
                            Block block = player.getLocation().getBlock();
                            if (block.getType() != Material.FIRE) {
                                continue;
                            }

                            if (ItemUtil.hasAncientArmour(player) && ChanceUtil.getChance(3)) {
                                ChatUtil.sendNotice(player, "Your armor shields you from the fire!");
                                continue;
                            }

                            EntityUtil.forceDamage(player, 3);
                        }

                        Map<UUID, Location> playerNewLoc = new HashMap<>();
                        for (Player player : getContainedParticipants()) {
                            Location playerLoc = player.getLocation();

                            Block block = playerLoc.getBlock();
                            if (block.getType() != Material.AIR) {
                                continue;
                            }

                            // Move down so if they jump the floor still ignites
                            while (block.getRelative(BlockFace.DOWN).getType() == Material.AIR) {
                                block = block.getRelative(BlockFace.DOWN);
                            }

                            playerLoc.setY(block.getY());
                            playerNewLoc.put(player.getUniqueId(), playerLoc);
                        }

                        server.getScheduler().runTaskLater(inst, () -> {
                            playerNewLoc.forEach((playerId, newLoc) -> {
                                Location lastLoc = playerLastLoc.get(playerId);
                                playerLastLoc.put(playerId, newLoc);

                                // No last block just set the new block
                                if (lastLoc == null) {
                                    newLoc.getBlock().setType(Material.FIRE);
                                    return;
                                }

                                // Create a ray trace from the last block to the current block
                                SimpleRayTrace trace = new SimpleRayTrace(
                                    lastLoc,
                                    VectorUtil.createDirectionalVector(lastLoc, newLoc),
                                    (int) (lastLoc.distance(newLoc) + 1)
                                );

                                // Light everything in that path on fire
                                while (trace.hasNext()) {
                                    Block block = trace.next().getBlock();

                                    // This really shouldn't happen often (ever) but let's be careful
                                    if (block.getType() != Material.AIR && block.getType() != Material.FIRE) {
                                        break;
                                    }

                                    block.setType(Material.FIRE);
                                }
                            });
                        }, 10);

                        return true;
                    }

                    @Override
                    public void end() {
                        server.getScheduler().runTaskLater(inst, () -> {
                            RegionWalker.walk(region, (x, y, z) -> {
                                Block block = world.getBlockAt(x, y, z);
                                if (block.getType() == Material.FIRE) {
                                    block.setType(Material.AIR);
                                }
                            });
                        }, 10);
                    }
                };
                TimedRunnable runnable = new TimedRunnable(integratedRunnable, 50);
                BukkitTask task = server.getScheduler().runTaskTimer(inst, runnable, 0, 4);
                runnable.setTask(task);
                break;
            }
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
                        PrayerComponent.constructPrayer(player, Prayers.DOOM, 120000);
                    });
                }, 20 * 7);
                break;
            case 9: {
                ChatUtil.sendNotice(audiblePlayers, ChatColor.DARK_RED, "Sink into the darkness!");
                List<Location> darkSmokePoints = new ArrayList<>();
                IntegratedRunnable integratedRunnable = new IntegratedRunnable() {
                    @Override
                    public boolean run(int times) {
                        if (!isBossSpawned()) return true;

                        if (times % 3 == 0) {
                            for (Location point : darkSmokePoints) {
                                for (Player player : point.getNearbyEntitiesByType(Player.class, 2)) {
                                    if (!contains(player)) {
                                        continue;
                                    }

                                    player.damage(50, boss);
                                }
                            }
                        } else {
                            for (LivingEntity entity : getContained(Zombie.class)) {
                                if (ChanceUtil.getChance(20)) {
                                    entity.setHealth(0);
                                    darkSmokePoints.add(entity.getLocation());
                                }
                            }
                        }

                        for (Location point : darkSmokePoints) {
                            EnvironmentUtil.generateRadialEffect(point, Effect.SMOKE);
                        }

                        return true;
                    }

                    @Override
                    public void end() {
                    }
                };
                TimedRunnable runnable = new TimedRunnable(integratedRunnable, 20);
                BukkitTask task = server.getScheduler().runTaskTimer(inst, runnable, 0, 10);
                runnable.setTask(task);
                break;
            }
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

    public void clearChests() {
        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        taskBuilder.setInterval(20);
        taskBuilder.setNumberOfRuns(30);

        taskBuilder.setAction((times) -> {
            if (TimerUtil.matchesFilter(times, 10, 5)) {
                ChatUtil.sendWarning(getAudiblePlayers(), "Clearing chest contents in: " + times + " seconds.");
            }
            return true;
        });

        taskBuilder.setFinishAction(() -> {
            ChatUtil.sendWarning(getAudiblePlayers(), "Clearing chest contents!");
            for (Location location : chestPts) {
                BlockState state = location.getBlock().getState();
                if (state instanceof Chest) {
                    ((Chest) state).getInventory().clear();
                }
            }
        });

        taskBuilder.build();
    }
}
