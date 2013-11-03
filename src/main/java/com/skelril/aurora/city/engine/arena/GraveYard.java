package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PlayerSacrificeItemEvent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.events.apocalypse.GemOfLifeUsageEvent;
import com.skelril.aurora.events.environment.CreepSpeakEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.database.IOUtil;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.skelril.aurora.util.restoration.BaseBlockRecordIndex;
import com.skelril.aurora.util.restoration.BlockRecord;
import com.skelril.aurora.util.restoration.RestorationUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Lever;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class GraveYard extends AbstractRegionedArena implements MonitoredArena, PersistentArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    // Components
    private AdminComponent adminComponent;

    // Other
    private Economy economy;

    // Temple regions
    private ProtectedRegion temple, pressurePlateLockArea, rewards;

    // Block information
    private static Set<BaseBlock> breakable = new HashSet<>();

    static {
        breakable.add(new BaseBlock(BlockID.LONG_GRASS, -1));
        breakable.add(new BaseBlock(BlockID.RED_FLOWER, -1));
        breakable.add(new BaseBlock(BlockID.YELLOW_FLOWER, -1));
        breakable.add(new BaseBlock(BlockID.DIRT, -1));
        breakable.add(new BaseBlock(BlockID.TORCH, -1));
        breakable.add(new BaseBlock(BlockID.STONE_BRICK, 2));
        breakable.add(new BaseBlock(BlockID.WEB, -1));
        breakable.add(new BaseBlock(BlockID.LEAVES, -1));
    }

    private static Set<BaseBlock> autoBreakable = new HashSet<>();

    static {
        autoBreakable.add(new BaseBlock(BlockID.STEP, 5));
        autoBreakable.add(new BaseBlock(BlockID.STEP, 13));
        autoBreakable.add(new BaseBlock(BlockID.WOODEN_STEP, 8));
        autoBreakable.add(new BaseBlock(BlockID.STONE_BRICK, 2));
    }

    private final Random random = new Random();

    // Head Stones
    private List<Location> headStones = new ArrayList<>();

    // Reward Chest
    private List<Location> rewardChest = new ArrayList<>();

    // Pressure Plate Lock
    // Use a boolean to store the check value instead of checking for every step
    private boolean isPressurePlateLocked = true;
    private ConcurrentHashMap<Location, Boolean> pressurePlateLocks = new ConcurrentHashMap<>();

    // Block Restoration
    private BaseBlockRecordIndex generalIndex = new BaseBlockRecordIndex();
    // Respawn Inventory Map
    private HashMap<String, PlayerState> playerState = new HashMap<>();

    public GraveYard(World world, ProtectedRegion[] regions, AdminComponent adminComponent) {

        super(world, regions[0]);

        this.temple = regions[1];
        this.pressurePlateLockArea = regions[2];
        this.rewards = regions[3];
        this.adminComponent = adminComponent;

        findHeadStones();
        findPressurePlateLockLevers();
        findRewardChest();

        resetRewardChest();

        reloadData();
        setupEconomy();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    public Player[] getTempleContained() {

        List<Player> returnedList = new ArrayList<>();

        for (Player player : server.getOnlinePlayers()) {

            if (player.isValid() && isHostileTempleArea(player.getLocation())) returnedList.add(player);
        }
        return returnedList.toArray(new Player[returnedList.size()]);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {

        if (contains(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (isHostileTempleArea(event.getPlayer().getLocation())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onThunderChange(ThunderChangeEvent event) {

        if (!event.getWorld().equals(getWorld())) return;

        if (event.toThunderState()) {

            resetPressurePlateLock();
            isPressurePlateLocked = !checkPressurePlateLock();
            resetRewardChest();

            List<Player> returnedList = new ArrayList<>();

            for (Player player : server.getOnlinePlayers()) {

                if (player.isValid() && LocationUtil.isInRegion(getWorld(), rewards, player)) returnedList.add(player);
            }

            for (Player player : returnedList) {
                ChatUtil.sendNotice(player, ChatColor.DARK_RED + "You dare disturb our graves!");
                ChatUtil.sendNotice(player, ChatColor.DARK_RED + "Taste the wrath of thousands!");
                for (int i = 0; i < 15; i++) {
                    localSpawn(player, true);
                }
            }
        } else {

            ChatUtil.sendNotice(getContainedPlayers(), ChatColor.DARK_RED, "Rawwwgggggghhhhhhhhhh......");
            for (Entity entity : getContainedEntities(Zombie.class)) {

                if (!ChanceUtil.getChance(5)) ((Zombie) entity).setHealth(0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent event) {

        World world = event.getWorld();
        if (getWorld().equals(world) && world.isThundering()) {
            for (Location headStone : headStones) {
                if (world.getEntitiesByClass(Zombie.class).size() > 1000) return;
                if (ChanceUtil.getChance(18)) {
                    for (int i = 0; i < ChanceUtil.getRangedRandom(3, 6); i++) {
                        spawnAndArm(headStone, EntityType.ZOMBIE, true);
                    }
                }
            }
        }
    }

    private static Set<PotionEffectType> excludedTypes = new HashSet<>();

    static {
        excludedTypes.add(PotionEffectType.SLOW);
        excludedTypes.add(PotionEffectType.POISON);
        excludedTypes.add(PotionEffectType.WEAKNESS);
        excludedTypes.add(PotionEffectType.REGENERATION);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        Entity aDefender = event.getEntity();
        Entity aAttacker = event.getDamager();

        if (!(aDefender instanceof LivingEntity)) return;
        if (isHostileTempleArea(event.getEntity().getLocation())) {

            double damage = event.getDamage();
            LivingEntity defender = (LivingEntity) aDefender;
            if (ItemUtil.hasAncientArmour(defender) && !(getWorld().isThundering() && defender instanceof Player)) {
                double diff = defender.getMaxHealth() - defender.getHealth();
                if (ChanceUtil.getChance((int) Math.max(3, Math.round(defender.getMaxHealth() - diff)))) {
                    EffectUtil.Ancient.powerBurst(defender, damage);
                }
            }

            if (aAttacker instanceof Player) {
                Player player = (Player) aAttacker;

                for (PotionEffect effect : player.getActivePotionEffects()) {

                    if (!excludedTypes.contains(effect.getType())) {
                        defender.addPotionEffect(effect);
                    }
                }

                if (getWorld().isThundering()) return;

                if (ItemUtil.hasMasterSword(player)) {

                    if (ChanceUtil.getChance(10)) {
                        EffectUtil.Master.healingLight(player, defender);
                    }

                    if (ChanceUtil.getChance(18)) {
                        List<LivingEntity> entities = new ArrayList<>();
                        for (Entity e : player.getNearbyEntities(6, 4, 6)) {

                            if (EnvironmentUtil.isHostileEntity(e)) entities.add((LivingEntity) e);
                        }
                        EffectUtil.Master.doomBlade(player, entities);
                    }
                }
            } else if (defender instanceof Player) {

                Player player = (Player) defender;
                Iterator<PotionEffect> potionIt = player.getActivePotionEffects().iterator();
                while (potionIt.hasNext()) {

                    potionIt.next();
                    if (ChanceUtil.getChance(18)) {
                        potionIt.remove();
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {

        for (Entity entity : event.getAffectedEntities()) {
            if (entity != null && entity instanceof Player && ChanceUtil.getChance(14)) {
                if (ChanceUtil.getChance(14)) {
                    ((Player) entity).removePotionEffect(PotionEffectType.REGENERATION);
                }
                if (ChanceUtil.getChance(14)) {
                    ((Player) entity).removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                }
            }
        }
    }

    private void localSpawn(Player player) {

        localSpawn(player, false);
    }

    private void localSpawn(Player player, boolean bypass) {

        if (!ChanceUtil.getChance(3) && !bypass) return;

        Block playerBlock = player.getLocation().getBlock();
        Location ls;

        if (LocationUtil.isInRegion(getWorld(), rewards, player)) {
            for (int i = 0; i < 3; i++) {

                ls = LocationUtil.findRandomLoc(playerBlock, 8, true, false);

                if (!BlockType.isTranslucent(ls.getBlock().getTypeId())) {
                    ls = player.getLocation();
                }

                Zombie zombie = (Zombie) spawn(ls, EntityType.ZOMBIE, "Guardian Zombie");
                EntityEquipment equipment = zombie.getEquipment();

                equipment.setArmorContents(new ItemStack[]{
                        ItemUtil.Ancient.makeBoots(), ItemUtil.Ancient.makeLegs(),
                        ItemUtil.Ancient.makeChest(), ItemUtil.Ancient.makeHelmet()
                });
                equipment.setItemInHand(ItemUtil.God.makeSword());

                // Drop Chances
                equipment.setItemInHandDropChance(0);
                equipment.setHelmetDropChance(0);
                equipment.setChestplateDropChance(0);
                equipment.setLeggingsDropChance(0);
                equipment.setBootsDropChance(0);
            }
            return;
        }

        Block aBlock;

        for (int i = 0; i < ChanceUtil.getRandom(16 - playerBlock.getLightLevel()); i++) {

            ls = LocationUtil.findRandomLoc(playerBlock, 8, true, false);

            if (!BlockType.isTranslucent(ls.getBlock().getTypeId())) {
                ls = player.getLocation();
            }

            aBlock = ls.getBlock().getRelative(BlockFace.DOWN);
            // If the block is a half slab or it is wood, don't do this
            if (aBlock.getTypeId() != BlockID.STEP && aBlock.getTypeId() != BlockID.WOOD) {
                aBlock = aBlock.getRelative(BlockFace.DOWN, 2);
                if (BlockType.canPassThrough(aBlock.getTypeId())) {
                    ls.add(0, -3, 0);
                }
            }

            spawnAndArm(ls, EntityType.ZOMBIE, true);
        }
    }

    private Entity spawnAndArm(Location location, EntityType type, boolean allowItemPickup) {

        if (!location.getChunk().isLoaded()) return null;

        Entity e = spawn(location, type);
        if (e == null) return null;
        arm(e, allowItemPickup);
        return e;
    }

    private Entity spawn(Location location, EntityType type) {

        return spawn(location, type, "Grave Zombie");
    }

    private Entity spawn(Location location, EntityType type, String name) {

        if (location == null || !type.isAlive()) return null;
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        entity.setCustomName(name);
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
            equipment.setItemInHandDropChance(equipment.getItemInHand() == null ? 1 : .35F);
            equipment.setHelmetDropChance(equipment.getHelmet() == null ? 1 : .35F);
            equipment.setChestplateDropChance(equipment.getChestplate() == null ? 1 : .35F);
            equipment.setLeggingsDropChance(equipment.getLeggings() == null ? 1 : .35F);
            equipment.setBootsDropChance(equipment.getBoots() == null ? 1 : .35F);
        } else {
            equipment.setItemInHandDropChance(.17F);
            equipment.setHelmetDropChance(.17F);
            equipment.setChestplateDropChance(.17F);
            equipment.setLeggingsDropChance(.17F);
            equipment.setBootsDropChance(.17F);
        }
    }

    private final String IMBUED = ChatColor.AQUA + "Imbued Crystal";
    private final String DARKNESS = ChatColor.DARK_RED + "Gem of Darkness";

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSacrifice(PlayerSacrificeItemEvent event) {

        Player player = event.getPlayer();

        ItemStack item = event.getItemStack();
        Location origin = event.getBlock().getLocation();

        boolean isInRewardsRoom = LocationUtil.isInRegion(getWorld(), rewards, origin);
        int c;
        int o = 1;
        int m = item.getType().getMaxDurability();
        ItemStack[] i;

        if (ItemUtil.isPhantomGold(item)) {
            int amount = 50;
            if (isInRewardsRoom) {
                amount = 100;
            }
            economy.depositPlayer(player.getName(), amount * item.getAmount());
            event.setItemStack(null);
        } else if (ItemUtil.isFearSword(item) || ItemUtil.isFearBow(item)) {

            if (!isInRewardsRoom) {
                o = 2;
            }

            c = ItemUtil.countItemsOfName(player.getInventory().getContents(), DARKNESS);
            i = ItemUtil.removeItemOfName(player.getInventory().getContents(), DARKNESS);
            player.getInventory().setContents(i);
            while (item.getDurability() > 0 && c >= o) {
                item.setDurability((short) Math.max(0, item.getDurability() - (m / 9)));
                c -= o;
            }
            player.getInventory().addItem(item);
            int amount = Math.min(c, 64);
            while (amount > 0) {
                player.getInventory().addItem(ItemUtil.Misc.gemOfDarkness(amount));
                c -= amount;
                amount = Math.min(c, 64);
            }
            player.updateInventory();
            event.setItemStack(null);
        } else if (ItemUtil.isUnleashedSword(item) || ItemUtil.isUnleashedBow(item)) {

            if (!isInRewardsRoom) {
                o = 2;
            }

            c = ItemUtil.countItemsOfName(player.getInventory().getContents(), IMBUED);
            i = ItemUtil.removeItemOfName(player.getInventory().getContents(), IMBUED);
            player.getInventory().setContents(i);
            while (item.getDurability() > 0 && c >= o) {
                item.setDurability((short) Math.max(0, item.getDurability() - (m / 9)));
                c -= o;
            }
            player.getInventory().addItem(item);
            int amount = Math.min(c, 64);
            while (amount > 0) {
                player.getInventory().addItem(ItemUtil.Misc.imbuedCrystal(amount));
                c -= amount;
                amount = Math.min(c, 64);
            }
            event.setItemStack(null);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();
        List<ItemStack> drops = event.getDrops();

        if (entity.getCustomName() != null) {
            String customName = entity.getCustomName();

            if (customName.equals("Grave Zombie")) {

                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();

                    if (stack != null && stack.getTypeId() == ItemID.ROTTEN_FLESH) it.remove();
                }

                if (ChanceUtil.getChance(15000)) {
                    drops.add(ItemUtil.Misc.phantomClock(ChanceUtil.getRandom(3)));
                }

                if (ChanceUtil.getChance(10000)) {
                    drops.add(ItemUtil.Misc.imbuedCrystal(1));
                }

                if (ChanceUtil.getChance(6000) || getWorld().isThundering() && ChanceUtil.getChance(4000)) {
                    drops.add(ItemUtil.Misc.batBow());
                }

                if (ChanceUtil.getChance(6000) || getWorld().isThundering() && ChanceUtil.getChance(4000)) {
                    drops.add(ItemUtil.Misc.gemOfDarkness(1));
                }

                if (ChanceUtil.getChance(6000) || getWorld().isThundering() && ChanceUtil.getChance(4000)) {
                    drops.add(ItemUtil.Misc.gemOfLife(1));
                }

                if (ChanceUtil.getChance(400)) {
                    drops.add(ItemUtil.Misc.phantomGold(ChanceUtil.getRandom(3)));
                }

                if (ChanceUtil.getChance(1000000)) {
                    switch (ChanceUtil.getRandom(4)) {
                        case 1:
                            drops.add(ItemUtil.Fear.makeSword());
                            break;
                        case 2:
                            drops.add(ItemUtil.Fear.makeBow());
                            break;
                        case 3:
                            drops.add(ItemUtil.Unleashed.makeSword());
                            break;
                        case 4:
                            drops.add(ItemUtil.Unleashed.makeBow());
                            break;
                    }
                }
            } else if (customName.equals("Guardian Zombie")) {

                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();

                    if (stack != null && stack.getTypeId() == ItemID.ROTTEN_FLESH) it.remove();
                }

                if (ChanceUtil.getChance(60)) {
                    drops.add(ItemUtil.CPotion.divineCombatPotion());
                } else if (ChanceUtil.getChance(40)) {
                    drops.add(ItemUtil.CPotion.holyCombatPotion());
                } else if (ChanceUtil.getChance(20)) {
                    drops.add(ItemUtil.CPotion.extremeCombatPotion());
                }

                if (ChanceUtil.getChance(250)) {
                    drops.add(ItemUtil.Misc.phantomClock(ChanceUtil.getRandom(3)));
                }

                if (ChanceUtil.getChance(100)) {
                    drops.add(ItemUtil.Misc.imbuedCrystal(1));
                }

                if (ChanceUtil.getChance(60) || getWorld().isThundering() && ChanceUtil.getChance(40)) {
                    drops.add(ItemUtil.Misc.batBow());
                }

                if (ChanceUtil.getChance(60) || getWorld().isThundering() && ChanceUtil.getChance(40)) {
                    drops.add(ItemUtil.Misc.gemOfDarkness(1));
                }

                if (ChanceUtil.getChance(60) || getWorld().isThundering() && ChanceUtil.getChance(40)) {
                    drops.add(ItemUtil.Misc.gemOfLife(1));
                }

                if (ChanceUtil.getChance(20)) {
                    drops.add(ItemUtil.Misc.phantomGold(1));
                }

                if (ChanceUtil.getChance(8000)) {
                    switch (ChanceUtil.getRandom(4)) {
                        case 1:
                            drops.add(ItemUtil.Fear.makeSword());
                            break;
                        case 2:
                            drops.add(ItemUtil.Fear.makeBow());
                            break;
                        case 3:
                            drops.add(ItemUtil.Unleashed.makeSword());
                            break;
                        case 4:
                            drops.add(ItemUtil.Unleashed.makeBow());
                            break;
                    }
                }
            }
        } else if (contains(entity)) {
            if (entity instanceof CaveSpider) {
                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();

                    if (stack != null && !ChanceUtil.getChance(15)) {
                        if (stack.getTypeId() == ItemID.STRING) it.remove();
                        if (stack.getTypeId() == ItemID.SPIDER_EYE) it.remove();
                    }
                }
            } else if (entity instanceof Creeper) {
                Iterator<ItemStack> it = drops.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();

                    if (stack != null && !ChanceUtil.getChance(15)) {
                        if (stack.getTypeId() == ItemID.SULPHUR) it.remove();
                    }
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

    @EventHandler(ignoreCancelled = true)
    public void onSheepEatGrass(EntityChangeBlockEvent event) {

        Entity entity = event.getEntity();
        if (entity instanceof Sheep && contains(entity)) {
            int type = event.getBlock().getTypeId();
            if (type == BlockID.GRASS || EnvironmentUtil.isShrubBlock(type)) {
                event.setCancelled(true);
                Location loc = entity.getLocation();
                entity.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 4, false, false);
            }
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

            generalIndex.addItem(new BlockRecord(block));

            block.setTypeId(0);
            RestorationUtil.handleToolDamage(event.getPlayer());
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
        if (isHostileTempleArea(contactedLoc)) {
            if (block.getTypeId() == BlockID.STONE_PRESSURE_PLATE
                    && (isPressurePlateLocked || contactedLoc.getBlockY() < 57)) {
                throwSlashPotion(contactedLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        final Player player = event.getPlayer();

        server.getScheduler().runTaskLater(inst, new Runnable() {

            @Override
            public void run() {

                if (isHostileTempleArea(player.getLocation()) && !adminComponent.isAdmin(player)) {
                    player.teleport(headStones.get(ChanceUtil.getRandom(headStones.size()) - 1));
                    ChatUtil.sendWarning(player, "You feel dazed and confused as you wake up near a head stone.");
                }
            }
        }, 1);
    }

    private static Set<TeleportCause> watchedCauses = new HashSet<>();

    static {
        watchedCauses.add(TeleportCause.ENDER_PEARL);
        watchedCauses.add(TeleportCause.COMMAND);
        watchedCauses.add(TeleportCause.PLUGIN);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {

        if (isHostileTempleArea(event.getFrom()) && event.getCause().equals(TeleportCause.NETHER_PORTAL)) {
            Location tg = headStones.get(ChanceUtil.getRandom(headStones.size()) - 1);
            tg = LocationUtil.findFreePosition(tg);
            if (tg == null) tg = getWorld().getSpawnLocation();
            event.setTo(tg);
            event.useTravelAgent(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();
        if (isHostileTempleArea(event.getTo()) && !adminComponent.isSysop(player)) {
            if (!watchedCauses.contains(event.getCause())) return;
            if (contains(event.getFrom())) {
                event.setCancelled(true);
            } else {
                Location tg = headStones.get(ChanceUtil.getRandom(headStones.size()) - 1);
                tg = LocationUtil.findFreePosition(tg);
                if (tg == null) tg = getWorld().getSpawnLocation();
                event.setTo(tg);
            }

            ChatUtil.sendWarning(event.getPlayer(), "It would seem your teleport has failed to penetrate the temple.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        final Player player = event.getPlayer();

        Block block = event.getClickedBlock();
        Location clickedLoc = block.getLocation();
        ItemStack stack = player.getItemInHand();
        Action action = event.getAction();

        if (isHostileTempleArea(clickedLoc)) {
            switch (block.getTypeId()) {
                case BlockID.LEVER:
                    server.getScheduler().runTaskLater(inst, new Runnable() {

                        @Override
                        public void run() {

                            isPressurePlateLocked = !checkPressurePlateLock();
                        }
                    }, 1);
                    break;
                case BlockID.STONE_PRESSURE_PLATE:
                    if ((isPressurePlateLocked || clickedLoc.getBlockY() < 57) && action.equals(Action.PHYSICAL)) {
                        throwSlashPotion(clickedLoc);
                    }
                    break;
            }
        }

        switch (action) {
            case RIGHT_CLICK_BLOCK:
                if (ItemUtil.matchesFilter(stack, ChatColor.DARK_RED + "Phantom Clock")) {

                    player.teleport(new Location(getWorld(), -126, 42, -685), TeleportCause.UNKNOWN);

                    final int amt = stack.getAmount() - 1;
                    server.getScheduler().runTaskLater(inst, new Runnable() {
                        @Override
                        public void run() {

                            ItemStack newStack = null;
                            if (amt > 0) {
                                newStack = ItemUtil.Misc.phantomClock(amt);
                            }
                            player.setItemInHand(newStack);
                        }
                    }, 1);
                }
                break;
        }
    }

    private static final PotionType[] thrownTypes = new PotionType[]{
            PotionType.INSTANT_DAMAGE, PotionType.INSTANT_DAMAGE,
            PotionType.POISON, PotionType.WEAKNESS
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

    private static final String GEM_OF_LIFE = ChatColor.DARK_AQUA + "Gem of Life";

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        boolean contained = contains(player);

        if (contained || player.getWorld().isThundering()) {

            List<ItemStack> drops = event.getDrops();
            ItemStack[] dropArray = ItemUtil.clone(drops.toArray(new ItemStack[drops.size()]));
            if (ItemUtil.findItemOfName(dropArray, GEM_OF_LIFE)) {
                if (!playerState.containsKey(player.getName())) {
                    GemOfLifeUsageEvent aEvent = new GemOfLifeUsageEvent(player);
                    server.getPluginManager().callEvent(aEvent);
                    if (!aEvent.isCancelled()) {
                        playerState.put(player.getName(), new PlayerState(player.getName(),
                                player.getInventory().getContents(),
                                player.getInventory().getArmorContents(),
                                player.getLevel(),
                                player.getExp()));
                        if (contained) {
                            dropArray = null;
                        } else {
                            drops.clear();
                            return;
                        }
                    }
                }
            }

            // Leave admin mode deaths out of this
            if (!contained || adminComponent.isAdmin(player)) return;

            makeGrave(player.getName(), dropArray);
            drops.clear();

            event.setDeathMessage(ChatColor.DARK_RED + "RIP ~ " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !adminComponent.isAdmin(player)) {

            try {
                PlayerState identity = playerState.get(player.getName());

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());

                // Count then remove the Gems of Life
                int c = ItemUtil.countItemsOfName(player.getInventory().getContents(), GEM_OF_LIFE) - 1;
                ItemStack[] newInv = ItemUtil.removeItemOfName(player.getInventory().getContents(), GEM_OF_LIFE);
                player.getInventory().setContents(newInv);

                // Add back the gems of life as needed
                int amount = Math.min(c, 64);
                while (amount > 0) {
                    player.getInventory().addItem(ItemUtil.Misc.gemOfLife(amount));
                    c -= amount;
                    amount = Math.min(c, 64);
                }
                //noinspection deprecation
                player.updateInventory();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                playerState.remove(player.getName());
            }
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

            if (itemStacks == null) return;

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
                } else {
                    org.bukkit.material.Sign sign =
                            new org.bukkit.material.Sign(BlockID.WALL_SIGN, signState.getRawData());
                    BlockFace attachedFace = sign.getAttachedFace();

                    headStone = headStone.getBlock().getRelative(attachedFace, 2).getLocation();
                    headStone.add(0, 2, 0);
                    chestState = headStone.getBlock().getState();

                    if (chestState instanceof Chest) {
                        ((Chest) chestState).getInventory().clear();
                        ((Chest) chestState).getInventory().addItem(itemStacks);
                    } else {
                        headStone.add(0, -1, 0);
                        chestState = headStone.getBlock().getState();

                        if (chestState instanceof Chest) {
                            ((Chest) chestState).getInventory().clear();
                            ((Chest) chestState).getInventory().addItem(itemStacks);
                        } else {
                            for (ItemStack stack : itemStacks) {
                                getWorld().dropItem(signState.getLocation(), stack);
                            }
                        }
                    }
                }
            }
        }
    }

    private void findHeadStones() {

        headStones.clear();

        final List<Chunk> chunkList = new ArrayList<>();

        com.sk89q.worldedit.Vector min = getRegion().getMinimumPoint();
        com.sk89q.worldedit.Vector max = getRegion().getMaximumPoint();

        final int minX = min.getBlockX();
        final int minZ = min.getBlockZ();
        final int minY = min.getBlockY();
        final int maxX = max.getBlockX();
        final int maxZ = max.getBlockZ();

        Chunk c;
        for (int x = minX; x <= maxX; x += 16) {
            for (int z = minZ; z <= maxZ; z += 16) {
                c = getWorld().getBlockAt(x, minY, z).getChunk();
                if (!chunkList.contains(c)) chunkList.add(c);
            }
        }

        for (final Chunk aChunk : chunkList) {

            try {
                server.getScheduler().runTaskLater(inst, new Runnable() {
                    @Override
                    public void run() {

                        for (BlockState aSign : aChunk.getTileEntities()) {
                            if (!(aSign instanceof Sign)) continue;
                            checkHeadStone((Sign) aSign);
                        }
                    }
                }, chunkList.indexOf(aChunk) * 20);
            } catch (NullPointerException ex) {
                findHeadStones();
                log.info("Failed to get head stones for Chunk: " + aChunk.getX() + ", " + aChunk.getZ() + ".");
                return;
            }
        }
    }

    private boolean checkHeadStone(Sign sign) {

        Location l = sign.getLocation();
        if ((l.getBlockY() != 81 && l.getBlockY() != 82) || !contains(l)) return false;
        headStones.add(sign.getLocation());
        return true;
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

        ChatUtil.sendNotice(getTempleContained(), "You hear a clicking sound.");
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

    private void findRewardChest() {

        com.sk89q.worldedit.Vector min = rewards.getMinimumPoint();
        com.sk89q.worldedit.Vector max = rewards.getMaximumPoint();

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
                    if (block.getTypeId() == BlockID.CHEST) {
                        rewardChest.add(block.getLocation());
                    }
                }
            }
        }
    }

    private void resetRewardChest() {

        BlockState block;
        Chest chest;
        for (Location location : rewardChest) {
            block = location.getBlock().getState();
            if (!block.getChunk().isLoaded()) block.getChunk().load();
            chest = (Chest) block;
            chest.getBlockInventory().clear();

            int length = chest.getBlockInventory().getContents().length;
            int target;
            for (int i = 0; i < length / 3; i++) {
                target = ChanceUtil.getRandom(length) - 1;
                if (chest.getBlockInventory().getContents()[target] != null) {
                    i--;
                    continue;
                }
                chest.getBlockInventory().setItem(target, pickRandomItem());
            }
            chest.update();
        }
    }

    private ItemStack pickRandomItem() {

        switch (ChanceUtil.getRandom(39)) {
            case 1:
                return ItemUtil.Misc.gemOfLife(6);
            case 3:
                if (!ChanceUtil.getChance(17)) return null;
                return ItemUtil.Fear.makeSword();
            case 4:
                if (!ChanceUtil.getChance(17)) return null;
                return ItemUtil.Fear.makeBow();
            case 5:
                if (!ChanceUtil.getChance(25)) return null;
                return ItemUtil.Unleashed.makeSword();
            case 6:
                if (!ChanceUtil.getChance(25)) return null;
                return ItemUtil.Unleashed.makeBow();
            case 7:
                return ItemUtil.Misc.imbuedCrystal(ChanceUtil.getRandom(3));
            case 8:
                return ItemUtil.Misc.gemOfDarkness(ChanceUtil.getRandom(3));
            case 9:
                return ItemUtil.Misc.batBow();
            case 10:
                return ItemUtil.Misc.phantomGold(ChanceUtil.getRandom(64));
            case 11:
                return ItemUtil.Ancient.makeHelmet();
            case 12:
                return ItemUtil.Ancient.makeChest();
            case 13:
                return ItemUtil.Ancient.makeLegs();
            case 14:
                return ItemUtil.Ancient.makeBoots();
            case 15:
                return ItemUtil.God.makeHelmet();
            case 16:
                return ItemUtil.God.makeChest();
            case 17:
                return ItemUtil.God.makeLegs();
            case 18:
                return ItemUtil.God.makeBoots();
            case 19:
                return ItemUtil.God.makePickaxe(false);
            case 20:
                return ItemUtil.God.makePickaxe(true);
            case 21:
                return new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRandom(64));
            case 22:
                return new ItemStack(ItemID.DIAMOND, ChanceUtil.getRandom(64));
            case 23:
                return new ItemStack(ItemID.EMERALD, ChanceUtil.getRandom(64));
            case 24:
                return new ItemStack(ItemID.REDSTONE_DUST, ChanceUtil.getRandom(64));
            case 25:
                return new ItemStack(ItemID.ENDER_PEARL, ChanceUtil.getRandom(16));
            case 26:
                return new ItemStack(ItemID.GOLD_APPLE, ChanceUtil.getRandom(64), (short) 1);
            case 28:
                return new ItemStack(ItemID.SADDLE);
            case 29:
                return new ItemStack(ItemID.HORSE_ARMOR_IRON);
            case 30:
                return new ItemStack(ItemID.HORSE_ARMOR_GOLD);
            case 31:
                return new ItemStack(ItemID.HORSE_ARMOR_DIAMOND);
            default:
                return ItemUtil.Misc.barbarianBone(ChanceUtil.getRandom(5));
        }
    }

    private void breakBlock(Entity e, Location location) {

        int chance = e instanceof Player ? 2 : 6;

        Block block = location.getBlock();
        for (BlockFace face : EnvironmentUtil.getNearbyBlockFaces()) {
            final Block aBlock = block.getRelative(face);
            Block bBlock = aBlock.getRelative(BlockFace.DOWN);
            if (!BlockType.canPassThrough(bBlock.getTypeId())) continue;
            BaseBlock aBB = new BaseBlock(aBlock.getTypeId(), aBlock.getData());
            if (ChanceUtil.getChance(chance) && accept(aBB, autoBreakable)) {

                final BlockRecord record = new BlockRecord(aBlock.getLocation(location), aBB);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        generalIndex.addItem(record);
                        aBlock.setTypeId(0);
                    }
                };

                if (aBB.equals(new BaseBlock(BlockID.STONE_BRICK, 2))) {
                    server.getScheduler().runTaskLater(inst, r, 17);
                } else {
                    r.run();
                }
            }
        }
    }

    private void fogPlayer(Player player) {

        if (ItemUtil.hasFearHelmet(player)) return;
        ItemStack[] inventoryContents = player.getInventory().getContents();
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        if (ItemUtil.findItemOfName(inventoryContents, DARKNESS) || ItemUtil.findItemOfName(armorContents, ChatColor.GOLD + "Ancient Crown"))
            return;
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 6, 1));
    }

    @Override
    public void forceRestoreBlocks() {

        resetPressurePlateLock();
        generalIndex.revertAll();
    }

    @Override
    public void writeData(boolean doAsync) {

        Runnable run = new Runnable() {
            @Override
            public void run() {

                IOUtil.toBinaryFile(getWorkingDir(), "general", generalIndex);
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

        File generalFile = new File(getWorkingDir().getPath() + "/general.dat");
        File playerStateFile = new File(getWorkingDir().getPath() + "/respawns.dat");

        if (generalFile.exists()) {
            Object generalFileO = IOUtil.readBinaryFile(generalFile);

            if (generalFileO instanceof BaseBlockRecordIndex) {
                generalIndex = (BaseBlockRecordIndex) generalFileO;
                log.info("Loaded: " + generalIndex.size() + " general records for: " + getId() + ".");
            } else {
                log.warning("Invalid block record file encountered: " + generalFile.getName() + "!");
                log.warning("Attempting to use backup file...");

                generalFile = new File(getWorkingDir().getPath() + "/old-" + generalFile.getName());

                if (generalFile.exists()) {

                    generalFileO = IOUtil.readBinaryFile(generalFile);

                    if (generalFileO instanceof BaseBlockRecordIndex) {
                        generalIndex = (BaseBlockRecordIndex) generalFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + generalIndex.size() + " general records for: " + getId() + ".");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }

        if (playerStateFile.exists()) {
            Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);

            if (playerStateFileO instanceof HashMap) {
                //noinspection unchecked
                playerState = (HashMap<String, PlayerState>) playerStateFileO;
                log.info("Loaded: " + playerState.size() + " respawn records for: " + getId() + ".");
            } else {
                log.warning("Invalid block record file encountered: " + playerStateFile.getName() + "!");
                log.warning("Attempting to use backup file...");

                playerStateFile = new File(getWorkingDir().getPath() + "/old-" + playerStateFile.getName());

                if (playerStateFile.exists()) {

                    playerStateFileO = IOUtil.readBinaryFile(playerStateFile);

                    if (playerStateFileO instanceof BaseBlockRecordIndex) {
                        //noinspection unchecked
                        playerState = (HashMap<String, PlayerState>) playerStateFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + playerState.size() + " respawn records for: " + getId() + ".");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }
    }

    public void restoreBlocks() {

        generalIndex.revertByTime(1000 * 27);

        writeData(true);
    }

    @Override
    public void run() {

        restoreBlocks();

        if (isEmpty()) return;

        equalize();

        for (Entity entity : getContainedEntities(LivingEntity.class)) {

            if (!entity.isValid()) continue;

            // Cave Spider killer
            if (entity instanceof CaveSpider && entity.getLocation().getBlock().getLightFromSky() >= 10) {
                for (int i = 0; i < 20; i++) getWorld().playEffect(entity.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
                entity.remove();
                continue;
            }

            // Auto break stuff
            Location belowLoc = entity.getLocation();
            if (!(entity instanceof Player) || isInEvilRegion(belowLoc)) {
                breakBlock(entity, belowLoc);
                breakBlock(entity, belowLoc.add(0, -1, 0));
                breakBlock(entity, belowLoc.add(0, -1, 0));
            }

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
        //noinspection SimplifiableIfStatement
        if (EnvironmentUtil.isNightTime(getWorld().getTime()) || getWorld().hasStorm()) return true;

        return isHostileTempleArea(block.getLocation()) || block.getLightLevel() == 0;
    }

    private boolean isHostileTempleArea(Location location) {

        return isInEvilRegion(location) && location.getY() < 93 && location.getBlock().getLightFromSky() < 4;
    }

    private boolean isInEvilRegion(Location location) {

        for (ProtectedRegion region : new ProtectedRegion[]{temple}) {
            if (LocationUtil.isInRegion(getWorld(), region, location)) return true;
        }
        return location.getY() < 69 && contains(location);
    }

    @Override
    public void disable() {

        writeData(false);
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

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
}