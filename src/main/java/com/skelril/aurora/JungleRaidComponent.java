package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitConfiguration;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.data.ChunkStore;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.data.MissingWorldException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.snapshots.Snapshot;
import com.sk89q.worldedit.snapshots.SnapshotRestore;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.*;
import com.skelril.aurora.exceptions.UnkownPluginException;
import com.skelril.aurora.exceptions.UnsupportedPrayerException;
import com.skelril.aurora.prayer.Prayer;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.ItemUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import de.diddiz.LogBlock.events.BlockChangePreLogEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Jungle Raid", desc = "Warfare at it's best!")
@Depend(components = {AdminComponent.class, PrayerComponent.class}, plugins = {"WorldEdit", "WorldGuard"})
public class JungleRaidComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private ProtectedRegion region;
    private World world;
    private final Random random = new Random();
    private boolean gameHasBeenInitialised = false;
    private boolean allowAllRun = false;
    private boolean gameHasStarted = false;
    private LocalConfiguration config;
    private ConcurrentHashMap<String, Integer> teams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PlayerState> playerState = new ConcurrentHashMap<>();
    private Set<Character> gameFlags = new HashSet<>();
    private static Economy economy = null;
    private static final double BASE_AMT = 12;
    private short attempts = 0;

    private long start = 0;
    private int amt = 7;
    private static final int potionAmt = PotionType.values().length;

    @InjectComponent
    AdminComponent adminComponent;
    @InjectComponent
    PrayerComponent prayerComponent;

    private Prayer[] getPrayers(Player player) throws UnsupportedPrayerException {

        return new Prayer[] {
                prayerComponent.constructPrayer(player, PrayerType.BLINDNESS, TimeUnit.DAYS.toMillis(1)),
                prayerComponent.constructPrayer(player, PrayerType.WALK, TimeUnit.DAYS.toMillis(1))
        };
    }

    private static final ItemStack sword = new ItemStack(ItemID.IRON_SWORD);
    private static final ItemStack bow = new ItemStack(ItemID.BOW);
    private static final ItemStack tnt = new ItemStack(BlockID.TNT, 32);
    private static final ItemStack flintAndSteel = new ItemStack(ItemID.FLINT_AND_TINDER);
    private static final ItemStack shears = new ItemStack(ItemID.SHEARS);
    private static final ItemStack axe = new ItemStack(ItemID.IRON_AXE);
    private static final ItemStack steak = new ItemStack(ItemID.COOKED_BEEF, 64);
    private static final ItemStack arrows = new ItemStack(ItemID.ARROW, 64);

    // Player Management
    private void addToJungleRaidTeam(Player player, int teamNumber, Set<Character> flags) {

        teams.put(player.getName(), teamNumber);

        playerState.put(player.getName(), new PlayerState(player.getName(),
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getLevel(),
                player.getExp(),
                player.getLocation()));

        try {
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
                gear.add(sword);
                gear.add(bow);
            }

            for (int i = 0; i < 3; i++) gear.add(tnt.clone());
            gear.add(flintAndSteel);
            gear.add(shears);
            gear.add(axe);
            gear.add(steak);
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

            Location battleLoc = new Location(Bukkit.getWorld(config.worldName), config.x, config.y, config.z);

            player.teleport(battleLoc);

            prayerComponent.influencePlayer(player, getPrayers(player));
        } catch (Exception e) {
            Bukkit.broadcastMessage(ChatColor.RED + "An issue has been found in the configuration of the "
                    + "Jungle Raid Component.");
        }
        player.sendMessage(ChatColor.YELLOW + "You have joined the Jungle Raid.");
    }

    private boolean isInJungleRaidTeam(Player player) {

        return teams.containsKey(player.getName());
    }

    private void removeFromJungleRaidTeam(Player player) {

        teams.remove(player.getName());
    }

    private boolean isJungleRaidActive() {

        return gameHasStarted;
    }

    private boolean canAllRun() {

        return allowAllRun;
    }

    private boolean isJungleRaidInitialised() {

        return gameHasBeenInitialised;
    }

    public Player[] getContainedPlayers() {


        return getContainedPlayers(0);
    }

    public Player[] getContainedPlayers(int parentsUp) {

        List<Player> returnedList = new ArrayList<>();
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();

        for (Player player : server.getOnlinePlayers()) {

            if (LocationUtil.isInRegion(world, r, player)) returnedList.add(player);
        }
        return returnedList.toArray(new Player[returnedList.size()]);
    }

    public boolean contains(Location location) {

        return LocationUtil.isInRegion(world, region, location);
    }

    public boolean probe() {

        world = Bukkit.getWorld(config.worldName);
        try {
            region = getWorldGuard().getGlobalRegionManager().get(world).getRegion(config.region);
        } catch (UnkownPluginException |NullPointerException e) {
            if (attempts > 10) {
                e.printStackTrace();
                return false;
            }
            server.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {
                    attempts++;
                    probe();
                }
            }, 2);
        }

        return world != null && region != null;
    }

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());

        probe();
        setupEconomy();
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 10);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
        probe();
        saveInventories();
    }

    @Override
    public void disable() {

        saveInventories();
    }

    private void saveInventories() {

        for (Player player : server.getOnlinePlayers()) {
            restorePlayer(player, 0);
        }
    }

    @Override
    public void run() {

        try {
            if (teams.size() == 0 && !isJungleRaidInitialised()) return;

            if (isJungleRaidInitialised() && !canAllRun()) {
                for (Map.Entry<String, Integer> entry : teams.entrySet()) {
                    if (entry.getValue() > 1) continue;
                    try {
                        Player player = Bukkit.getPlayerExact(entry.getKey());
                        if (player == null) continue;
                        prayerComponent.uninfluencePlayer(player);
                        for (PotionEffectType potionEffectType : PotionEffectType.values()) {
                            if (potionEffectType == null) continue;
                            if (player.hasPotionEffect(potionEffectType)) player.removePotionEffect(potionEffectType);
                        }
                    } catch (Exception e) {
                        teams.remove(entry.getKey());
                        e.printStackTrace();
                    }
                }
            } else if (canAllRun() && !isJungleRaidActive()) {
                for (Map.Entry<String, Integer> entry : teams.entrySet()) {
                    if (entry.getValue() != 2) continue;
                    try {
                        Player player = Bukkit.getPlayerExact(entry.getKey());
                        if (player == null) continue;
                        prayerComponent.uninfluencePlayer(player);
                        for (PotionEffectType potionEffectType : PotionEffectType.values()) {
                            if (potionEffectType == null) continue;
                            if (player.hasPotionEffect(potionEffectType)) player.removePotionEffect(potionEffectType);
                        }
                    } catch (Exception e) {
                        teams.remove(entry.getKey());
                        e.printStackTrace();
                    }
                }
            }

            if (!isJungleRaidActive()) return;

            // Security
            for (Player player : getContainedPlayers()) {

                if (!player.getGameMode().equals(GameMode.SURVIVAL)) {
                    if (player.isFlying()) {
                        player.setAllowFlight(true);
                        player.setFlying(true);
                        player.setGameMode(GameMode.SURVIVAL);
                    } else player.setGameMode(GameMode.SURVIVAL);
                }
            }

            // Sudden death
            boolean suddenD = !gameFlags.contains('S')
                    && System.currentTimeMillis() - start >= TimeUnit.MINUTES.toMillis(15);
            if (suddenD) amt = 100;

            // Distributor
            if (gameFlags.contains('a') || gameFlags.contains('g') || gameFlags.contains('p') || suddenD) {

                BlockVector bvMax = region.getMaximumPoint();
                BlockVector bvMin = region.getMinimumPoint();

                for (int i = 0; i < ChanceUtil.getRandom(Math.min(100, amt)); i++) {

                    Vector v = LocationUtil.pickLocation(bvMin.getX(), bvMax.getX(),
                            bvMin.getZ(), bvMax.getZ()).add(0, bvMax.getY(), 0);
                    Location testLoc = new Location(world, v.getX(), v.getY(), v.getZ());

                    if (testLoc.getBlock().getTypeId() != BlockID.AIR) continue;

                    if (gameFlags.contains('a') || suddenD) {
                        TNTPrimed e = (TNTPrimed) world.spawnEntity(testLoc, EntityType.PRIMED_TNT);
                        e.setVelocity(new org.bukkit.util.Vector(
                                random.nextDouble() * 2.0 - 1.5,
                                random.nextDouble() * 2 * -1,
                                random.nextDouble() * 2.0 - 1.5));
                        if (ChanceUtil.getChance(4)) e.setIsIncendiary(true);
                    }
                    if (gameFlags.contains('p')) {
                        PotionType type = PotionType.values()[ChanceUtil.getRandom(potionAmt) - 1];
                        if (type == null) continue;
                        for (int ii = 0; ii < ChanceUtil.getRandom(5); ii++) {
                            ThrownPotion potion = (ThrownPotion) world.spawnEntity(testLoc, EntityType.SPLASH_POTION);
                            potion.setItem(new Potion(type).splash().toItemStack(1));
                            potion.setVelocity(new org.bukkit.util.Vector(
                                    random.nextDouble() * 2.0 - 1.75,
                                    0,
                                    random.nextDouble() * 2.0 - 1.75));
                        }
                    }
                    if (gameFlags.contains('g')) {
                        testLoc.getWorld().dropItem(testLoc, new ItemStack(ItemID.SNOWBALL, ChanceUtil.getRandom(3)));
                    }
                }
                if (ChanceUtil.getChance(gameFlags.contains('s') ? 9 : 25)) amt++;
            }

            // Team Counter
            int teamZero = 0;
            int teamOne = 0;
            int teamTwo = 0;
            for (String name : teams.keySet()) {
                try {
                    Player teamPlayer = Bukkit.getPlayerExact(name);

                    adminComponent.standardizePlayer(teamPlayer);
                    if (teams.get(teamPlayer.getName()) == 0) {
                        teamZero++;
                    } else if (teams.get(teamPlayer.getName()) == 1) {
                        teamOne++;
                    } else if (teams.get(teamPlayer.getName()) == 2) {
                        teamTwo++;
                    }
                } catch (Exception e) {
                    teams.remove(name);
                }
            }

            // Win Machine
            if (teamOne > 0 || teamTwo > 0 || teamZero > 0) {
                String winner;
                if (teamOne >= 1) {
                    if (teamTwo >= 1 || teamZero >= 1) return;
                    else winner = "Team one";
                } else if (teamTwo >= 1) {
                    if (teamOne >= 1 || teamZero >= 1) return;
                    else winner = "Team two";
                } else {
                    if (teamZero > 1) return;
                    else winner = teams.keySet().toArray(new String[teams.keySet().size()])[0];
                }
                Bukkit.broadcastMessage(ChatColor.GOLD + winner + " has won!");
            } else {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Tie game!");
            }

            for (String name : teams.keySet()) {
                try {
                    Player teamPlayer = Bukkit.getPlayerExact(name);
                    removeFromJungleRaidTeam(teamPlayer);
                    restorePlayer(teamPlayer, ChanceUtil.getRandom(10.00));
                } catch (Exception e) {
                    teams.remove(name);
                }
            }

            teams.clear();
            gameFlags.clear();

            restore();

            amt = 7;
            gameHasStarted = false;
            allowAllRun = false;
            gameHasBeenInitialised = false;
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.broadcastMessage(ChatColor.RED + "[WARNING] Jungle Raid logic failed to process.");
        }
    }

    private void restorePlayer(Player teamPlayer, double multiplier) {

        // Restore Player
        if (playerState.containsKey(teamPlayer.getName())) {

            if (multiplier > 0 && economy != null) {
                economy.depositPlayer(teamPlayer.getName(), BASE_AMT * multiplier);
                ChatUtil.sendNotice(teamPlayer, "You received: " + economy.format(BASE_AMT * multiplier)
                        + ' ' + economy.currencyNamePlural() + '.');
            }

            // Clear Player
            teamPlayer.getInventory().clear();
            teamPlayer.getInventory().setArmorContents(null);

            PlayerState identity = playerState.get(teamPlayer.getName());

            // Restore the contents
            teamPlayer.getInventory().setArmorContents(identity.getArmourContents());
            teamPlayer.getInventory().setContents(identity.getInventoryContents());
            teamPlayer.setHealth(identity.getHealth());
            teamPlayer.setFoodLevel(identity.getHunger());
            teamPlayer.setSaturation(identity.getSaturation());
            teamPlayer.setExhaustion(identity.getExhaustion());
            teamPlayer.setLevel(identity.getLevel());
            teamPlayer.setExp(identity.getExperience());
            teamPlayer.teleport(identity.getLocation());

            playerState.remove(teamPlayer.getName());
        }
    }

    private void restore() {

        BukkitConfiguration worldEditConfig = null;
        try {
            worldEditConfig = getWorldEdit().getLocalConfiguration();
        } catch (UnkownPluginException e) {
            e.printStackTrace();
        }
        if (worldEditConfig.snapshotRepo == null) {
            log.warning("No snapshots configured, restoration cancelled.");
            return;
        }

        try {
            // Discover chunks
            Location battleLoc = new Location(world, config.x, config.y, config.z);

            for (Entity entity : world.getEntitiesByClasses(Item.class, TNTPrimed.class)) {
                if (region.contains(BukkitUtil.toVector(entity.getLocation()))) {
                    entity.remove();
                }
            }

            final Snapshot snap = worldEditConfig.snapshotRepo.getDefaultSnapshot(config.worldName);

            if (snap == null) {
                log.warning("No snapshot could be found, restoration cancelled.");
                return;
            }

            final List<Chunk> chunkList = new ArrayList<>();
            chunkList.add(battleLoc.getChunk());

            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();

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
            final EditSession fakeEditor = new EditSession(new BukkitWorld(world), -1);
            for (final Chunk chunk : chunkList) {
                server.getScheduler().runTaskLater(inst, new Runnable() {

                    @Override
                    public void run() {

                        ChunkStore chunkStore;

                        try {
                            chunkStore = snap._getChunkStore();
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
                            SnapshotRestore restore = new SnapshotRestore(chunkStore, r);

                            try {
                                restore.restore(fakeEditor);
                            } catch (MaxChangedBlocksException e) {
                                log.warning("Congratulations! You got an error which makes no sense!");
                                e.printStackTrace();
                                return;
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
                    }
                }, 5 * chunkList.indexOf(chunk));
            }
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

        @Setting("jungle-raid-start-World")
        public String worldName = "City";
        @Setting("jungle-raid-start-X")
        public int x = -654;
        @Setting("jungle-raid-start-Y")
        public int y = 37;
        @Setting("jungle-raid-start-Z")
        public int z = -404;
        @Setting("jungle-raid-region")
        public String region = "carpe-diem-district-jungle-raid";

    }

    private final String[] cmdWhiteList = new String[] {
            "ar", "jr", "stopweather", "me", "say", "pm", "msg", "message", "whisper", "tell",
            "reply", "r", "mute", "unmute", "debug", "dropclear", "dc", "auth", "toggleeditwand"
    };

    @EventHandler(ignoreCancelled = true)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();
        if (isInJungleRaidTeam(player)) {
            String command = event.getMessage();
            boolean allowed = false;
            for (String cmd : cmdWhiteList) {
                if (command.toLowerCase().startsWith("/" + cmd)) {
                    allowed = true;
                    break;
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

        if (isInJungleRaidTeam(event.getPlayer()) && !isJungleRaidActive()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {

        Entity e = event.getEntity();

        if (!(e instanceof Player)) return;

        Player player = (Player) e;

        if (isInJungleRaidTeam(player)) {
            switch (event.getCause()) {
                case FALL:
                    if (LocationUtil.getBelowID(e.getLocation(), BlockID.LEAVES)
                            || (gameFlags.contains('s') && gameFlags.contains('j'))) {
                        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                        server.getPluginManager().callEvent(new FallBlockerEvent(player));
                        if (ChanceUtil.getChance(2) || gameFlags.contains('j')) {
                            player.setVelocity(new org.bukkit.util.Vector(
                                    random.nextDouble() * 2.0 - 1.5,
                                    random.nextDouble() * 2,
                                    random.nextDouble() * 2.0 - 1.5).add(player.getVelocity()));
                        }
                        event.setCancelled(true);
                    }
                    break;
                case BLOCK_EXPLOSION:
                    if ((gameFlags.contains('x') || gameFlags.contains('g'))
                            && !(event instanceof EntityDamageByEntityEvent)) {
                        event.setDamage(Math.min(event.getDamage(), 2));
                    }
                case FIRE:
                    if (!isJungleRaidActive()) event.setCancelled(true);
                    break;
            }
        } else if (contains(player.getLocation())) {
            player.teleport(player.getWorld().getSpawnLocation());
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

        Entity attackingEntity = event.getDamager();
        Entity defendingEntity = event.getEntity();

        if (!(defendingEntity instanceof Player)) return;
        Player defendingPlayer = (Player) defendingEntity;

        Player attackingPlayer;
        if (attackingEntity instanceof Player) {
            attackingPlayer = (Player) attackingEntity;
        } else if (attackingEntity instanceof Arrow) {
            if (!(((Arrow) attackingEntity).getShooter() instanceof Player)) return;
            attackingPlayer = (Player) ((Arrow) attackingEntity).getShooter();
        } else {
            return;
        }

        if (!isInJungleRaidTeam(attackingPlayer) && isInJungleRaidTeam(defendingPlayer)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(attackingPlayer, "Don't attack Jungle Raiders.");
            return;
        }

        if (!isInJungleRaidTeam(attackingPlayer)) return;
        if (!isInJungleRaidTeam(defendingPlayer)) {
            ChatUtil.sendWarning(attackingPlayer, "Don't attack bystanders.");
            return;
        }

        if (!isJungleRaidActive()) {
            event.setCancelled(true);
            ChatUtil.sendError(attackingPlayer, "The game has not yet started!");
            return;
        }

        if ((teams.get(attackingPlayer.getName()).equals(teams.get(defendingPlayer.getName())))
                && (teams.get(attackingPlayer.getName()) != 0)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(attackingPlayer, "Don't hit your team mates!");
        } else {
            if (gameFlags.contains('d')) {
                event.setDamage(200);
                ChatUtil.sendNotice(attackingPlayer, "You've killed " + defendingPlayer.getName() + "!");
            } else {
                ChatUtil.sendNotice(attackingPlayer, "You've hit " + defendingPlayer.getName() + "!");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        if (isInJungleRaidTeam(player)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            removeFromJungleRaidTeam(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        final Player p = event.getPlayer();

        server.getScheduler().runTaskLater(inst, new Runnable() {

            @Override
            public void run() {

                restorePlayer(p, 1);
            }
        }, 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        if (isInJungleRaidTeam(player)) {
            if (!isJungleRaidInitialised()) {
                event.setCancelled(true);
            } else if (gameFlags.contains('b')) {
                ChatUtil.sendError(player, "You cannot break blocks by hand this game.");
                event.setCancelled(true);
            } else if (gameFlags.contains('m')) {
                int bt = event.getBlock().getTypeId();
                if (bt == BlockID.STONE || bt == BlockID.DIRT || bt == BlockID.GRASS) event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (isInJungleRaidTeam(player) && !isJungleRaidInitialised()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireSpread(BlockBurnEvent event) {

        Location l = event.getBlock().getLocation();

        if (contains(l) && gameFlags.contains('f')) event.setCancelled(true);
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

        if (isInJungleRaidTeam(event.getPlayer())) removeFromJungleRaidTeam(event.getPlayer());
        restorePlayer(player, 0);
    }

    @EventHandler
    public void onZombieLocalSpawn(ApocalypseLocalSpawnEvent event) {

        if (isInJungleRaidTeam(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEggDrop(EggDropEvent event) {

        if (contains(event.getLocation())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDarkAreaInjury(DarkAreaInjuryEvent event) {

        if (isInJungleRaidTeam(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {

        if (isInJungleRaidTeam(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onProjectileLand(ProjectileHitEvent event) {

        Projectile p = event.getEntity();
        if (p.getShooter() == null || !(p.getShooter() instanceof Player)) return;
        if (isInJungleRaidTeam((Player) p.getShooter()) && isJungleRaidActive()) {

            int explosionSize = 2;

            if (p instanceof Arrow) {
                if (gameFlags.contains('t')) {
                    if (gameFlags.contains('s')) explosionSize = 4;
                    for (Entity e : p.getNearbyEntities(16, 16, 16)) {
                        if (e.equals(p.getShooter())) continue;
                        if (e instanceof LivingEntity) {
                            ((LivingEntity) e).damage(1, p);
                            if (ChanceUtil.getChance(5)) {
                                p.getShooter().setHealth(Math.min(p.getShooter().getHealth() + 1,
                                        p.getShooter().getMaxHealth()));
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

    public class Commands {

        @Command(aliases = {"jr","ar"}, desc = "Jungle Raid Commands")
        @NestedCommand({NestedCommands.class})
        public void jungleRaidCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {

        @Command(aliases = {"join", "j"},
                usage = "[Player] [Team Number]", desc = "Join the Jungle Raid.",
                flags = "az", min = 0, max = 2)
        @CommandPermissions({"aurora.jr"})
        public void joinJungleRaidCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) sender.sendMessage("You must be a player to use this command.");
            Player targetPlayer = (Player) sender;

            if (args.argsLength() == 0) {
                inst.checkPermission(sender, "aurora.jr.self.join");
                if (adminComponent.isAdmin(targetPlayer)) {
                    throw new CommandException("You must first leave admin mode.");
                } else if (!targetPlayer.getWorld().equals(Bukkit.getWorld(config.worldName))) {
                    throw new CommandException("You cannot access this Jungle Raid from that world.");
                } else if (isJungleRaidInitialised()) {
                    throw new CommandException("You cannot add players while a Jungle Raid game is active!");
                } else if (isInJungleRaidTeam(targetPlayer)) {
                    throw new CommandException("You are already in a Jungle Raid.");
                }
                addToJungleRaidTeam(targetPlayer, 0, args.getFlags());
            } else if (args.argsLength() == 1) {
                inst.checkPermission(sender, "aurora.jr.self.join");
                if (adminComponent.isAdmin(targetPlayer)) {
                    throw new CommandException("You must first leave admin mode.");
                } else if (!targetPlayer.getWorld().equals(Bukkit.getWorld(config.worldName))) {
                    throw new CommandException("You cannot access this Jungle Raid from that world.");
                } else if (isJungleRaidInitialised()) {
                    throw new CommandException("You cannot add players while a Jungle Raid game is active!");
                } else if (isInJungleRaidTeam(targetPlayer)) {
                    throw new CommandException("You are already in a Jungle Raid.");
                }

                try {
                    int integer = Integer.parseInt(args.getString(0));

                    if (integer > 2) throw new CommandException("Valid teams: 0, 1, 2.");
                    addToJungleRaidTeam(targetPlayer, integer, args.getFlags());
                } catch (Exception e) {
                    throw new CommandException("Valid teams: 0, 1, 2.");
                }

            } else if (args.argsLength() == 2) {
                targetPlayer = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                inst.checkPermission(sender, targetPlayer.getWorld(), "aurora.jr.other.join");
                if (adminComponent.isAdmin(targetPlayer)) {
                    throw new CommandException("That player must first leave admin mode.");
                } else if (!targetPlayer.getWorld().equals(Bukkit.getWorld(config.worldName))) {
                    throw new CommandException("That player cannot access this Jungle Raid from their world.");
                } else if (isJungleRaidInitialised()) {
                    throw new CommandException("You cannot add players while a Jungle Raid game is active!");
                } else if (isInJungleRaidTeam(targetPlayer)) {
                    throw new CommandException("That player is already in a Jungle Raid.");
                }
                try {
                    int integer = Integer.parseInt(args.getString(1));

                    if (integer > 2) throw new CommandException("Valid teams: 0, 1, 2.");
                    addToJungleRaidTeam(targetPlayer, integer, args.getFlags());
                } catch (Exception e) {
                    throw new CommandException("Valid teams: 0, 1, 2.");
                }
            }

            ChatUtil.sendNotice(targetPlayer, ChatColor.DARK_GREEN, "Currently present players:");
            for (Player player : getContainedPlayers()) {
                if (targetPlayer.equals(player)) continue;
                ChatUtil.sendNotice(targetPlayer, ChatColor.GREEN, player.getName());
                ChatUtil.sendNotice(player, ChatColor.DARK_GREEN,
                        targetPlayer.getName() + " has joined the Jungle Raid.");
            }
        }

        @Command(aliases = {"leave", "l"},
                usage = "[Player]", desc = "Leave the Jungle Raid.",
                min = 0, max = 1)
        @CommandPermissions({"aurora.jr"})
        public void leaveJungleRaidCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            Player targetPlayer = (Player) sender;

            if (args.argsLength() == 0) {
                inst.checkPermission(sender, "aurora.jr.self.leave");
            } else if (args.argsLength() == 1) {
                targetPlayer = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
                inst.checkPermission(sender, targetPlayer.getWorld(), "aurora.jr.other.leave");
            }

            if (!isInJungleRaidTeam(targetPlayer)) throw new CommandException("That player is not currently in a " +
                    "Jungle Raid team.");

            removeFromJungleRaidTeam(targetPlayer);
            restorePlayer(targetPlayer, 0);


            for (Prayer prayer : prayerComponent.getInfluences(targetPlayer)) {
                if (prayer.getEffect().getType().equals(PrayerType.BLINDNESS)
                        || prayer.getEffect().getType().equals(PrayerType.WALK)) {
                    prayerComponent.uninfluencePlayer(targetPlayer, prayer);
                }
            }

            if (teams.size() < 2 && (isJungleRaidActive() || isJungleRaidInitialised())) {
                for (String name : teams.keySet()) {
                    try {
                        Player teamPlayer = Bukkit.getPlayerExact(name);
                        removeFromJungleRaidTeam(teamPlayer);
                        restorePlayer(teamPlayer, 0);
                    } catch (Exception e) {
                        teams.remove(name);
                    }
                }

                teams.clear();
                gameFlags.clear();

                restore();

                gameHasStarted = false;
                allowAllRun = false;
                gameHasBeenInitialised = false;
            }
        }

        @Command(aliases = {"reset", "r"}, desc = "Reset the Jungle Raid.",
                flags = "p",
                min = 0, max = 0)
        @CommandPermissions({"aurora.jr.reset"})
        public void endJungleRaidCmd(CommandContext args, CommandSender sender) throws CommandException {

            for (String name : teams.keySet()) {
                try {
                    Player teamPlayer = Bukkit.getPlayerExact(name);
                    removeFromJungleRaidTeam(teamPlayer);
                    restorePlayer(teamPlayer, 0);
                } catch (Exception e) {
                    teams.remove(name);
                }
            }

            teams.clear();
            gameFlags.clear();

            if (args.hasFlag('p')) probe();
            restore();

            amt = 7;
            gameHasStarted = false;
            allowAllRun = false;
            gameHasBeenInitialised = false;
        }

        @Command(aliases = {"start", "s"},
                usage = "", desc = "Jungle Raid start command",
                flags = "sdajbtfmxghpS", min = 0, max = 0)
        @CommandPermissions({"aurora.jr.start"})
        public void startJungleRaidCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player) || !isInJungleRaidTeam((Player) sender)) {
                throw new CommandException("You must first join a Jungle Raid.");
            } else if (!(teams.size() >= 2)) {
                throw new CommandException("You need more players to start a Jungle Raid.");
            } else if (gameHasBeenInitialised) {
                throw new CommandException("This Jungle Raid has already been initialised.");
            }

            final Player[] players = getContainedPlayers();

            gameFlags.clear();
            gameFlags.addAll(args.getFlags());
            gameHasBeenInitialised = true;

            ChatUtil.sendNotice(players, ChatColor.GREEN, "Team one and all neutral players can now run!");
            if (gameFlags.size() > 0) {
                ChatUtil.sendNotice(players, ChatColor.GREEN + "The following flags are enabled: ");
                if (gameFlags.contains('h')) {
                    ChatUtil.sendWarning(players, "Survival Mode");
                    for (Player player : players) {
                        PlayerInventory inventory = player.getInventory();
                        ItemStack stack;
                        for (int i = 0; i < inventory.getContents().length; i++)  {
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
                if (gameFlags.contains('p')) ChatUtil.sendNotice(players, ChatColor.MAGIC, "Potion Plummet");
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

                if (gameFlags.contains('S')) ChatUtil.sendNotice(players, ChatColor.GOLD, "Sudden death disabled");
            }


            inst.getServer().getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                @Override
                public void run() {

                    ChatUtil.sendNotice(players, ChatColor.GREEN, "Team two can now run!");
                    allowAllRun = true;

                    inst.getServer().getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                        @Override
                        public void run() {

                            ChatUtil.sendNotice(players, ChatColor.GREEN, "Fighting can now commence!");
                            gameHasStarted = true;
                            start = System.currentTimeMillis();
                        }
                    }, 20 * 15);
                }
            }, (20 * 45)); // Multiply seconds by 20 to convert to ticks
        }
    }

    private WorldEditPlugin getWorldEdit() throws UnkownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldEdit");

        // WorldEdit may not be loaded
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            throw new UnkownPluginException("WorldEdit");
        }

        return (WorldEditPlugin) plugin;
    }

    private WorldGuardPlugin getWorldGuard() throws UnkownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new UnkownPluginException("WorldGuard");
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