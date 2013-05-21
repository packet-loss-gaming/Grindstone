package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.environment.CreepSpeakEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Lever;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class GraveYard extends AbstractRegionedArena implements MonitoredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    // Components
    private AdminComponent adminComponent;

    // Temple regions
    private ProtectedRegion temple, pressurePlateLockArea;

    // Block information
    private static Set<BaseBlock> breakable = new HashSet<>();

    static {
        breakable.add(new BaseBlock(BlockID.DIRT, -1));
        breakable.add(new BaseBlock(BlockID.TORCH, -1));
        breakable.add(new BaseBlock(BlockID.STONE_BRICK, 2));
        breakable.add(new BaseBlock(BlockID.WEB, -1));
    }

    private static Set<BaseBlock> autoBreakable = new HashSet<>();

    static {
        autoBreakable.add(new BaseBlock(BlockID.STEP, 5));
        autoBreakable.add(new BaseBlock(BlockID.STEP, 13));
        autoBreakable.add(new BaseBlock(BlockID.STONE_BRICK, 2));
    }

    private final Random random = new Random();

    // Head Stones
    private List<Location> headStones = new ArrayList<>();

    // Pressure Plate Lock
    // Use a boolean to store the check value instead of checking for every step
    private boolean isPressurePlateLocked = true;
    private ConcurrentHashMap<Location, Boolean> pressurePlateLocks = new ConcurrentHashMap<>();

    // Block Restoration Map
    private ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> map = new ConcurrentHashMap<>();

    public GraveYard(World world, ProtectedRegion[] regions, AdminComponent adminComponent) {

        super(world, regions[0]);

        this.temple = regions[1];
        this.pressurePlateLockArea = regions[2];
        this.adminComponent = adminComponent;

        findHeadStones();
        findPressurePlateLockLevers();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {

        if (contains(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {

        if (event.toThunderState()) {
            resetPressurePlateLock();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent event) {

        World world = event.getWorld();
        if (getWorld().equals(world) && world.isThundering()) {
            for (Location headStone : headStones) {
                if (ChanceUtil.getChance(18)) {
                    for (int i = 0; i < ChanceUtil.getRangedRandom(3, 6); i++) {
                        spawnAndArm(headStone, EntityType.ZOMBIE, true);
                    }
                }
            }
        }
    }

    private void localSpawn(Player player) {

        if (!ChanceUtil.getChance(3)) return;

        Block playerBlock = player.getLocation().getBlock();
        Location ls;

        for (int i = 0; i < ChanceUtil.getRandom(16 - playerBlock.getLightLevel()); i++) {

            ls = LocationUtil.findRandomLoc(playerBlock, 8, true, false);

            if (!BlockType.isTranslucent(ls.getBlock().getTypeId())) {
                ls = player.getLocation();
            }

            spawnAndArm(ls, EntityType.ZOMBIE, true);
        }
    }

    private void spawnAndArm(Location location, EntityType type, boolean allowItemPickup) {

        if (!location.getChunk().isLoaded()) return;

        Entity e = spawn(location, type);
        if (e == null) return;
        arm(e, allowItemPickup);
    }

    private Entity spawn(Location location, EntityType type) {

        if (location == null || !type.isAlive()) return null;
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        entity.setCustomName("Grave Zombie");
        entity.setCustomNameVisible(false);
        return entity;
    }

    private void arm(Entity e, boolean allowItemPickup) {

        if (!(e instanceof LivingEntity)) return;

        EntityEquipment equipment = ((LivingEntity) e).getEquipment();
        ((LivingEntity) e).setCanPickupItems(allowItemPickup);

        if (ChanceUtil.getChance(50)) {
            if (ChanceUtil.getChance(15)) {
                equipment.setArmorContents(ItemUtil.diamondArmour);
            } else {
                equipment.setArmorContents(ItemUtil.ironArmour);
            }

            if (ChanceUtil.getChance(4)) equipment.setHelmet(null);
            if (ChanceUtil.getChance(4)) equipment.setChestplate(null);
            if (ChanceUtil.getChance(4)) equipment.setLeggings(null);
            if (ChanceUtil.getChance(4)) equipment.setBoots(null);
        }

        if (ChanceUtil.getChance(50)) {
            ItemStack sword = new ItemStack(ItemID.IRON_SWORD);
            if (ChanceUtil.getChance(35)) sword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta meta = sword.getItemMeta();
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_ALL, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_UNDEAD, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.FIRE_ASPECT, ChanceUtil.getRandom(2), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.KNOCKBACK, ChanceUtil.getRandom(2), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.LOOT_BONUS_MOBS, ChanceUtil.getRandom(3), false);
            sword.setItemMeta(meta);
            equipment.setItemInHand(sword);
        }

        if (allowItemPickup) {
            equipment.setItemInHandDropChance(1);
            equipment.setHelmetDropChance(1);
            equipment.setChestplateDropChance(1);
            equipment.setLeggingsDropChance(1);
            equipment.setBootsDropChance(1);
        } else {
            equipment.setItemInHandDropChance(.55F);
            equipment.setHelmetDropChance(.55F);
            equipment.setChestplateDropChance(.55F);
            equipment.setLeggingsDropChance(.55F);
            equipment.setBootsDropChance(.55F);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();

        if (entity.getCustomName() != null) {
            String customName = entity.getCustomName();
            List<ItemStack> drops = event.getDrops();

            if (customName.equals("Grave Zombie")) {

                if (ChanceUtil.getChance(6000) || getWorld().isThundering() && ChanceUtil.getChance(4000)) {
                    drops.add(ItemUtil.GraveYard.batBow());
                }

                if (ChanceUtil.getChance(6000) || getWorld().isThundering() && ChanceUtil.getChance(4000)) {
                    drops.add(ItemUtil.GraveYard.gemOfDarkness(1));
                }

                if (ChanceUtil.getChance(400)) {
                    drops.add(ItemUtil.GraveYard.phantomGold(ChanceUtil.getRandom(3)));
                }

                if (ChanceUtil.getChance(1000000)) {
                    switch (ChanceUtil.getRandom(2)) {
                        case 1:
                            drops.add(ItemUtil.Fear.makeSword());
                            break;
                        case 2:
                            drops.add(ItemUtil.Fear.makeBow());
                            break;
                    }
                }

                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();

                    if (stack != null && stack.getTypeId() == ItemID.ROTTEN_FLESH) it.remove();
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {

        int fromType = event.getSource().getTypeId();

        if (fromType == BlockID.GRASS && contains(event.getBlock())) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {

        Block block = event.getBlock();
        BaseBlock baseBlock = new BaseBlock(block.getTypeId(), block.getData());
        if (contains(block) && !adminComponent.isAdmin(event.getPlayer())) {

            event.setCancelled(true);
            if (!accept(baseBlock, breakable)) {
                return;
            }

            block.setTypeId(0);
            map.put(block.getLocation(), new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), baseBlock));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (contains(event.getBlock()) && !adminComponent.isAdmin(event.getPlayer())) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {

        Block block = event.getBlock();
        Location contactedLoc = block.getLocation();
        if (LocationUtil.isInRegion(getWorld(), temple, contactedLoc)) {
            if (block.getTypeId() == BlockID.STONE_PRESSURE_PLATE && isPressurePlateLocked) {
                throwSlashPotion(contactedLoc);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        Block block = event.getClickedBlock();
        Location clickedLoc = block.getLocation();
        if (LocationUtil.isInRegion(getWorld(), temple, clickedLoc)) {
            if (event.getAction().equals(Action.PHYSICAL)) {
                if (block.getTypeId() == BlockID.STONE_PRESSURE_PLATE && isPressurePlateLocked) {
                    throwSlashPotion(clickedLoc);
                }
            }
        }
    }

    private static final PotionType[] thrownTypes = new PotionType[] {
            PotionType.INSTANT_DAMAGE, PotionType.POISON, PotionType.WEAKNESS,
            PotionType.SLOWNESS
    };

    private void throwSlashPotion(Location location) {

        ThrownPotion potionEntity = (ThrownPotion) getWorld().spawnEntity(location, EntityType.SPLASH_POTION);
        PotionType type = thrownTypes[ChanceUtil.getRandom(thrownTypes.length) - 1];
        Potion potion = new Potion(type);
        potion.setLevel(type.getMaxLevel());
        potion.setSplash(true);
        potionEntity.setItem(potion.toItemStack(1));
        potionEntity.setVelocity(new Vector(
                random.nextDouble() * .5 - .25,
                random.nextDouble() * .4 + .1,
                random.nextDouble() * .5 - .25
        ));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (LocationUtil.isInRegion(getWorld(), getRegion(), player.getLocation())) {

            List<ItemStack> drops = event.getDrops();
            makeGrave(player.getName(), ItemUtil.clone(drops.toArray(new ItemStack[drops.size()])));
            drops.clear();

            event.setDeathMessage(ChatColor.DARK_RED + "RIP ~ " + player.getName());
        }
    }

    private void makeGrave(String name, ItemStack[] itemStacks) {

        if (headStones.size() < 1) return;
        Location headStone = headStones.get(ChanceUtil.getRandom(headStones.size()) - 1).clone();
        BlockState signState = headStone.getBlock().getState();

        if (signState instanceof Sign) {

            Calendar calendar = Calendar.getInstance();
            // Why the month is zero based I'll never know
            int month = calendar.get(Calendar.MONTH) + 1;
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int year = calendar.get(Calendar.YEAR);

            ((Sign) signState).setLine(0, month + "/" + day + "/" + year);
            ((Sign) signState).setLine(1, "RIP");
            ((Sign) signState).setLine(2, name);
            signState.update();

            headStone.add(0, -2, 0);

            BlockState chestState = headStone.getBlock().getState();

            if (chestState instanceof Chest) {

                ((Chest) chestState).getInventory().clear();
                ((Chest) chestState).getInventory().addItem(itemStacks);
            } else {
                headStone.add(0, -1, 0);

                chestState = headStone.getBlock().getState();

                if (chestState instanceof Chest) {

                    ((Chest) chestState).getInventory().clear();
                    ((Chest) chestState).getInventory().addItem(itemStacks);
                }
            }
        }
    }

    private void findHeadStones() {

        com.sk89q.worldedit.Vector min = getRegion().getMinimumPoint();
        com.sk89q.worldedit.Vector max = getRegion().getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!checkHeadStone(x, 82, z)) {
                    checkHeadStone(x, 81, z);
                }
            }
        }
    }

    private boolean checkHeadStone(int x, int y, int z) {

        BlockState block = getWorld().getBlockAt(x, y, z).getState();
        if (!block.getChunk().isLoaded()) block.getChunk().load();
        if (block.getTypeId() == BlockID.WALL_SIGN) {
            headStones.add(block.getLocation());
            return true;
        }
        return false;
    }

    private void findPressurePlateLockLevers() {

        com.sk89q.worldedit.Vector min = pressurePlateLockArea.getMinimumPoint();
        com.sk89q.worldedit.Vector max = pressurePlateLockArea.getMaximumPoint();

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
                    if (!block.getChunk().isLoaded()) block.getChunk().load();
                    if (block.getTypeId() == BlockID.LEVER) {
                        Lever lever = (Lever) block.getData();
                        lever.setPowered(false);
                        block.setData(lever);
                        block.update(true);
                        pressurePlateLocks.put(block.getLocation(), !ChanceUtil.getChance(3));
                    }
                }
            }
        }
    }

    private boolean checkPressurePlateLock() {

        for (Map.Entry<Location, Boolean> lever : pressurePlateLocks.entrySet()) {

            if (!lever.getKey().getBlock().getChunk().isLoaded()) return false;
            Lever aLever = (Lever) lever.getKey().getBlock().getState().getData();
            if (aLever.isPowered() != lever.getValue()) return false;
        }
        return true;
    }

    private void resetPressurePlateLock() {

        BlockState state;
        for (Location entry : pressurePlateLocks.keySet()) {

            if (!entry.getBlock().getChunk().isLoaded()) entry.getBlock().getChunk().load();
            state = entry.getBlock().getState();
            Lever lever = (Lever) state.getData();
            lever.setPowered(false);
            state.setData(lever);
            state.update(true);
            pressurePlateLocks.put(entry, !ChanceUtil.getChance(3));
        }
    }

    private void breakBlock(Entity e, Location location) {

        int chance = e instanceof Player ? 2 : 6;

        Block block = location.getBlock();
        for (BlockFace face : EnvironmentUtil.getNearbyBlockFaces()) {
            Block aBlock = block.getRelative(face);
            if (!BlockType.canPassThrough(aBlock.getRelative(BlockFace.DOWN).getTypeId())) continue;
            BaseBlock aBB = new BaseBlock(aBlock.getTypeId(), aBlock.getData());
            if (ChanceUtil.getChance(chance) && accept(aBB, autoBreakable)) {
                map.put(aBlock.getLocation(), new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), aBB));
                aBlock.setTypeId(0);
            }
        }
    }

    private void fogPlayer(Player player) {

        if (ItemUtil.hasFearHelmet(player)) return;
        ItemStack[] inventoryContents = player.getInventory().getContents();
        if (ItemUtil.findItemOfName(inventoryContents, ChatColor.DARK_RED + "Gem of Darkness")) return;
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 6, 1));
    }

    @Override
    public void forceRestoreBlocks() {

        resetPressurePlateLock();
        BaseBlock b;
        for (Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e : map.entrySet()) {
            b = e.getValue().getValue();
            if (!e.getKey().getChunk().isLoaded()) e.getKey().getChunk().load();
            e.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
        }
        map.clear();
    }

    public void restoreBlocks() {

        int min = 1000 * 27;

        BaseBlock b;
        Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e;
        Iterator<Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>>> it = map.entrySet().iterator();

        while (it.hasNext()) {
            e = it.next();
            if ((System.currentTimeMillis() - e.getValue().getKey()) > min) {
                b = e.getValue().getValue();
                if (!e.getKey().getChunk().isLoaded()) e.getKey().getChunk().load();
                e.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
                it.remove();
            } else if (System.currentTimeMillis() - e.getValue().getKey() > (min / 20)
                    && EnvironmentUtil.isShrubBlock(e.getValue().getValue().getType())) {
                b = e.getValue().getValue();
                if (!e.getKey().getChunk().isLoaded()) e.getKey().getChunk().load();
                e.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
                it.remove();
            }
        }
    }

    @Override
    public void run() {

        if (isEmpty()) return;

        equalize();
        restoreBlocks();
        isPressurePlateLocked = !checkPressurePlateLock();

        Entity[] contained = getContainedEntities();
        for (Entity entity : contained) {

            if (!entity.isValid()) continue;

            // Cave Spider killer
            if (entity instanceof CaveSpider && entity.getLocation().getBlock().getLightFromSky() >= 10) {
                for (int i = 0; i < 20; i++) getWorld().playEffect(entity.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                entity.remove();
                continue;
            }

            // Auto break stuff
            Location belowLoc = entity.getLocation();
            breakBlock(entity, belowLoc);
            breakBlock(entity, belowLoc.add(0, -1, 0));
            breakBlock(entity, belowLoc.add(0, -1, 0));

            // People Code
            if (entity instanceof Player && isEvilMode(((Player) entity).getEyeLocation().getBlock())) {
                if (adminComponent.isAdmin((Player) entity)) continue;
                fogPlayer((Player) entity);
                localSpawn((Player) entity);
            }
        }
    }

    private boolean accept(BaseBlock baseBlock, Set<BaseBlock> baseBlocks) {

        for (BaseBlock aBaseBlock : baseBlocks) {

            if (baseBlock.equalsFuzzy(aBaseBlock)) return true;
        }
        return false;
    }

    private boolean isEvilMode(Block block) {

        // Weather/Day Check
        if (EnvironmentUtil.isNightTime(getWorld().getTime()) || getWorld().hasStorm()) return true;
        // Location
        //noinspection RedundantIfStatement,SimplifiableIfStatement
        if (LocationUtil.isInRegion(getWorld(), temple, block.getLocation()) && block.getY() < 93) {
            return block.getLightFromSky() < 15;
        }

        return false;
    }

    @Override
    public void disable() {

        forceRestoreBlocks();
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        for (Player player : getContainedPlayers()) {
            try {
                adminComponent.standardizePlayer(player);
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }
}