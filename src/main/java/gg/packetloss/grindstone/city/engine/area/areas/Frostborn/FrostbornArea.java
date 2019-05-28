package gg.packetloss.grindstone.city.engine.area.areas.Frostborn;

import com.sk89q.commandbook.util.entity.ProjectileUtil;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;
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
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.item.itemstack.SerializableItemStack;
import gg.packetloss.grindstone.util.restoration.BaseBlockRecordIndex;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@ComponentInformation(friendlyName = "Frostborn", desc = "The frozen king")
@Depend(components = {AdminComponent.class}, plugins = {"WorldGuard"})
public class FrostbornArea extends AreaComponent<FrostbornConfig> implements PersistentArena {
    @InjectComponent
    protected AdminComponent admin;

    protected Economy economy;

    protected ProtectedRegion gate_RG, entrance_RG;

    protected Snowman boss;
    protected Location gate;
    protected Location bossSpawnLoc;

    // Block information
    protected static Set<BaseBlock> breakable = new HashSet<>();

    static {
        breakable.add(new BaseBlock(BlockID.SNOW, -1));
        breakable.add(new BaseBlock(BlockID.SNOW_BLOCK, -1));
        breakable.add(new BaseBlock(BlockID.LIGHTSTONE, -1));
    }

    protected static Set<BaseBlock> restoreable = new HashSet<>();

    static {
        restoreable.add(new BaseBlock(BlockID.SNOW_BLOCK, -1));
        restoreable.add(new BaseBlock(BlockID.LIGHTSTONE, -1));
    }

    // Block Restoration
    protected BaseBlockRecordIndex generalIndex = new BaseBlockRecordIndex();

    // Items taken from players returned upon death
    protected ArrayList<SerializableItemStack> lootItems = new ArrayList<>();

    protected long lastDeath = 0;

    @Override
    public void setUp() {
        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();
            world = server.getWorlds().get(0);
            gate = new Location(world, -48.5, 81, 392, 270, 0);
            bossSpawnLoc = new Location(getWorld(), -137, 67, 392, 270, 0);
            RegionManager manager = WG.getRegionManager(world);
            String base = "glacies-mare-district-frostborn";
            region = manager.getRegion(base + "-arena");
            gate_RG = manager.getRegion(base + "-gate");
            entrance_RG = manager.getRegion(base + "-entrance");
            tick = 4 * 20;
            listener = new FrostbornListener(this);
            config = new FrostbornConfig();

            reloadData();
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
    }

    @Override
    public void run() {
        restoreBlocks();
        movePlayers();
        if (!isBossSpawned()) {
            if (lastDeath == 0 || System.currentTimeMillis() - lastDeath >= 1000 * 60 * 3) {
                spawnBoss();
                sendPlayersToGate();
                thawEntrance();
            }
        } else {
            preventRestoreDrowning();

            if (!isEmpty()) {
                feedPlayers();
                runAttack();
            }
        }
        writeData(true);
    }

    public void restoreBlocks() {
        generalIndex.revertByTime(1000 * config.timeToRestore);
    }

    public void movePlayer(Player player) {
        // Add the players inventory to loot list
        List<ItemStack> stacks = new ArrayList<>();
        stacks.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        stacks.addAll(Arrays.asList(player.getInventory().getContents()));
        stacks.removeIf(i -> i == null || i.getType() == Material.AIR);

        stacks.stream().map(SerializableItemStack::new).collect(Collectors.toCollection(() -> lootItems));

        // Clear the players inventory
        player.getInventory().setArmorContents(null);
        player.getInventory().clear();

        // Add a stack of snowballs so the player isn't totally helpless
        player.getInventory().addItem(new ItemStack(Material.SNOW_BALL, Material.SNOW_BALL.getMaxStackSize()));

        // Move them into the arena
        Location loc = new Location(getWorld(), -102, 67, 392, 90, 0);
        loc.add(ChanceUtil.getRangedRandom(-5, 5), 0, ChanceUtil.getRangedRandom(-5, 5));
        player.teleport(loc, TeleportCause.UNKNOWN);
    }

    private void movePlayers() {
        for (Player player : getContained(entrance_RG, Player.class)) {
            movePlayer(player);
        }
    }

    public void preventRestoreDrowning() {
        Location bossLoc = boss.getLocation();
        while (bossLoc.getBlock().getType() != Material.AIR) {
            bossLoc.add(0, 1, 0);
        }
        boss.teleport(bossLoc);
    }

    public void feedPlayers() {
        for (Player player : getContained(Player.class)) {
            player.setFoodLevel(20);
            player.setSaturation(5);
            player.setExhaustion(0);
        }
    }

    private void createEvilSnowballFountain(Location loc) {
        IntegratedRunnable snowballFountain = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                if (!isBossSpawned()) return true;
                for (int i = ChanceUtil.getRandom(9); i > 0; --i) {
                    ProjectileUtil.sendProjectileFromLocation(
                            loc,
                            new Vector(
                                    ChanceUtil.getRangedRandom(-.25, .25),
                                    1,
                                    ChanceUtil.getRangedRandom(-.25, .25)
                            ),
                            .6F,
                            Snowball.class
                    );
                }
                return true;
            }

            @Override
            public void end() {
            }
        };
        TimedRunnable fountainTask = new TimedRunnable(snowballFountain, 24);
        BukkitTask fountainTaskExecutor = server.getScheduler().runTaskTimer(inst, fountainTask, 10, 7);
        fountainTask.setTask(fountainTaskExecutor);
    }

    private void createEvilSnowballVomit() {
        IntegratedRunnable snowballVomit = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                if (!isBossSpawned()) return true;
                for (int i = ChanceUtil.getRandom(4); i > 0; --i) {
                    Snowball snowball = boss.launchProjectile(Snowball.class);
                    Vector vector = new Vector(ChanceUtil.getRandom(2.0), 1, ChanceUtil.getRandom(2.0));
                    snowball.setVelocity(snowball.getVelocity().multiply(vector));
                }
                return true;
            }

            @Override
            public void end() {
            }
        };
        TimedRunnable snowballVomitTask = new TimedRunnable(snowballVomit, 40);
        BukkitTask snowballVomitTaskExecutor = server.getScheduler().runTaskTimer(inst, snowballVomitTask, 10, 4);
        snowballVomitTask.setTask(snowballVomitTaskExecutor);
    }

    public void runSpecial(int specialNumber) {
        switch (specialNumber) {
            case 1:
                ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "BOW TO THE KING!");
                ProjectileUtil.sendProjectilesFromLocation(boss.getLocation(), 25, 2F, Snowball.class);
                break;
            case 2:
                ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "I AM SNOWTASTIC!");
                for (Player player : getContained(Player.class)) {
                    Location fountainLoc = player.getLocation();
                    createEvilSnowballFountain(fountainLoc);

                    Location targetLoc = fountainLoc;
                    int requested = ChanceUtil.getRandom(7);
                    for (int maxTries = 10; requested > 0 && maxTries > 0; --maxTries) {
                        Location newLoc = LocationUtil.findRandomLoc(
                                targetLoc,
                                15,
                                true,
                                true
                        );

                        if (contains(newLoc)) {
                            Material lowerBlockMat = newLoc.clone().add(0, -1, 0).getBlock().getType();
                            if (lowerBlockMat != Material.SNOW_BLOCK && lowerBlockMat != Material.GLOWSTONE) {
                                continue;
                            }

                            createEvilSnowballFountain(newLoc);
                            --requested;

                            targetLoc = newLoc;
                        }
                    }
                }
                break;
            case 3:
                ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "BLAARRGGGHHHH!!!");
                createEvilSnowballVomit();
                break;
            case 4:
                ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "RELEASE THE BATS!");
                for (Player player : getContained(Player.class)) {
                    for (int i = ChanceUtil.getRandomNTimes(20, 3) + 10; i > 0; --i) {
                        Bat b = (Bat) getWorld().spawnEntity(player.getLocation().add(0, 4, 0), EntityType.BAT);
                        b.setMaxHealth(50);
                        b.setHealth(50);
                    }
                }
                break;
        }
    }

    public void runSnowbats() {
        for (Bat bat : getContained(Bat.class)) {
            Location spawnLoc = bat.getLocation().add(0, -1, 0);
            for (int i = ChanceUtil.getRandom(3); i > 0; --i) {
                Snowball snowball = (Snowball) getWorld().spawnEntity(spawnLoc, EntityType.SNOWBALL);
                Vector vector = new Vector(
                        ChanceUtil.getRangedRandom(-.5, .5),
                        0,
                        ChanceUtil.getRangedRandom(-.5, .5)
                );
                snowball.setVelocity(vector);
            }
        }
    }

    public void runAttack() {
        boss.setTarget(CollectionUtil.getElement(getContained(Player.class)));

        if (!boss.hasLineOfSight(boss.getTarget())) {
            Location targetLoc;
            do {
                targetLoc = LocationUtil.findRandomLoc(
                        boss.getTarget().getLocation(),
                        15,
                        false,
                        true
                );
            } while (!contains(targetLoc));

            boss.teleport(targetLoc);
        }

        runSnowbats();

        if (ChanceUtil.getChance(10)) {
            runSpecial(ChanceUtil.getRandom(4));
        }
    }

    public boolean isArenaLoaded() {
        BlockVector min = getRegion().getMinimumPoint();
        BlockVector max = getRegion().getMaximumPoint();
        Region region = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(getWorld(), region.getCenter()).getChunk().isLoaded();
    }

    public boolean isBossSpawned() {
        if (!isArenaLoaded()) return true;
        boolean found = false;
        boolean second = false;
        for (Snowman e : getContained(Snowman.class)) {
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
            getContained(Snowman.class).stream().filter(e -> e.isValid() && !e.equals(boss)).forEach(Entity::remove);
        }
        return boss != null && boss.isValid();
    }

    public void spawnBoss() {
        boss = getWorld().spawn(bossSpawnLoc, Snowman.class);
        boss.setMaxHealth(1750);
        boss.setHealth(1750);
        boss.setRemoveWhenFarAway(false);
    }

    public void sendPlayersToGate() {
        for (Player player : getContained(Player.class)) {
            player.teleport(gate, TeleportCause.UNKNOWN);
        }
    }

    private void replaceBlocksInEntrance(Material from, Material to) {
        com.sk89q.worldedit.Vector min = entrance_RG.getMinimumPoint();
        com.sk89q.worldedit.Vector max = entrance_RG.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = getWorld().getBlockAt(x, y, z);
                    if (!block.getChunk().isLoaded()) block.getChunk().load();
                    if (block.getType() == from) {
                        block.setType(to);
                    }
                }
            }
        }

    }

    public void thawEntrance() {
        replaceBlocksInEntrance(Material.ICE, Material.AIR);
    }

    public void freezeEntrance() {
        replaceBlocksInEntrance(Material.AIR, Material.ICE);
    }

    protected boolean accept(BaseBlock baseBlock, Set<BaseBlock> baseBlocks) {
        for (BaseBlock aBaseBlock : baseBlocks) {
            if (baseBlock.equalsFuzzy(aBaseBlock)) return true;
        }
        return false;
    }

    protected void dropLoot() {
        generalIndex.revertAll();

        // Gather the players in the arena
        Collection<Player> players = getContained(Player.class);

        // Clear the players inventories
        for (Player player : players) {
            player.getInventory().setArmorContents(null);
            player.getInventory().clear();
        }

        // Clear items on the ground, and any lingering snowballs and bats
        getContained(Item.class, Snowball.class, Bat.class).forEach(Entity::remove);

        // Drop the loot
        for (SerializableItemStack lootItem : lootItems) {
            ItemStack bukkitStack = lootItem.bukkitRestore();
            world.dropItem(bossSpawnLoc, bukkitStack.clone());
            if (ChanceUtil.getChance(config.chanceOfDupe)) {
                world.dropItem(bossSpawnLoc, bukkitStack.clone());
                ChatUtil.sendNotice(getContained(1, Player.class), "An item has been duplicated!");
            }
        }
        lootItems.clear();

        // Drop some additional holdover loot
        for (int i = ChanceUtil.getRandom(8) * players.size(); i > 0; --i) {
            world.dropItem(bossSpawnLoc, new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRangedRandom(32, 64)));
        }

        for (int i = 0; i < players.size(); ++i) {
            if (ChanceUtil.getChance(100)) {
                world.dropItem(bossSpawnLoc, CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
            }
        }

        // Teleport the players to a reasonable location where they'll see the loot
        for (Player player : players) {
            Location loc = bossSpawnLoc.clone();
            loc.add(ChanceUtil.getRangedRandom(10, 15), 0, ChanceUtil.getRangedRandom(-5, 5));
            loc.setYaw(90);
            player.teleport(loc, TeleportCause.UNKNOWN);
        }
    }

    @Override
    public void writeData(boolean doAsync) {
        Runnable run = () -> {
            generalFile:
            {
                File generalFile = new File(getWorkingDir().getPath() + "/general.dat");
                if (generalFile.exists()) {
                    Object generalFileO = IOUtil.readBinaryFile(generalFile);

                    if (generalIndex.equals(generalFileO)) {
                        break generalFile;
                    }
                }
                IOUtil.toBinaryFile(getWorkingDir(), "general", generalIndex);
            }
            lootFile:
            {
                File lootFile = new File(getWorkingDir().getPath() + "/loot.dat");
                if (lootFile.exists()) {
                    Object lootFileO = IOUtil.readBinaryFile(lootFile);

                    if (lootItems.equals(lootFileO)) {
                        break lootFile;
                    }
                }
                IOUtil.toBinaryFile(getWorkingDir(), "loot", lootItems);
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
        File generalFile = new File(getWorkingDir().getPath() + "/general.dat");
        if (generalFile.exists()) {
            Object generalFileO = IOUtil.readBinaryFile(generalFile);
            if (generalFileO instanceof BaseBlockRecordIndex) {
                generalIndex = (BaseBlockRecordIndex) generalFileO;
                log.info("Loaded: " + generalIndex.size() + " general records for Frostborn.");
            } else {
                log.warning("Invalid block record file encountered: " + generalFile.getName() + "!");
                log.warning("Attempting to use backup file...");
                generalFile = new File(getWorkingDir().getPath() + "/old-" + generalFile.getName());
                if (generalFile.exists()) {
                    generalFileO = IOUtil.readBinaryFile(generalFile);
                    if (generalFileO instanceof BaseBlockRecordIndex) {
                        generalIndex = (BaseBlockRecordIndex) generalFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + generalIndex.size() + " general records for Frostborn.");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }

        File lootFile = new File(getWorkingDir().getPath() + "/loot.dat");
        if (lootFile.exists()) {
            Object lootFileO = IOUtil.readBinaryFile(lootFile);
            if (lootFileO instanceof ArrayList) {
                //noinspection unchecked
                lootItems = (ArrayList<SerializableItemStack>) lootFileO;
                log.info("Loaded: " + lootItems.size() + " loot items for Frostborn.");
            } else {
                log.warning("Invalid item loot file found: " + lootFile.getName() + "!");
                log.warning("Attempting to use backup file...");
                lootFile = new File(getWorkingDir().getPath() + "/old-" + lootFile.getName());
                if (lootFile.exists()) {
                    lootFileO = IOUtil.readBinaryFile(lootFile);
                    if (lootFileO instanceof ArrayList) {
                        //noinspection unchecked
                        lootItems = (ArrayList<SerializableItemStack>) lootFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + lootItems.size() + " loot items for Frostborn.");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }
    }
}