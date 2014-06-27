/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.minigame.games;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitConfiguration;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.DataException;
import com.sk89q.worldedit.world.snapshot.Snapshot;
import com.sk89q.worldedit.world.snapshot.SnapshotRestore;
import com.sk89q.worldedit.world.storage.ChunkStore;
import com.sk89q.worldedit.world.storage.MissingWorldException;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.anticheat.AntiCheatCompatibilityComponent;
import com.skelril.aurora.city.engine.minigame.GameProgress;
import com.skelril.aurora.city.engine.minigame.MinigameComponent;
import com.skelril.aurora.city.engine.minigame.PlayerGameState;
import com.skelril.aurora.events.anticheat.FallBlockerEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.events.apocalypse.ApocalypseLocalSpawnEvent;
import com.skelril.aurora.events.egg.EggDropEvent;
import com.skelril.aurora.events.environment.DarkAreaInjuryEvent;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.exceptions.UnsupportedPrayerException;
import com.skelril.aurora.prayer.Prayer;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.*;
import com.skelril.aurora.util.checker.RegionChecker;
import com.skelril.aurora.util.extractor.entity.CombatantPair;
import com.skelril.aurora.util.extractor.entity.EDBEExtractor;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.hackbook.ChunkBook;
import com.skelril.hackbook.exceptions.UnsupportedFeatureException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

//import net.gravitydevelopment.anticheat.check.CheckType;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Jungle Raid", desc = "Warfare at it's best!")
@Depend(components = {AdminComponent.class, PrayerComponent.class}, plugins = {"WorldEdit", "WorldGuard"})
public class JungleRaidComponent extends MinigameComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private ProtectedRegion region;
    private World world;
    private final Random random = new Random();
    private LocalConfiguration config;
    private static Economy economy = null;
    private static final double BASE_AMT = 1.2;
    private short attempts = 0;
    private Location[] teamSpawns;
    private List<BukkitTask> restorationTask = new ArrayList<>();

    private long start = 0;
    private int amt = 7;

    private String titan = "";

    @InjectComponent
    AdminComponent adminComponent;
    @InjectComponent
    PrayerComponent prayerComponent;
    @InjectComponent
    AntiCheatCompatibilityComponent antiCheat;

    public JungleRaidComponent() {
        super("Jungle Raid", "jr", 3);
        teamSpawns = new Location[MAX_TEAMS - 1];
    }

    private Prayer[] getPrayers(Player player) throws UnsupportedPrayerException {

        return new Prayer[]{
                PrayerComponent.constructPrayer(player, PrayerType.BLINDNESS, TimeUnit.DAYS.toMillis(1)),
                PrayerComponent.constructPrayer(player, PrayerType.WALK, TimeUnit.DAYS.toMillis(1))
        };
    }

    private static final ItemStack sword = new ItemStack(ItemID.IRON_SWORD);
    private static final ItemStack bow = new ItemStack(ItemID.BOW);
    private static final ItemStack tnt = new ItemStack(BlockID.TNT, 32);
    private static final ItemStack flintAndSteel = new ItemStack(ItemID.FLINT_AND_TINDER);
    private static final ItemStack shears = new ItemStack(ItemID.SHEARS);
    private static final ItemStack axe = new ItemStack(ItemID.IRON_AXE);
    private static final ItemStack steak = new ItemStack(ItemID.COOKED_BEEF, 64);
    private static final ItemStack compass = new ItemStack(ItemID.COMPASS);
    private static final ItemStack arrows = new ItemStack(ItemID.ARROW, 64);

    @Override
    public void initialize(Set<Character> flags) {
        super.initialize(flags);

        start = System.currentTimeMillis();

        Collection<Player> players = getContainedPlayers();

        if (gameFlags.contains('H')) {
            ChatUtil.sendNotice(players, "Team two can now run.");
        } else {
            ChatUtil.sendNotice(players, "All players can now run.");
        }
    }

    @Override
    public void start() {
        super.start();

        Collection<Player> players = getContainedPlayers();

        if (gameFlags.contains('H')) {
            ChatUtil.sendNotice(players, "All players can now run.");
        }
        ChatUtil.sendNotice(players, "Fighting can now commence!");
    }

    // Player Management
    @Override
    public boolean addToTeam(Player player, int teamNumber, Set<Character> flags) {

        if (adminComponent.isAdmin(player) && !adminComponent.isSysop(player)) return false;

        super.addToTeam(player, teamNumber, flags);

        PlayerInventory playerInventory = player.getInventory();
        playerInventory.clear();

        List<ItemStack> gear = new ArrayList<>();
        if (flags.contains('z')) {
            ItemStack enchantedSword = sword.clone();
            enchantedSword.addEnchantment(Enchantment.FIRE_ASPECT, 1);
            enchantedSword.addEnchantment(Enchantment.KNOCKBACK, 1);

            gear.add(enchantedSword);
        } else if (flags.contains('a')) {
            ItemStack dmgBow = bow.clone();
            dmgBow.addEnchantment(Enchantment.ARROW_KNOCKBACK, 2);

            ItemStack fireBow = bow.clone();
            fireBow.addEnchantment(Enchantment.ARROW_FIRE, 1);

            gear.add(dmgBow);
            gear.add(fireBow);
        } else {
            gear.add(sword.clone());
            gear.add(bow.clone());
        }

        for (int i = 0; i < 3; i++) gear.add(tnt.clone());
        gear.add(flintAndSteel.clone());
        gear.add(shears.clone());
        gear.add(axe.clone());
        gear.add(steak.clone());
        gear.add(compass.clone());
        for (int i = 0; i < 2; i++) gear.add(arrows.clone());

        player.getInventory().addItem(gear.toArray(new ItemStack[gear.size()]));

        ItemStack[] leatherArmour = ItemUtil.leatherArmour;
        Color color = Color.WHITE;
        if (teamNumber == 2) color = Color.RED;
        else if (teamNumber == 1) color = Color.BLUE;

        LeatherArmorMeta helmMeta = (LeatherArmorMeta) leatherArmour[3].getItemMeta();
        helmMeta.setDisplayName(ChatColor.WHITE + "Team Hood");
        helmMeta.setColor(color);
        leatherArmour[3].setItemMeta(helmMeta);

        LeatherArmorMeta chestMeta = (LeatherArmorMeta) leatherArmour[2].getItemMeta();
        chestMeta.setDisplayName(ChatColor.WHITE + "Team Chestplate");
        chestMeta.setColor(color);
        leatherArmour[2].setItemMeta(chestMeta);

        LeatherArmorMeta legMeta = (LeatherArmorMeta) leatherArmour[1].getItemMeta();
        legMeta.setDisplayName(ChatColor.WHITE + "Team Leggings");
        legMeta.setColor(color);
        leatherArmour[1].setItemMeta(legMeta);

        LeatherArmorMeta bootMeta = (LeatherArmorMeta) leatherArmour[0].getItemMeta();
        bootMeta.setDisplayName(ChatColor.WHITE + "Team Boots");
        bootMeta.setColor(color);
        leatherArmour[0].setItemMeta(bootMeta);

        playerInventory.setArmorContents(leatherArmour);

        // Gets a player a nice random location of entry
        Location battleLoc = null;
        if (teamNumber > 0) {
            battleLoc = teamSpawns[teamNumber - 1].clone();
        }

        RegionChecker checker = new RegionChecker(region);
        while (battleLoc == null || !contains(battleLoc) || battleLoc.getBlock().getTypeId() != BlockID.AIR) {
            battleLoc = LocationUtil.findFreePosition(LocationUtil.pickLocation(world, 75, checker));
        }

        if (teamNumber > 0) {
            teamSpawns[teamNumber - 1] = battleLoc;
        }

        if (player.getVehicle() != null) {
            player.getVehicle().eject();
        }

        try {
            prayerComponent.influencePlayer(player, getPrayers(player));
        } catch (UnsupportedPrayerException e) {
            e.printStackTrace();
        }
        return player.teleport(battleLoc);
    }

    @Override
    public void printFlags() {

        Collection<Player> players = getContainedPlayers();

        ChatUtil.sendNotice(players, ChatColor.GREEN + "The following flags are enabled: ");
        if (gameFlags.contains('H')) ChatUtil.sendNotice(players, "Hunter Mode");
        if (gameFlags.contains('G')) ChatUtil.sendNotice(players, "Guilds Enabled");
        if (gameFlags.contains('T')) {
            ChatUtil.sendNotice(players, "Titan Mode");

            List<PlayerGameState> gameStates = Lists.newArrayList(playerState.values());
            for (PlayerGameState aPlayerState : gameStates) {
                aPlayerState.setTeamNumber(0);

                try {
                    final Player player = Bukkit.getPlayerExact(aPlayerState.getOwnerName());
                    if (player == null || !player.isValid()) continue;

                    PlayerInventory pInventory = player.getInventory();
                    ItemStack[] stacks = pInventory.getArmorContents();

                    for (ItemStack stack : stacks) {
                        if (stack == null || !stack.hasItemMeta()) continue;
                        if (stack.getItemMeta() instanceof LeatherArmorMeta) {
                            LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
                            meta.setColor(Color.BLACK);
                            stack.setItemMeta(meta);
                        }
                    }
                    pInventory.setArmorContents(stacks);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            titan = CollectionUtil.getElement(gameStates).getOwnerName();
            try {
                Player aPlayer = Bukkit.getPlayerExact(titan);
                // antiCheat.exempt(aPlayer, CheckType.FAST_BREAK);
                // antiCheat.exempt(aPlayer, CheckType.NO_SWING);
                // antiCheat.exempt(aPlayer, CheckType.AUTOTOOL);
            } catch (Exception ex) {
                ChatUtil.sendNotice(players, ChatColor.RED, "[ERROR] Cannot find titan.");
            }
        }
        if (gameFlags.contains('r')) {
            ChatUtil.sendWarning(players, "Random Rockets");
        }
        if (gameFlags.contains('h')) {
            ChatUtil.sendWarning(players, "Survival Mode");
            for (Player player : players) {
                PlayerInventory inventory = player.getInventory();
                ItemStack stack;
                for (int i = 0; i < inventory.getContents().length; i++) {
                    stack = inventory.getItem(i);
                    if (stack == null || stack.getTypeId() == ItemID.COOKED_BEEF) continue;
                    inventory.setItem(i, null);
                }
            }
        }
        if (gameFlags.contains('x')) {
            if (gameFlags.contains('s')) {
                ChatUtil.sendWarning(players, "Highly Explosive Arrows");
            } else {
                ChatUtil.sendWarning(players, "Explosive Arrows");
            }
        }
        if (gameFlags.contains('g')) {
            if (gameFlags.contains('s')) {
                ChatUtil.sendWarning(players, "OP Grenades");
            } else {
                ChatUtil.sendWarning(players, "Grenades");
            }
        }
        if (gameFlags.contains('t')) ChatUtil.sendWarning(players, "Torment Arrows");
        if (gameFlags.contains('d')) ChatUtil.sendWarning(players, "Death touch");
        if (gameFlags.contains('a')) ChatUtil.sendWarning(players, "2012");
        if (gameFlags.contains('p')) ChatUtil.sendNotice(players, "Potion Plummet");
        if (gameFlags.contains('j')) {
            if (gameFlags.contains('s')) {
                ChatUtil.sendNotice(players, ChatColor.AQUA, "Super jumpy");
            } else {
                ChatUtil.sendNotice(players, ChatColor.AQUA, "Jumpy");
            }
        }
        if (gameFlags.contains('f')) ChatUtil.sendNotice(players, ChatColor.AQUA, "No fire spread");
        if (gameFlags.contains('m')) ChatUtil.sendNotice(players, ChatColor.AQUA, "No mining");
        if (gameFlags.contains('b')) ChatUtil.sendNotice(players, ChatColor.AQUA, "No block break");

        if (gameFlags.contains('q')) ChatUtil.sendNotice(players, ChatColor.GOLD, "Quick start");
        if (gameFlags.contains('S')) ChatUtil.sendNotice(players, ChatColor.GOLD, "Sudden death disabled");
    }

    @Override
    public String getWinner() {

        int[] teams = new int[MAX_TEAMS];
        for (PlayerGameState entry : playerState.values()) {
            teams[entry.getTeamNumber()]++;
        }

        int aliveTeam = -1;
        for (int team = 0; team < teams.length; team++) {
            if (teams[team] > 0) {
                if (aliveTeam != -1) return null;
                aliveTeam = team;
            }
        }

        String winnerName = "Team " + aliveTeam;
        switch (aliveTeam) {
            case -1:
                return "";
            case 0:
                if (teams[0] > 1) return null;
                winnerName = Lists.newArrayList(playerState.values()).get(0).getOwnerName();
                break;
        }
        return winnerName;
    }

    @Override
    public void restore(Player player, PlayerGameState state) {

        prayerComponent.uninfluencePlayer(player);

        super.restore(player, state);
    }

    @Override
    public void removeFromTeam(Player player, boolean forced) {

        if (!playerState.containsKey(player.getName())) return;

        super.removeFromTeam(player, forced);

        if (economy != null && forced && isGameInitialised()) {
            payPlayer(player, progress == GameProgress.ENDING ? 10 : 1);
        }
    }

    @Override
    public void removeGoneFromTeam(Player player, boolean forced) {

        if (!goneState.containsKey(player.getName())) return;

        super.removeGoneFromTeam(player, forced);

        if (economy != null && forced) {
            payPlayer(player, 1);
        }
    }

    private void payPlayer(Player player, double modifier) {

        double amt = BASE_AMT * modifier;

        economy.depositPlayer(player.getName(), amt);
        ChatUtil.sendNotice(player, "You received: " + economy.format(amt) + '.');
    }

    @Override
    public Collection<Player> getContainedPlayers() {

        return getContainedPlayers(0);
    }

    public Collection<Player> getContainedPlayers(int parentsUp) {
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();

        final ProtectedRegion finalR = r;
        return server.getOnlinePlayers().stream()
                .filter(p -> LocationUtil.isInRegion(world, finalR, p))
                .collect(Collectors.toList());
    }

    public boolean contains(Location location) {

        return LocationUtil.isInRegion(world, region, location);
    }

    @Override
    public boolean probe() {

        world = Bukkit.getWorld(config.worldName);
        try {
            region = getWorldGuard().getGlobalRegionManager().get(world).getRegion(config.region);
        } catch (UnknownPluginException | NullPointerException e) {
            if (attempts > 10) {
                e.printStackTrace();
                return false;
            }
            attempts++;
            server.getScheduler().runTaskLater(inst, this::probe, 2);
        }

        return world != null && region != null;
    }

    @Override
    public void enable() {

        super.enable();

        config = configure(new LocalConfiguration());

        probe();
        setupEconomy();
        //noinspection AccessStaticViaInstance
        inst.registerEvents(new JungleRaidListener());
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 10);
    }

    @Override
    public void end() {

        super.end();

        restore();

        for (int i = 0; i < teamSpawns.length; i++) {
            teamSpawns[i] = null;
        }
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
        probe();
    }

    @Override
    public void disable() {

        super.disable();

        stopRestore();
    }

    @Override
    public void run() {

        try {
            if (!isGameInitialised() && !queue.isEmpty()) processQueue();
            if (playerState.size() == 0 && !isGameInitialised()) return;

            if (isGameInitialised()) {
                boolean guild = gameFlags.contains('G');
                int min = gameFlags.contains('H') ? (isGameActive() ? 0 : 2) : 0;

                for (PlayerGameState entry : playerState.values()) {
                    if (entry.getTeamNumber() < min) continue;
                    try {
                        Player player = Bukkit.getPlayerExact(entry.getOwnerName());
                        if (!guild) {
                            adminComponent.deguildPlayer(player);
                        }
                        if (player == null) continue;
                        prayerComponent.uninfluencePlayer(player);
                        for (PotionEffectType potionEffectType : PotionEffectType.values()) {
                            if (potionEffectType == null) continue;
                            if (player.hasPotionEffect(potionEffectType)) player.removePotionEffect(potionEffectType);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Kill missing players
            for (PlayerGameState entry : playerState.values()) {
                try {
                    Player player = Bukkit.getPlayerExact(entry.getOwnerName());
                    if (player == null || !player.isValid() || contains(player.getLocation())) continue;
                    player.setHealth(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!isGameInitialised()) return;

            // Security
            for (Player player : getContainedPlayers()) {

                if (!player.isValid()) continue;

                if (!player.getGameMode().equals(GameMode.SURVIVAL)) {
                    if (player.isFlying()) {
                        player.setAllowFlight(true);
                        player.setFlying(true);
                        player.setGameMode(GameMode.SURVIVAL);
                    } else player.setGameMode(GameMode.SURVIVAL);
                }

                if (gameFlags.contains('T') && titan.equals(player.getName())) {

                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 20, 1, true));
                }
            }

            if (!isGameActive()) return;

            // Sudden death
            boolean suddenD = !gameFlags.contains('S')
                    && System.currentTimeMillis() - start >= TimeUnit.MINUTES.toMillis(15);
            if (suddenD) amt = 100;

            // Distributor
            if (gameFlags.contains('a') || gameFlags.contains('g') || gameFlags.contains('p') || suddenD) {

                BlockVector bvMax = region.getMaximumPoint();
                BlockVector bvMin = region.getMinimumPoint();

                for (int i = 0; i < ChanceUtil.getRangedRandom(amt / 3, amt); i++) {

                    Vector v = LocationUtil.pickLocation(bvMin.getX(), bvMax.getX(),
                            bvMin.getZ(), bvMax.getZ()).add(0, bvMax.getY(), 0);
                    Location testLoc = new Location(world, v.getX(), v.getY(), v.getZ());

                    if (testLoc.getBlock().getTypeId() != BlockID.AIR) continue;

                    if (gameFlags.contains('a') || suddenD) {
                        TNTPrimed e = world.spawn(testLoc, TNTPrimed.class);
                        e.setVelocity(new org.bukkit.util.Vector(
                                random.nextDouble() * 2.0 - 1,
                                random.nextDouble() * 2 * -1,
                                random.nextDouble() * 2.0 - 1));
                        if (ChanceUtil.getChance(4)) e.setIsIncendiary(true);
                    }
                    if (gameFlags.contains('p')) {
                        PotionType type = CollectionUtil.getElement(PotionType.values());
                        if (type == null || type == PotionType.WATER) {
                            i--;
                            continue;
                        }
                        for (int ii = ChanceUtil.getRandom(5); ii > 0; --ii) {
                            ThrownPotion potion = world.spawn(testLoc, ThrownPotion.class);
                            Potion brewedPotion = new Potion(type);
                            brewedPotion.setLevel(type.getMaxLevel());
                            brewedPotion.setSplash(true);
                            potion.setItem(brewedPotion.toItemStack(1));
                            potion.setVelocity(new org.bukkit.util.Vector(
                                    random.nextDouble() * 2.0 - 1,
                                    0,
                                    random.nextDouble() * 2.0 - 1));
                        }
                    }
                    if (gameFlags.contains('g')) {
                        testLoc.getWorld().dropItem(testLoc, new ItemStack(ItemID.SNOWBALL, ChanceUtil.getRandom(3)));
                    }
                }
                if (amt < 150 && ChanceUtil.getChance(gameFlags.contains('s') ? 9 : 25)) amt++;
            }

            // Random Rockets
            if (gameFlags.contains('r')) {
                for (final Player player : getContainedPlayers()) {
                    if (!ChanceUtil.getChance(30)) continue;
                    for (int i = 0; i < 5; i++) {
                        server.getScheduler().runTaskLater(inst, () -> {
                            Location targetLocation = player.getLocation();
                            Firework firework = targetLocation.getWorld().spawn(targetLocation, Firework.class);
                            FireworkMeta meta = firework.getFireworkMeta();
                            FireworkEffect.Builder builder = FireworkEffect.builder();
                            builder.flicker(ChanceUtil.getChance(2));
                            builder.trail(ChanceUtil.getChance(2));
                            builder.withColor(Arrays.asList(Color.RED));
                            builder.withFade(Arrays.asList(Color.YELLOW));
                            builder.with(FireworkEffect.Type.BURST);
                            meta.addEffect(builder.build());
                            meta.setPower(ChanceUtil.getRangedRandom(2, 5));
                            firework.setFireworkMeta(meta);
                        }, i * 4);
                    }
                }
            }

            String winnerName = getWinner();

            // No winner
            if (winnerName == null) return;

            // Tie or a specific team/player?
            if (winnerName.isEmpty()) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Tie game!");
            } else {
                Bukkit.broadcastMessage(ChatColor.GOLD + winnerName + " has won!");
            }

            end();
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.broadcastMessage(ChatColor.RED + "[WARNING] Jungle Raid logic failed to process.");
        }
    }

    public void stopRestore() {

        if (restorationTask.size() < 1) return;
        Bukkit.broadcastMessage(ChatColor.RED + "Jungle Arena restoration cancelled.");
        for (BukkitTask task : Collections.synchronizedList(restorationTask)) {

            task.cancel();
        }
        restorationTask.clear();
    }

    private void restore() {

        BukkitConfiguration worldEditConfig = null;
        try {
            worldEditConfig = getWorldEdit().getLocalConfiguration();
        } catch (UnknownPluginException e) {
            e.printStackTrace();
        }
        if ((worldEditConfig != null ? worldEditConfig.snapshotRepo : null) == null) {
            log.warning("No snapshots configured, restoration cancelled.");
            return;
        }

        try {
            // Discover chunks
            Location battleLoc;
            try {
                battleLoc = new Location(world, config.x, config.y, config.z);
            } catch (NullPointerException ex) {
                return;
            }

            world.getEntitiesByClasses(Item.class, TNTPrimed.class).stream()
                    .filter(entity -> region.contains(BukkitUtil.toVector(entity.getLocation())))
                    .forEach(Entity::remove);

            final Snapshot snap = worldEditConfig.snapshotRepo.getDefaultSnapshot(config.worldName);

            if (snap == null) {
                log.warning("No snapshot could be found, restoration cancelled.");
                return;
            }

            final List<Chunk> chunkList = new ArrayList<>();
            chunkList.add(battleLoc.getChunk());

            Vector min;
            Vector max;

            try {
                min = region.getMinimumPoint();
                max = region.getMaximumPoint();
            } catch (NullPointerException ex) {
                return;
            }

            final int minX = min.getBlockX();
            final int minZ = min.getBlockZ();
            final int minY = min.getBlockY();
            final int maxX = max.getBlockX();
            final int maxZ = max.getBlockZ();
            final int maxY = max.getBlockY();

            Chunk c;
            for (int x = minX; x <= maxX; x += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {
                    c = world.getBlockAt(x, minY, z).getChunk();
                    if (!chunkList.contains(c)) chunkList.add(c);
                }
            }

            log.info("Snapshot '" + snap.getName() + "' loaded; now restoring Jungle Arena...");
            // Tell players restoration is beginning
            for (Player player : server.getOnlinePlayers()) {

                ChatUtil.sendWarning(player, "Restoring Jungle Arena...");
            }

            // Setup task to progressively restore
            final EditSession fakeEditor = WorldEdit.getInstance()
                    .getEditSessionFactory().getEditSession(new BukkitWorld(world), -1);
            for (final Chunk chunk : chunkList) {
                BukkitTask aTask = server.getScheduler().runTaskLater(inst, () -> {
                    ChunkStore chunkStore;

                    try {
                        chunkStore = snap.getChunkStore();
                    } catch (DataException | IOException e) {
                        log.warning("Failed to load snapshot: " + e.getMessage());
                        return;
                    }

                    try {
                        Block minBlock = chunk.getBlock(0, minY, 0);
                        Block maxBlock = chunk.getBlock(15, maxY, 15);
                        Vector minPt = new Vector(minBlock.getX(), minBlock.getY(), minBlock.getZ());
                        Vector maxPt = new Vector(maxBlock.getX(), maxBlock.getY(), maxBlock.getZ());

                        Region r = new CuboidRegion(minPt, maxPt);

                        // Restore snapshot
                        if (!chunk.isLoaded()) chunk.load();
                        SnapshotRestore restore = new SnapshotRestore(chunkStore, fakeEditor, r);

                        try {
                            restore.restore();
                            ChunkBook.relight(chunk);
                        } catch (MaxChangedBlocksException e) {
                            log.warning("Congratulations! You got an error which makes no sense!");
                            e.printStackTrace();
                            return;
                        } catch (UnsupportedFeatureException e) {
                            log.warning("Couldn't relight the chunk!");
                        }

                        if (restore.hadTotalFailure()) {
                            String error = restore.getLastErrorMessage();
                            if (error != null) {
                                log.warning("Errors prevented any blocks from being restored.");
                                log.warning("Last error: " + error);
                            } else {
                                log.warning("No chunks could be loaded. (Bad archive?)");
                            }
                        } else {
                            if (restore.getMissingChunks().size() > 0 || restore.getErrorChunks().size() > 0) {
                                log.info(String.format("Restored, %d missing chunks and %d other errors.",
                                        restore.getMissingChunks().size(),
                                        restore.getErrorChunks().size()));
                            }
                            if (chunkList.indexOf(chunk) == chunkList.size() - 1) {
                                Bukkit.broadcastMessage(ChatColor.YELLOW + "Restored successfully.");
                            }
                        }
                    } finally {
                        try {
                            chunkStore.close();
                        } catch (IOException ignored) {
                        }
                    }
                }, 5 * chunkList.indexOf(chunk));
                restorationTask.add(aTask);
            }

            // Setup a task to clear out any restoration task
            server.getScheduler().runTaskLater(inst, restorationTask::clear, (5 * chunkList.size()) + 20);
        } catch (MissingWorldException e) {
            log.warning("The world: " + config.worldName + " could not be found, restoration cancelled.");
        }
        /* LogBlock Legacy Code
        if (startT == 0) return;

        Bukkit.dispatchCommand(server.getConsoleSender(), "lb savequeue");
        server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

            @Override
            public void run() {


                World w = Bukkit.getWorld("City");
                ProtectedRegion rg = getWorldGuard().getGlobalRegionManager().get(w).getRegion(config.region);
                CuboidSelection selection = new CuboidSelection(w, rg.getMinimumPoint(), rg.getMaximumPoint());

                int time = (int) TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - startT);
                if (time < 1) time = 1;

                QueryParams params = new QueryParams(getLogBlock());
                params.since = time;
                params.sel = selection;
                params.limit = -1;
                params.bct = QueryParams.BlockChangeType.ALL;
                params.world = w;
                params.needDate = true;
                params.needType = true;
                params.needData = true;
                params.needPlayer = true;
                params.needCoords = true;

                try {
                    List<BlockChange> created = getLogBlock().getBlockChanges(params);

                    int changeCount = 0;
                    for (BlockChange change : created) {
                        Block b = change.getLocation().getBlock();
                        if (!b.getChunk().isLoaded()) b.getChunk().load();
                        b.setTypeIdAndData(change.replaced, change.data, true);
                        changeCount++;
                    }

                    log.info("Jungle Raid Restorer changed: " + changeCount + " blocks.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                startT = 0;
            }
        }, 10);
        */
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("start.World")
        public String worldName = "City";
        @Setting("start.X")
        public int x = -654;
        @Setting("start.Y")
        public int y = 37;
        @Setting("start.Z")
        public int z = -404;
        @Setting("region")
        public String region = "carpe-diem-district-jungle-raid";

    }

    private static EDBEExtractor<Player, Player, Projectile> extractor = new EDBEExtractor<>(
            Player.class,
            Player.class,
            Projectile.class
    );

    private class JungleRaidListener implements Listener {

        private final String[] guildCmdList = new String[]{
                "ninja", "rogue", "unninja", "derogue"
        };
        private final String[] cmdWhiteList = new String[]{
                "ar", "jr", "stopweather", "me", "say", "pm", "msg", "message", "whisper", "tell",
                "reply", "r", "mute", "unmute", "debug", "dropclear", "dc", "auth", "toggleeditwand"
        };

        @EventHandler(ignoreCancelled = true)
        public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {

            Player player = event.getPlayer();
            if (getTeam(player) != -1) {
                String command = event.getMessage();
                boolean allowed = false;
                for (String cmd : cmdWhiteList) {
                    if (command.toLowerCase().startsWith("/" + cmd)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed && gameFlags.contains('G')) {
                    for (String cmd : guildCmdList) {
                        if (command.toLowerCase().startsWith("/" + cmd)) {
                            allowed = true;
                            break;
                        }
                    }
                }
                if (!allowed) {
                    ChatUtil.sendError(player, "Command blocked.");
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onItemDrop(PlayerDropItemEvent event) {

            if (getTeam(event.getPlayer()) != -1 && !isGameActive()) event.setCancelled(true);
        }

        @EventHandler
        public void onClick(PlayerInteractEvent event) {

            final Player player = event.getPlayer();
            ItemStack stack = player.getItemInHand();

            if (!isGameActive()) return;

            if (getTeam(player) != -1) {
                if (stack != null && stack.getTypeId() == ItemID.COMPASS) {
                    Set<String> resultSet = new HashSet<>();
                    for (PlayerGameState state : playerState.values()) {

                        Player aPlayer = Bukkit.getPlayerExact(state.getOwnerName());

                        // Check validity
                        if (aPlayer == null || !player.isValid() || player.equals(aPlayer)) continue;

                        // Check team
                        if (isFriendlyFire(player, aPlayer)) continue;

                        ChatColor color = player.hasLineOfSight(aPlayer) ? ChatColor.DARK_RED : ChatColor.RED;

                        resultSet.add(color + aPlayer.getName() + " - " + player.getLocation().distance(aPlayer.getLocation()));
                    }

                    if (resultSet.isEmpty()) {
                        ChatUtil.sendNotice(player, "No players found.");
                    }

                    ChatUtil.sendNotice(player, "Player - Distance");
                    for (String string : resultSet) {
                        ChatUtil.sendNotice(player, string);
                    }
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamageEvent(EntityDamageEvent event) {

            Entity e = event.getEntity();

            if (!(e instanceof Player)) return;

            Player player = (Player) e;

            if (getTeam(player) != -1) {

                if (!isGameActive()) {
                    event.setCancelled(true);

                    if (event instanceof EntityDamageByEntityEvent) {

                        Entity attacker = ((EntityDamageByEntityEvent) event).getDamager();
                        boolean wasProjectile = attacker instanceof Projectile;
                        if (wasProjectile) {
                            ProjectileSource source = ((Projectile) attacker).getShooter();
                            if (source instanceof Entity) {
                                attacker = (Entity) source;
                            }
                        }
                        if (!(attacker instanceof Player)) return;
                        ChatUtil.sendError((Player) attacker, "The game has not yet started!");

                        if (!wasProjectile) {
                            attacker.teleport(new Location(world, config.x, config.y, config.z));
                        }
                    }
                    return;
                }

                switch (event.getCause()) {
                    case FALL:
                        if (LocationUtil.getBelowID(e.getLocation(), BlockID.LEAVES)
                                || (gameFlags.contains('s') && gameFlags.contains('j'))) {
                            server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                            server.getPluginManager().callEvent(new FallBlockerEvent(player));
                            if (ChanceUtil.getChance(2) || gameFlags.contains('j')) {
                                org.bukkit.util.Vector v = player.getLocation().getDirection();
                                v.setY(0);
                                v.setX(v.getX() > 0 ? -.5 : .5);
                                v.setZ(v.getZ() > 0 ? -.5 : .5);
                                player.setVelocity(new org.bukkit.util.Vector(0, .1, 0).multiply(event.getDamage()).add(v));
                            }
                            event.setCancelled(true);
                        }
                        break;
                    case BLOCK_EXPLOSION:
                        if ((gameFlags.contains('x') || gameFlags.contains('g'))
                                && !(event instanceof EntityDamageByEntityEvent)) {
                            event.setDamage(Math.min(event.getDamage(), 2));
                        }
                }
            } else if (contains(player.getLocation())) {
                player.teleport(player.getWorld().getSpawnLocation());
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

            CombatantPair<Player, Player, Projectile> result = extractor.extractFrom(event);

            if (result == null) return;

            Player attackingPlayer = result.getAttacker();
            Player defendingPlayer = result.getDefender();

            if (getTeam(attackingPlayer) == -1 && getTeam(defendingPlayer) != -1) {
                event.setCancelled(true);
                ChatUtil.sendWarning(attackingPlayer, "Don't attack Jungle Raiders.");
                return;
            }

            if (getTeam(attackingPlayer) == -1) return;
            if (getTeam(defendingPlayer) == -1) {
                ChatUtil.sendWarning(attackingPlayer, "Don't attack bystanders.");
                return;
            }

            if (isFriendlyFire(attackingPlayer, defendingPlayer)) {
                event.setCancelled(true);
                ChatUtil.sendWarning(attackingPlayer, "Don't hit your team mates!");
            } else {

                if (gameFlags.contains('T') && titan.equals(attackingPlayer.getName())) {
                    event.setDamage(event.getDamage() * 2);
                }

                if (gameFlags.contains('d')) {
                    event.setDamage(Math.pow(defendingPlayer.getMaxHealth(), 3));
                    ChatUtil.sendNotice(attackingPlayer, "You've killed " + defendingPlayer.getName() + "!");
                } else {
                    ChatUtil.sendNotice(attackingPlayer, "You've hit " + defendingPlayer.getName() + "!");
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerDeath(PlayerDeathEvent event) {

            final Player player = event.getEntity();
            if (getTeam(player) != -1) {

                // Enable disabled Checks
                boolean isTitanEnabled = gameFlags.contains('T');
                boolean isTitan = titan.equals(player.getName());

                if (isTitanEnabled && isTitan) {
                    Player aPlayer = Bukkit.getPlayerExact(titan);
                    // antiCheat.unexempt(aPlayer, CheckType.FAST_BREAK);
                    // antiCheat.unexempt(aPlayer, CheckType.NO_SWING);
                    // antiCheat.unexempt(aPlayer, CheckType.AUTOTOOL);
                }

                // Normal Jungle Raid fireworks and stuff
                int killerColor = 0;
                int teamColor = getTeam(player);
                Player killer = player.getKiller();
                if (killer != null && getTeam(killer) != -1) {
                    killerColor = getTeam(killer);
                    if (isTitanEnabled && killer.isValid()) {
                        if (isTitan) {
                            titan = killer.getName();
                            try {
                                Player aPlayer = Bukkit.getPlayerExact(titan);
                                // antiCheat.exempt(aPlayer, CheckType.FAST_BREAK);
                                // antiCheat.exempt(aPlayer, CheckType.NO_SWING);
                                // antiCheat.exempt(aPlayer, CheckType.AUTOTOOL);
                            } catch (Exception ex) {
                                ChatUtil.sendNotice(getContainedPlayers(), ChatColor.RED, "[ERROR] Cannot find titan.");
                            }
                        } else if (titan.equals(killer.getName())) {
                            killerColor = -1;
                        }
                    }
                }

                final List<Color> colors;
                if (teamColor < 1) {
                    colors = Arrays.asList(Color.WHITE);
                } else {
                    colors = teamColor == 1 ? Arrays.asList(Color.BLUE) : Arrays.asList(Color.RED);
                }

                final List<Color> fades;
                if (killerColor == -1) {
                    fades = Arrays.asList(Color.BLACK);
                } else if (killerColor < 1) {
                    fades = Arrays.asList(Color.WHITE);
                } else {
                    fades = killerColor == 1 ? Arrays.asList(Color.BLUE) : Arrays.asList(Color.RED);
                }

                final Location playerLoc = player.getLocation().clone();

                for (int i = 0; i < 12; i++) {
                    server.getScheduler().runTaskLater(inst, () -> {
                        Firework firework = world.spawn(playerLoc, Firework.class);
                        FireworkMeta meta = firework.getFireworkMeta();
                        FireworkEffect.Builder builder = FireworkEffect.builder();
                        builder.flicker(ChanceUtil.getChance(2));
                        builder.trail(ChanceUtil.getChance(2));
                        builder.withColor(colors);
                        builder.withFade(fades);
                        meta.addEffect(builder.build());
                        meta.setPower(ChanceUtil.getRangedRandom(2, 5));
                        firework.setFireworkMeta(meta);
                    }, i * 4);
                }

                if (killer != null) {
                    if (killerColor == -1) {
                        event.setDeathMessage(player.getName() + " has been taken out by the titan");
                    } else {
                        event.setDeathMessage(player.getName() + " has been taken out by " + killer.getName());
                    }
                } else {
                    event.setDeathMessage(player.getName() + " is out");
                }
                event.getDrops().clear();
                event.setDroppedExp(0);

                left(player);
            }
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {

            final Player p = event.getPlayer();

            // Technically forced, but because this
            // happens from disconnect/quit button
            // we don't want it to count as forced
            server.getScheduler().runTaskLater(inst, () -> removeGoneFromTeam(p, false), 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerRespawn(PlayerRespawnEvent event) {

            final Player p = event.getPlayer();

            server.getScheduler().runTaskLater(inst, () -> removeGoneFromTeam(p, true), 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockDamage(BlockDamageEvent event) {

            Player player = event.getPlayer();
            Block block = event.getBlock();

            if (gameFlags.contains('T') && contains(block.getLocation())) {
                if (titan.equals(player.getName()) && block.getTypeId() != BlockID.BEDROCK) {
                    event.setInstaBreak(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockBreak(BlockBreakEvent event) {

            Player player = event.getPlayer();

            if (getTeam(player) != -1) {
                if (!isGameInitialised()) {
                    ChatUtil.sendError(player, "The game has not yet started.");
                    event.setCancelled(true);
                } else if (gameFlags.contains('b')) {
                    ChatUtil.sendError(player, "You cannot break blocks by hand this game.");
                    event.setCancelled(true);
                } else if (gameFlags.contains('m')) {
                    if (BlockType.isNaturalTerrainBlock(event.getBlock().getTypeId())) {
                        ChatUtil.sendError(player, "You cannot mine this game.");
                        event.setCancelled(true);
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockPlace(BlockPlaceEvent event) {

            Player player = event.getPlayer();

            if (getTeam(player) != -1 && !isGameInitialised()) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onFireSpread(BlockIgniteEvent event) {

            Location l = event.getBlock().getLocation();

            if (contains(l) && (progress == GameProgress.DONE || gameFlags.contains('f'))) {
                switch (event.getCause()) {
                    case SPREAD:
                        event.setCancelled(true);
                        break;
                }
            }
        }

        @EventHandler
        public void onTNTExplode(EntityExplodeEvent event) {

            for (Block block : event.blockList()) {
                if (contains(block.getLocation())) {
                    event.setYield(0);
                    break;
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {

            Player player = event.getPlayer();

            if (getTeam(player) != -1) left(event.getPlayer());
        }

        @EventHandler
        public void onZombieLocalSpawn(ApocalypseLocalSpawnEvent event) {

            if (getTeam(event.getPlayer()) != -1) event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void onEggDrop(EggDropEvent event) {

            if (contains(event.getLocation())) event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void onDarkAreaInjury(DarkAreaInjuryEvent event) {

            if (getTeam(event.getPlayer()) != -1) event.setCancelled(true);
        }

        @EventHandler
        public void onKick(PlayerKickEvent event) {

            if (getTeam(event.getPlayer()) != -1) event.setCancelled(true);
        }

        @EventHandler
        public void onProjectileLand(ProjectileHitEvent event) {

            Projectile p = event.getEntity();
            if (p.getShooter() == null || !(p.getShooter() instanceof Player)) return;
            LivingEntity shooter = (LivingEntity) p.getShooter();
            if (getTeam((Player) p.getShooter()) != -1 && isGameActive()) {

                int explosionSize = 2;

                if (p instanceof Arrow) {
                    if (gameFlags.contains('t')) {
                        if (gameFlags.contains('s')) explosionSize = 4;
                        for (Entity e : p.getNearbyEntities(16, 16, 16)) {
                            if (e.equals(shooter)) continue;
                            if (e instanceof LivingEntity) {
                                ((LivingEntity) e).damage(1, p);
                                if (ChanceUtil.getChance(5)) {
                                    EntityUtil.heal(shooter, 1);
                                }
                            }
                        }
                    }
                    if (gameFlags.contains('x')) {
                        if (gameFlags.contains('s')) explosionSize = 4;
                    } else return;
                }
                if (p instanceof Snowball) {
                    if (gameFlags.contains('g')) {
                        if (gameFlags.contains('s')) explosionSize = 10;
                        else explosionSize = 6;
                    } else return;
                }

                p.getWorld().createExplosion(p.getLocation(), explosionSize);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onBlockChangePreLog(BlockChangePreLogEvent event) {

            if (contains(event.getLocation())) event.setCancelled(true);
        }
    }

    public class Commands {

        @Command(aliases = {"jr", "ar"}, desc = "Jungle Raid Commands")
        @NestedCommand({NestedCommands.class})
        public void jungleRaidCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {

        @Command(aliases = {"join", "j"},
                usage = "[Player] [Team Number]", desc = "Join the Minigame",
                anyFlags = true, min = 0, max = 2)
        public void joinJungleRaidCmd(CommandContext args, CommandSender sender) throws CommandException {

            joinCmd(args, sender);
        }

        @Command(aliases = {"leave", "l"},
                usage = "[Player]", desc = "Leave the Minigame",
                min = 0, max = 1)
        public void leaveJungleRaidCmd(CommandContext args, CommandSender sender) throws CommandException {

            leaveCmd(args, sender);
        }

        @Command(aliases = {"reset", "r"}, desc = "Reset the Minigame.",
                flags = "p",
                min = 0, max = 0)
        public void resetJungleRaidCmd(CommandContext args, CommandSender sender) throws CommandException {

            resetCmd(args, sender);
        }

        @Command(aliases = {"start", "s"},
                usage = "", desc = "Minigame start command",
                anyFlags = true, min = 0, max = 0)
        public void startJungleRaidCmd(CommandContext args, CommandSender sender) throws CommandException {

            startCmd(args, sender);
        }
    }

    private WorldEditPlugin getWorldEdit() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldEdit");

        // WorldEdit may not be loaded
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            throw new UnknownPluginException("WorldEdit");
        }

        return (WorldEditPlugin) plugin;
    }

    private WorldGuardPlugin getWorldGuard() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }

        return (WorldGuardPlugin) plugin;
    }

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
}