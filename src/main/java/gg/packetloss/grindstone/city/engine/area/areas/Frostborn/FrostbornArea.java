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
import gg.packetloss.grindstone.ProtectedDroppedItemsComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.city.engine.area.PersistentArena;
import gg.packetloss.grindstone.exceptions.UnknownPluginException;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.item.itemstack.ProtectedSerializedItemStack;
import gg.packetloss.grindstone.util.item.itemstack.SerializableItemStack;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.restoration.BaseBlockRecordIndex;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import gg.packetloss.hackbook.AttributeBook;
import gg.packetloss.hackbook.exceptions.UnsupportedFeatureException;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.item.ItemNameCalculator.computeItemName;

@ComponentInformation(friendlyName = "Frostborn", desc = "The frozen king")
@Depend(components = {AdminComponent.class, ProtectedDroppedItemsComponent.class}, plugins = {"WorldGuard"})
public class FrostbornArea extends AreaComponent<FrostbornConfig> implements PersistentArena {
    protected static final int BASE_RAGE = -10;
    protected static final int ARENA_FLOOR_LEVEL = 66;

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected ProtectedDroppedItemsComponent dropProtector;

    protected Economy economy;

    protected ProtectedRegion gate_RG, entrance_RG;

    protected Snowman boss;
    protected Location gateOuter;
    protected Location gateInner;
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
    protected ArrayList<ProtectedSerializedItemStack> lootItems = new ArrayList<>();

    protected int rageModifier = BASE_RAGE;
    protected long lastDeath = 0;

    @Override
    public void setUp() {
        try {
            WorldGuardPlugin WG = APIUtil.getWorldGuard();
            world = server.getWorlds().get(0);
            gateOuter = new Location(world, -48.5, 81, 392, 270, 0);
            gateInner = new Location(world, -50.5, 81, 392, 90, 0);
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
        if (!isBossSpawned()) {
            if (lastDeath == 0 || System.currentTimeMillis() - lastDeath >= 1000 * 60 * 3) {
                spawnBoss();
                sendPlayersToGate();
                thawEntrance();
            }
        } else {
            // This is extremely weird. It seems upon restart if we call restoreBlocks as soon as we possibly can
            // this results in snow blocks having some sort of block update, which results in many of
            // the restored snow blocks turning into snow balls.
            //
            // This is a work around to only restore blocks when players are in the building.
            if (!isEmpty(1)) {
                restoreBlocks();
            }
            preventRestoreDrowning();

            if (!isEmpty()) {
                feedPlayers();
                runAttack();
            }
        }

        movePlayers();

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

        long timeOfAddition = System.currentTimeMillis();
        stacks.stream().map(SerializableItemStack::new).map(item -> {
            return new ProtectedSerializedItemStack(player.getUniqueId(), timeOfAddition, item);
        }).collect(Collectors.toCollection(() -> lootItems));

        // Clear the players inventory
        player.getInventory().setArmorContents(null);
        player.getInventory().clear();

        // Disable any flight powers
        GeneralPlayerUtil.takeFlightSafely(player);

        // Ensure no fall damage even if the player wasn't flying
        player.setFallDistance(0);

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

    private boolean isValidFountainBlock(Block block) {
        Material material = block.getType();
        return material == Material.SNOW_BLOCK || material == Material.GLOWSTONE || material == Material.AIR;
    }

    private Collection<Location> getRandomFountainOriginLocations() {
        List<Location> fountainLocations = new ArrayList<>();

        com.sk89q.worldedit.Vector min = region.getMinimumPoint();
        com.sk89q.worldedit.Vector max = region.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        do {
            int targetX = ChanceUtil.getRangedRandom(minX, maxX);
            int targetZ = ChanceUtil.getRangedRandom(minZ, maxZ);

            Block block = world.getBlockAt(targetX, ARENA_FLOOR_LEVEL, targetZ);
            if (!isValidFountainBlock(block)) {
                continue;
            }

            fountainLocations.add(block.getLocation().add(0, 1, 0));
        } while (fountainLocations.size() < config.fountainOrigins);

        return fountainLocations;
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
        BukkitTask fountainTaskExecutor = server.getScheduler().runTaskTimer(inst, fountainTask, 20, 7);
        fountainTask.setTask(fountainTaskExecutor);
    }

    private void createEvilSnowballVomit() {
        IntegratedRunnable snowballVomit = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                if (!isBossSpawned()) return true;
                for (int i = ChanceUtil.getRandom(4); i > 0; --i) {
                    Snowball snowball = boss.launchProjectile(Snowball.class);
                    Vector vector = new Vector(ChanceUtil.getRandom(1.3), 1, ChanceUtil.getRandom(1.3));
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
        rageModifier = BASE_RAGE;
        switch (specialNumber) {
            case 1:
                ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "THIS ONE IS GOING TO HURT!");
                Snowball snowball = boss.launchProjectile(Snowball.class);
                snowball.setMetadata("forstborn-avalanche", new FixedMetadataValue(inst, 1));
                break;
            case 2:
                ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "I AM SNOWTASTIC!");
                for (Location fountainLoc : getRandomFountainOriginLocations()) {
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
                            if (!isValidFountainBlock(newLoc.clone().add(0, -1, 0).getBlock())) {
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
                ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "ME NO FEEL SO GOOD...");
                server.getScheduler().runTaskLater(inst, () -> {
                    ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "BLAARRGGGHHHH!!!");
                    createEvilSnowballVomit();
                }, 20);
                break;
            case 4:
                ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "RELEASE THE BATS!");

                Collection<Player> players = getContained(Player.class);

                int totalBats = ChanceUtil.getRandomNTimes(60, 9) + 30;
                int batsPerPlayer = totalBats / players.size();

                for (Player player : getContained(Player.class)) {
                    for (int i = batsPerPlayer; i > 0; --i) {
                        Bat b = (Bat) getWorld().spawnEntity(player.getLocation().add(0, 6, 0), EntityType.BAT);
                        b.setMaxHealth(1);
                        b.setHealth(1);
                    }
                }
                break;
        }
    }

    private void aerialBombard(Location spawnLoc, int quantity, int chanceOfAvalanche) {
        for (int i = 0; i < quantity; ++i) {
            Snowball snowball = (Snowball) getWorld().spawnEntity(spawnLoc, EntityType.SNOWBALL);
            Vector vector = new Vector(
                    ChanceUtil.getRangedRandom(-.4, .4),
                    0,
                    ChanceUtil.getRangedRandom(-.4, .4)
            );
            snowball.setVelocity(vector);

            if (chanceOfAvalanche > 0) {
                snowball.setMetadata("forstborn-avalanche", new FixedMetadataValue(inst, chanceOfAvalanche));
            }
        }
    }

    public void runSnowbats() {
        for (Bat bat : getContained(Bat.class)) {
            Location spawnLoc = bat.getLocation().add(0, -1, 0);
            aerialBombard(spawnLoc, ChanceUtil.getRandom(3), 0);
        }
    }

    protected void createAvalanche(Location loc, int chanceOfCascade) {
        IntegratedRunnable snowballVomit = new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                if (!isBossSpawned()) return true;
                aerialBombard(loc, ChanceUtil.getRangedRandom(3, 9), chanceOfCascade);
                return true;
            }

            @Override
            public void end() {
            }
        };
        TimedRunnable aerialBombardTask = new TimedRunnable(snowballVomit, 40);
        BukkitTask aerialBombardTaskExecutor = server.getScheduler().runTaskTimer(inst, aerialBombardTask, 10, 4);
        aerialBombardTask.setTask(aerialBombardTaskExecutor);
    }


    public void runAttack() {
        boss.setTarget(CollectionUtil.getElement(getContained(Player.class)));

        runSnowbats();

        int baseChance = 10;
        int modifiedChance = Math.max(1, baseChance - Math.max(0, rageModifier));

        if (ChanceUtil.getChance(modifiedChance)) {
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

        try {
            AttributeBook.setAttribute(boss, AttributeBook.Attribute.MOVEMENT_SPEED, 0.6);
            AttributeBook.setAttribute(boss, AttributeBook.Attribute.FOLLOW_RANGE, 150);
        } catch (UnsupportedFeatureException ex) {
            ex.printStackTrace();
        }
    }

    public void sendPlayersToGate() {
        for (Player player : getContained(Player.class)) {
            player.teleport(gateOuter, TeleportCause.UNKNOWN);
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
        Collection<UUID> playerIds = players.stream().map(Entity::getUniqueId).collect(Collectors.toList());

        // Clear the players inventories
        for (Player player : players) {
            player.getInventory().setArmorContents(null);
            player.getInventory().clear();
        }

        // Clear items on the ground, and any lingering snowballs and bats
        getContained(Item.class, Snowball.class, Bat.class).forEach(Entity::remove);

        // Drop the loot
        Iterator<ProtectedSerializedItemStack> lootIt = lootItems.iterator();
        while (lootIt.hasNext()) {
            ProtectedSerializedItemStack lootItem = lootIt.next();
            long timeSinceAddition = System.currentTimeMillis() - lootItem.getAdditionDate();
            boolean stillProtected = timeSinceAddition <= TimeUnit.DAYS.toMillis(1);

            UUID ownerID = lootItem.getPlayer();
            if (!stillProtected || playerIds.contains(ownerID)) {
                ItemStack bukkitStack = lootItem.getItemStack().bukkitRestore();
                lootIt.remove();

                Optional<String> itemName = computeItemName(bukkitStack);
                boolean isPhantomItem = itemName.orElse("").contains("Phantom");
                if (!isPhantomItem && ChanceUtil.getChance(config.chanceofActivation)) {
                    if (ChanceUtil.getChance(config.chanceOfDupe)) {
                        Item firstSpawnedItem = world.dropItem(bossSpawnLoc, bukkitStack.clone());
                        if (stillProtected) {
                            dropProtector.protectDrop(firstSpawnedItem, ownerID);
                        }

                        ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.GOLD + "An item has been duplicated!");
                    } else {
                        ChatUtil.sendNotice(getContained(1, Player.class), ChatColor.DARK_RED + "An item has been destroyed!");
                        continue;
                    }
                }

                Item firstSpawnedItem = world.dropItem(bossSpawnLoc, bukkitStack.clone());
                if (stillProtected) {
                    dropProtector.protectDrop(firstSpawnedItem, ownerID);
                }
            }
        }

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
            IOUtil.toBinaryFile(getWorkingDir(), "general", generalIndex);
            IOUtil.toBinaryFile(getWorkingDir(), "loot", lootItems);
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
                lootItems = (ArrayList<ProtectedSerializedItemStack>) lootFileO;
                log.info("Loaded: " + lootItems.size() + " loot items for Frostborn.");
            } else {
                log.warning("Invalid item loot file found: " + lootFile.getName() + "!");
                log.warning("Attempting to use backup file...");
                lootFile = new File(getWorkingDir().getPath() + "/old-" + lootFile.getName());
                if (lootFile.exists()) {
                    lootFileO = IOUtil.readBinaryFile(lootFile);
                    if (lootFileO instanceof ArrayList) {
                        //noinspection unchecked
                        lootItems = (ArrayList<ProtectedSerializedItemStack>) lootFileO;
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
