package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.blocks.SkullBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.exceptions.UnsupportedPrayerException;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.item.BookUtil;
import com.skelril.aurora.util.item.EffectUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.restoration.BlockRecord;
import com.skelril.aurora.util.restoration.PlayerMappedBlockRecordIndex;
import com.skelril.aurora.util.restoration.RestorationUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class CursedMine extends AbstractRegionedArena implements MonitoredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private ProtectedRegion floodGate;

    private AdminComponent adminComponent;
    private PrayerComponent prayerComponent;
    private RestorationUtil restorationUtil;

    private WorldGuardPlugin worldGuard;


    //private ConcurrentHashMap<Player, ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long,
    //        BaseBlock>>> map = new ConcurrentHashMap<>();
    private long lastActivation = 0;
    private PlayerMappedBlockRecordIndex recordSystem = new PlayerMappedBlockRecordIndex();
    private List<Player> daveHitList = new ArrayList<>();

    private final int[] items = new int[]{
            BlockID.IRON_BLOCK, BlockID.IRON_ORE, ItemID.IRON_BAR,
            BlockID.GOLD_BLOCK, BlockID.GOLD_ORE, ItemID.GOLD_BAR, ItemID.GOLD_NUGGET,
            BlockID.REDSTONE_ORE, BlockID.GLOWING_REDSTONE_ORE, ItemID.REDSTONE_DUST,
            BlockID.LAPIS_LAZULI_BLOCK, BlockID.LAPIS_LAZULI_ORE, ItemID.INK_SACK,
            BlockID.DIAMOND_BLOCK, BlockID.DIAMOND_ORE, ItemID.DIAMOND,
            BlockID.EMERALD_BLOCK, BlockID.EMERALD_ORE, ItemID.EMERALD
    };

    public CursedMine(World world, ProtectedRegion[] regions, AdminComponent adminComponent,
                      PrayerComponent prayerComponent, RestorationUtil restorationUtil) {

        super(world, regions[0]);

        this.floodGate = regions[1];

        this.adminComponent = adminComponent;
        this.prayerComponent = prayerComponent;
        this.restorationUtil = restorationUtil;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        setUpWorldGuard();
    }

    @Override
    public void forceRestoreBlocks() {

        recordSystem.revertAll();
    }

    @Override
    public void run() {

        drain();
        sweepFloor();
        randomRestore();

        if (!isEmpty()) {
            equalize();
            changeWater();
        }
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
                adminComponent.deadmin(player);
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }

    private void setUpWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return; // Maybe you want throw an exception instead
        }

        this.worldGuard = (WorldGuardPlugin) plugin;
    }

    public void addSkull(Player player) {

        RegionManager manager = worldGuard.getRegionManager(getWorld());
        ProtectedRegion r = null;
        byte b = 0;
        switch (ChanceUtil.getRandom(3)) {
            case 1:
                r = manager.getRegion(getId() + "-deaths-east");
                b = 12;
                break;
            case 2:
                r = manager.getRegion(getId() + "-deaths-north");
                b = 8;
                break;
            case 3:
                r = manager.getRegion(getId() + "-deaths-west");
                b = 4;
                break;
        }

        if (r != null) {
            Vector v = LocationUtil.pickLocation(r.getMinimumPoint(), r.getMaximumPoint())
                    .add(0, r.getMinimumPoint().getY(), 0);
            BukkitWorld world = new BukkitWorld(getWorld());
            EditSession skullEditor = new EditSession(world, 1);
            skullEditor.rawSetBlock(v, new SkullBlock(3, b, player.getName()));
        }
    }

    public void randomRestore() {

        recordSystem.revertByTime(ChanceUtil.getRangedRandom(9000, 60000));
    }

    public void revertPlayer(Player player) {

        recordSystem.revertByPlayer(player.getName());
    }

    public void sweepFloor() {

        for (Item item : getWorld().getEntitiesByClass(Item.class)) {

            if (!contains(item)) continue;

            int id = item.getItemStack().getTypeId();
            for (int aItem : items) {
                if (aItem == id) {
                    double newAmt = item.getItemStack().getAmount() * .8;
                    if (newAmt < 1) {
                        item.remove();
                    } else {
                        item.getItemStack().setAmount((int) newAmt);
                    }
                    break;
                }
            }
        }
    }

    public void drain() {

        for (Entity e : getContainedEntities()) {
            if (!(e instanceof InventoryHolder)) continue;
            try {
                Inventory eInventory = ((InventoryHolder) e).getInventory();

                for (int i = 0; i < (ItemUtil.countFilledSlots(eInventory.getContents()) / 2) - 2 || i < 1; i++) {

                    if (e instanceof Player) {
                        if (ChanceUtil.getChance(15) && checkInventory((Player) e, eInventory.getContents())) {
                            ChatUtil.sendNotice((Player) e, "Divine intervention protects some of your items.");
                            continue;
                        }
                    }

                    // Iron
                    ItemUtil.removeItemOfType((InventoryHolder) e, BlockID.IRON_BLOCK, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, BlockID.IRON_ORE, ChanceUtil.getRandom(4), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, ItemID.IRON_BAR, ChanceUtil.getRandom(8), true);

                    // Gold
                    ItemUtil.removeItemOfName((InventoryHolder) e, ItemUtil.Misc.cursedGold(1, true), ChanceUtil.getRandom(4), true);
                    ItemUtil.removeItemOfName((InventoryHolder) e, ItemUtil.Misc.cursedGold(1, false), ChanceUtil.getRandom(10), true);

                    // Redstone
                    ItemUtil.removeItemOfType((InventoryHolder) e, BlockID.REDSTONE_ORE, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, BlockID.GLOWING_REDSTONE_ORE, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, ItemID.REDSTONE_DUST, ChanceUtil.getRandom(34), true);

                    // Lap
                    ItemUtil.removeItemOfType((InventoryHolder) e, BlockID.LAPIS_LAZULI_BLOCK, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, BlockID.LAPIS_LAZULI_ORE, ChanceUtil.getRandom(4), true);
                    eInventory.removeItem(new ItemStack(ItemID.INK_SACK, ChanceUtil.getRandom(34), (short) 4));

                    // Diamond
                    ItemUtil.removeItemOfType((InventoryHolder) e, BlockID.DIAMOND_BLOCK, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, BlockID.DIAMOND_ORE, ChanceUtil.getRandom(4), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, ItemID.DIAMOND, ChanceUtil.getRandom(16), true);

                    // Emerald
                    //pInventory.removeItem(new ItemStack(BlockID.EMERALD_BLOCK, ChanceUtil.getRandom(2)));
                    //pInventory.removeItem(new ItemStack(BlockID.EMERALD_ORE, ChanceUtil.getRandom(4)));
                    //pInventory.removeItem(new ItemStack(ItemID.EMERALD, ChanceUtil.getRandom(12)));
                }
            } catch (Exception ex) {
                log.warning("=== Cursed Mine Drain System Error ===");
                if (e instanceof Player) {
                    log.warning("The player: " + ((Player) e).getName() + " has avoided the drain.");
                } else {
                    log.warning("An entity has avoided the drain.");
                }
            }
        }
    }

    private boolean checkInventory(Player player, ItemStack[] itemStacks) {

        if (!inst.hasPermission(player, "aurora.prayer.intervention")) return false;

        for (int aItem : items) {
            if (player.getInventory().containsAtLeast(new ItemStack(aItem), 1)) return true;
        }
        return false;
    }

    private boolean hasSilkTouch(ItemStack item) {

        return item.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    private boolean hasFortune(ItemStack item) {

        return item.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS);
    }

    private static Set<Integer> replaceableTypes = new HashSet<>();

    static {
        replaceableTypes.add(BlockID.WATER);
        replaceableTypes.add(BlockID.STATIONARY_WATER);
        replaceableTypes.add(BlockID.WOOD);
        replaceableTypes.add(BlockID.AIR);
    }

    private void changeWater() {

        int id = 0;
        if (lastActivation == 0 || System.currentTimeMillis() - lastActivation >= 18000) {
            id = BlockID.WOOD;
        }
        com.sk89q.worldedit.Vector min = floodGate.getMinimumPoint();
        com.sk89q.worldedit.Vector max = floodGate.getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int blockY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = getWorld().getBlockAt(x, blockY, z);

                if (!block.getChunk().isLoaded()) continue;

                if (replaceableTypes.contains(block.getTypeId())) {
                    block.setTypeId(id);
                }
            }
        }
    }

    private void eatFood(Player player) {

        if (player.getSaturation() - 1 >= 0) {
            player.setSaturation(player.getSaturation() - 1);
        } else if (player.getFoodLevel() - 1 >= 0) {
            player.setFoodLevel(player.getFoodLevel() - 1);
        } else if (player.getHealth() - 1 >= 0) {
            player.setHealth(player.getHealth() - 1);
        }
    }

    private void poison(Player player, int duration) {

        if (ChanceUtil.getChance(player.getLocation().getBlockY() / 2)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * duration, 2));
            ChatUtil.sendWarning(player, "The ore releases a toxic gas poisoning you!");
        }
    }

    private void ghost(final Player player, int blockid) {

        try {
            if (ChanceUtil.getChance(player.getLocation().getBlockY())) {
                if (ChanceUtil.getChance(2)) {
                    switch (ChanceUtil.getRandom(4)) {
                        case 1:
                            ChatUtil.sendNotice(player, "Caspher the friendly ghost drops some bread.");
                            player.getWorld().dropItemNaturally(player.getLocation(),
                                    new ItemStack(ItemID.BREAD, ChanceUtil.getRandom(16)));
                            break;
                        case 2:
                            ChatUtil.sendNotice(player, "COOKIE gives you a cookie.");
                            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(ItemID.COOKIE));
                            break;
                        case 3:
                            ChatUtil.sendNotice(player, "Caspher the friendly ghost appears.");
                            for (int i = 0; i < 8; i++) {
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        new ItemStack(ItemID.IRON_BAR, ChanceUtil.getRandom(64)));
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        ItemUtil.Misc.cursedGold(ChanceUtil.getRandom(64), false));
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        new ItemStack(ItemID.DIAMOND, ChanceUtil.getRandom(64)));
                            }
                            break;
                        case 4:
                            ChatUtil.sendNotice(player, "John gives you a new jacket.");
                            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(ItemID.LEATHER_CHEST));
                            break;
                        default:
                            break;
                    }
                } else {

                    if (ItemUtil.hasAncientArmour(player) && ChanceUtil.getChance(2)) {
                        ChatUtil.sendNotice(player, ChatColor.AQUA, "Your armour blocks an incoming ghost attack.");
                        return;
                    }

                    Location modifiedLoc = null;

                    switch (ChanceUtil.getRandom(10)) {
                        case 1:
                            if (ChanceUtil.getChance(4)) {
                                if (blockid == BlockID.DIAMOND_ORE) {
                                    ChatUtil.sendWarning(player, "You ignite fumes in the air!");
                                    EditSession ess = new EditSession(new BukkitWorld(player.getWorld()), -1);
                                    try {
                                        ess.fillXZ(BukkitUtil.toVector(player.getLocation()), new BaseBlock(BlockID.FIRE), 20, 20, true);
                                    } catch (MaxChangedBlocksException ignored) {

                                    }
                                    for (int i = 1; i < ChanceUtil.getRandom(24) + 20; i++) {
                                        final boolean untele = i == 11;
                                        server.getScheduler().runTaskLater(inst, new Runnable() {
                                            @Override
                                            public void run() {

                                                if (untele) {
                                                    recordSystem.revertByPlayer(player.getName());
                                                }

                                                if (!contains(player)) return;

                                                Location l = LocationUtil.findRandomLoc(player.getLocation().getBlock(), 3, true, false);
                                                l.getWorld().createExplosion(l.getX(), l.getY(), l.getZ(), 3F, true, false);
                                            }
                                        }, 12 * i);
                                    }
                                } else {
                                    player.chat("Who's a good ghost?!?!");
                                    daveHitList.add(player);
                                    server.getScheduler().runTaskLater(inst, new Runnable() {
                                        @Override
                                        public void run() {
                                            player.chat("Don't hurt me!!!");
                                            server.getScheduler().runTaskLater(inst, new Runnable() {
                                                @Override
                                                public void run() {
                                                    player.chat("Nooooooooooo!!!");

                                                    try {
                                                        prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                                                PrayerType.CANNON, TimeUnit.MINUTES.toMillis(2)));
                                                    } catch (UnsupportedPrayerException ex) {
                                                        ex.printStackTrace();
                                                    }
                                                }
                                            }, 20);
                                        }
                                    }, 20);
                                }
                                break;
                            }
                        case 2:
                            ChatUtil.sendWarning(player, "You find yourself falling from the sky...");
                            daveHitList.add(player);
                            modifiedLoc = new Location(player.getWorld(), player.getLocation().getX(), 350, player.getLocation().getZ());
                            break;
                        case 3:
                            ChatUtil.sendWarning(player, "George plays with fire, sadly too close to you.");
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.FIRE, TimeUnit.SECONDS.toMillis(45)));
                            break;
                        case 4:
                            ChatUtil.sendWarning(player, "Simon says pick up sticks.");
                            for (int i = 0; i < player.getInventory().getContents().length * 1.5; i++) {
                                player.getWorld().dropItem(player.getLocation(), new ItemStack(ItemID.STICK, 64));
                            }
                            break;
                        case 5:
                            ChatUtil.sendWarning(player, "Ben dumps out your backpack.");
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.BUTTERFINGERS, TimeUnit.SECONDS.toMillis(10)));
                            break;
                        case 6:
                            ChatUtil.sendWarning(player, "Merlin attacks with a mighty rage!");
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.MERLIN, TimeUnit.SECONDS.toMillis(20)));
                            break;
                        case 7:
                            ChatUtil.sendWarning(player, "Dave tells everyone that your mining!");
                            Bukkit.broadcastMessage(ChatColor.GOLD + "The player: "
                                    + player.getDisplayName() + " is mining in the cursed mine!!!");
                            break;
                        case 8:
                            ChatUtil.sendWarning(player, "Dave likes your food.");
                            daveHitList.add(player);
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.STARVATION, TimeUnit.MINUTES.toMillis(15)));
                            break;
                        case 9:
                            ChatUtil.sendWarning(player, "Hallow declares war on YOU!");
                            for (int i = 0; i < ChanceUtil.getRangedRandom(10, 30); i++) {
                                Blaze blaze = getWorld().spawn(player.getLocation(), Blaze.class);
                                blaze.setTarget(player);
                                blaze.setRemoveWhenFarAway(false);
                            }
                            break;
                        case 10:
                            ChatUtil.sendWarning(player, "Dave says hi, that's not good.");
                            daveHitList.add(player);
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.SLAP, TimeUnit.MINUTES.toMillis(30)));
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.BUTTERFINGERS, TimeUnit.MINUTES.toMillis(30)));
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.FIRE, TimeUnit.MINUTES.toMillis(30)));
                            break;
                        default:
                            break;
                    }

                    if (modifiedLoc != null) player.teleport(modifiedLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                }
            }
        } catch (UnsupportedPrayerException ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (!contains(player)) return;

        InventoryType.SlotType st = event.getSlotType();
        if (st.equals(InventoryType.SlotType.CRAFTING)) {
            event.setResult(Event.Result.DENY);
            ChatUtil.sendWarning(player, "You cannot use that here.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (!contains(player)) return;

        event.setResult(Event.Result.DENY);
        ChatUtil.sendWarning(player, "You cannot do that here.");
    }

    private static Set<Integer> triggerBlocks = new HashSet<>();
    private static Set<Action> triggerInteractions = new HashSet<>();

    static {
        triggerBlocks.add(BlockID.STONE_BUTTON);
        triggerBlocks.add(BlockID.TRIPWIRE);

        triggerInteractions.add(Action.PHYSICAL);
        triggerInteractions.add(Action.RIGHT_CLICK_BLOCK);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        final Block block = event.getClickedBlock();

        if (contains(block) && triggerBlocks.contains(block.getTypeId()) && triggerInteractions.contains(event.getAction())) {
            lastActivation = System.currentTimeMillis();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final int typeId = block.getTypeId();

        ItemStack itemInHand = player.getItemInHand();

        if (!adminComponent.isAdmin(player)
                && contains(block)
                && EnvironmentUtil.isValuableOre(block)
                && itemInHand != null
                && ItemUtil.isPickAxe(itemInHand.getTypeId())) {

            if (ChanceUtil.getChance(4)) {
                ItemStack rawDrop = EnvironmentUtil.getOreDrop(block.getTypeId(), hasSilkTouch(itemInHand));

                if (inst.hasPermission(player, "aurora.mining.burningember") && !hasSilkTouch(itemInHand)) {
                    switch (typeId) {
                        case BlockID.GOLD_ORE:
                            rawDrop = ItemUtil.Misc.cursedGold(1, false);
                            break;
                        case BlockID.IRON_ORE:
                            rawDrop.setTypeId(ItemID.IRON_BAR);
                            break;
                    }
                }

                if (rawDrop.getTypeId() == BlockID.GOLD_ORE) {
                    rawDrop = ItemUtil.Misc.cursedGold(1, true);
                }

                if (hasFortune(itemInHand) && !EnvironmentUtil.isOre(rawDrop.getTypeId())) {
                    rawDrop.setAmount(rawDrop.getAmount() * ChanceUtil.getRangedRandom(4, 8) * ItemUtil.fortuneMultiplier(itemInHand));
                } else {
                    rawDrop.setAmount(rawDrop.getAmount() * ChanceUtil.getRangedRandom(4, 16));
                }

                player.getInventory().addItem(rawDrop);
            }
            event.setExpToDrop((70 - player.getLocation().getBlockY()) / 2);

            if (ChanceUtil.getChance(3000)) {
                ChatUtil.sendNotice(player, "You feel as though a spirit is trying to tell you something...");
                player.getInventory().addItem(BookUtil.Lore.Areas.theGreatMine());
            }

            eatFood(player);
            poison(player, 6);
            ghost(player, typeId);

            recordSystem.addItem(player.getName(), new BlockRecord(block));
            restorationUtil.blockAndLogEvent(event);
        } else if (!adminComponent.isAdmin(player) && contains(block)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot break this block for some reason.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (!adminComponent.isAdmin(player) && contains(event.getBlock())) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, ChatColor.DARK_RED, "You don't have permission for this area.");
        }
    }

    private static List<PlayerTeleportEvent.TeleportCause> accepted = new ArrayList<>();

    static {
        accepted.add(PlayerTeleportEvent.TeleportCause.UNKNOWN);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();

        if ((recordSystem.hasRecordForPlayer(player.getName()) || daveHitList.contains(player)) && !accepted.contains(event.getCause())) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You have been tele-blocked!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {

        if (contains(event.getBlockClicked())) {

            final Block block = event.getBlockClicked();

            recordSystem.addItem(event.getPlayer().getName(), new BlockRecord(block));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {

        Player player = event.getPlayer();
        if (!adminComponent.isAdmin(player) && contains(event.getBlockClicked())) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, ChatColor.DARK_RED, "You don't have permission for this area.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        Player player = event.getPlayer();

        if (event.getCause().getEffect().getType().isHoly() && contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        if (event.getEntity() instanceof Player) {

            Player player = (Player) event.getEntity();

            if (!contains(player)) return;
            if (ChanceUtil.getChance(7) && ItemUtil.hasMasterSword(player) && ItemUtil.hasAncientArmour(player)) {
                EffectUtil.Master.ultimateStrength(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        revertPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity().getPlayer();
        revertPlayer(player);

        if (daveHitList.contains(player) || contains(player)) {

            if (contains(player) && ChanceUtil.getChance(500)) {
                ChatUtil.sendNotice(player, "You feel as though a spirit is trying to tell you something...");
                event.getDrops().add(BookUtil.Lore.Areas.theGreatMine());
            }

            if (daveHitList.contains(player)) daveHitList.remove(player);
            switch (ChanceUtil.getRandom(11)) {
                case 1:
                    event.setDeathMessage(player.getName() + " was killed by Dave");
                    break;
                case 2:
                    event.setDeathMessage(player.getName() + " got on Dave's bad side");
                    break;
                case 3:
                    event.setDeathMessage(player.getName() + " was slain by an evil spirit");
                    break;
                case 4:
                    event.setDeathMessage(player.getName() + " needs to stay away from the cursed mine");
                    break;
                case 5:
                    event.setDeathMessage(player.getName() + " enjoys death a little too much");
                    break;
                case 6:
                    event.setDeathMessage(player.getName() + " seriously needs to stop mining");
                    break;
                case 7:
                    event.setDeathMessage(player.getName() + " angered an evil spirit");
                    break;
                case 8:
                    event.setDeathMessage(player.getName() + " doesn't get a cookie from COOKIE");
                    break;
                case 9:
                    event.setDeathMessage(player.getName() + " should stay away");
                    break;
                case 10:
                    event.setDeathMessage(player.getName() + " needs to consider retirement");
                    break;
                case 11:
                    event.setDeathMessage(player.getName() + "'s head is now on Dave's mantel");
                    break;
            }
            addSkull(player);
        }
    }
}
