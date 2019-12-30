/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.SkullBlock;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.PrayerApplicationEvent;
import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import gg.packetloss.grindstone.exceptions.UnsupportedPrayerException;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.highscore.ScoreTypes;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.PrayerFX.InventoryFX;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.state.block.BlockStateComponent;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.extractor.entity.CombatantPair;
import gg.packetloss.grindstone.util.extractor.entity.EDBEExtractor;
import gg.packetloss.grindstone.util.item.BookUtil;
import gg.packetloss.grindstone.util.item.EffectUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.restoration.RestorationUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CursedMine extends AbstractRegionedArena implements MonitoredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private ProtectedRegion floodGate;

    private AdminComponent adminComponent;
    private PrayerComponent prayerComponent;
    private HighScoresComponent highScoresComponent;
    private BlockStateComponent blockStateComponent;
    private RestorationUtil restorationUtil;

    private WorldGuardPlugin worldGuard;


    //private ConcurrentHashMap<Player, ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long,
    //        BaseBlock>>> map = new ConcurrentHashMap<>();
    private final long lastActivationTime = 18000;
    private long lastActivation = 0;
    private Map<UUID, Long> daveHitList = new HashMap<>();

    private static final Set<Material> AFFECTED_ITEMS = Set.of(
            Material.IRON_BLOCK, Material.IRON_ORE, Material.IRON_INGOT,
            Material.GOLD_BLOCK, Material.GOLD_ORE, Material.GOLD_INGOT, Material.GOLD_NUGGET,
            Material.REDSTONE_ORE, Material.REDSTONE,
            Material.LAPIS_BLOCK, Material.LAPIS_ORE, Material.LAPIS_LAZULI,
            Material.DIAMOND_BLOCK, Material.DIAMOND_ORE, Material.DIAMOND,
            Material.EMERALD_BLOCK, Material.EMERALD_ORE, Material.EMERALD
    );

    public CursedMine(World world, ProtectedRegion[] regions, AdminComponent adminComponent,
                      PrayerComponent prayerComponent, HighScoresComponent highScoresComponent,
                      BlockStateComponent blockStateComponent, RestorationUtil restorationUtil) {

        super(world, regions[0]);

        this.floodGate = regions[1];

        this.adminComponent = adminComponent;
        this.prayerComponent = prayerComponent;
        this.highScoresComponent = highScoresComponent;
        this.blockStateComponent = blockStateComponent;
        this.restorationUtil = restorationUtil;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        setUpWorldGuard();
    }

    @Override
    public void forceRestoreBlocks() {
        blockStateComponent.popAllBlocks(BlockStateKind.CURSED_MINE);
    }

    @Override
    public void run() {

        checkHitList();

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
    public void equalize() { }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }

    private void setUpWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) {
            return; // Maybe you want throw an exception instead
        }

        this.worldGuard = (WorldGuardPlugin) plugin;
    }

    public void addToHitList(Player player) {
        daveHitList.put(player.getUniqueId(), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
    }

    public void removeFromHitList(Player player) {
        daveHitList.remove(player.getUniqueId());
    }

    public boolean isOnHitList(Player player) {
        return daveHitList.containsKey(player.getUniqueId());
    }

    public void checkHitList() {
        daveHitList.entrySet().removeIf(stringLongEntry -> stringLongEntry.getValue() >= System.currentTimeMillis());
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
            EditSession skullEditor = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, 1);
            skullEditor.rawSetBlock(v, new SkullBlock(0x1, b, player.getName()));
        }
    }

    public void randomRestore() {
        blockStateComponent.popBlocksOlderThan(
                BlockStateKind.CURSED_MINE,
                TimeUnit.SECONDS.toMillis(ChanceUtil.getRangedRandom(9, 60))
        );
    }

    public void revertPlayer(Player player) {
        blockStateComponent.popBlocksCreatedBy(BlockStateKind.CURSED_MINE, player);
    }

    public void sweepFloor() {
        for (Item item : getWorld().getEntitiesByClass(Item.class)) {

            if (!contains(item)) continue;

            Material type = item.getItemStack().getType();
            for (Material aItem : AFFECTED_ITEMS) {
                if (aItem == type) {
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

        Location modifiable = new Location(getWorld(), 0, 0, 0);
        Location previousLoc;

        for (Entity e : getContained(InventoryHolder.class)) {
            try {
                Inventory eInventory = ((InventoryHolder) e).getInventory();

                if (e instanceof Player) {

                    if (adminComponent.isAdmin((Player) e)) continue;

                    modifiable = e.getLocation(modifiable);

                    // Emerald
                    long diff = System.currentTimeMillis() - lastActivation;
                    if (modifiable.getY() < 30 && (lastActivation == 0 || diff <= lastActivationTime * .35 || diff >= lastActivationTime * 5)) {
                        for (int i = 0; i < ChanceUtil.getRangedRandom(2, 5); i++) {

                            previousLoc = modifiable.clone();
                            modifiable = LocationUtil.findRandomLoc(previousLoc, 5, true, false);

                            if (modifiable.getBlock().getType() != Material.AIR) {
                                modifiable = previousLoc;
                            }

                            getWorld().spawn(modifiable, Blaze.class);
                        }
                    }
                }

                for (int i = 0; i < (ItemUtil.countFilledSlots(eInventory.getContents()) / 2) - 2 || i < 1; i++) {

                    if (e instanceof Player) {
                        if (ChanceUtil.getChance(15) && checkInventory((Player) e, eInventory.getContents())) {
                            ChatUtil.sendNotice(e, "Divine intervention protects some of your items.");
                            continue;
                        }
                    }

                    // Iron
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.IRON_BLOCK, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.IRON_ORE, ChanceUtil.getRandom(4), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.IRON_INGOT, ChanceUtil.getRandom(8), true);

                    // Gold
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.GOLD_BLOCK, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.GOLD_ORE, ChanceUtil.getRandom(4), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.GOLD_INGOT, ChanceUtil.getRandom(10), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.GOLD_NUGGET, ChanceUtil.getRandom(80), true);

                    // Redstone
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.REDSTONE_BLOCK, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.REDSTONE_ORE, ChanceUtil.getRandom(4), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.REDSTONE, ChanceUtil.getRandom(34), true);

                    // Lap
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.LAPIS_BLOCK, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.LAPIS_ORE, ChanceUtil.getRandom(4), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.LAPIS_LAZULI, ChanceUtil.getRandom(34), true);

                    // Diamond
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.DIAMOND_BLOCK, ChanceUtil.getRandom(2), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.DIAMOND_ORE, ChanceUtil.getRandom(4), true);
                    ItemUtil.removeItemOfType((InventoryHolder) e, Material.DIAMOND, ChanceUtil.getRandom(16), true);
                }
            } catch (Exception ex) {
                log.warning("=== Cursed Mine Drain System Error ===");
                if (e instanceof Player) {
                    log.warning("The player: " + e.getName() + " has avoided the drain.");
                } else {
                    log.warning("An entity has avoided the drain.");
                }
            }
        }
    }

    private boolean checkInventory(Player player, ItemStack[] itemStacks) {
        if (!inst.hasPermission(player, "aurora.tome.divinity")) return false;

        for (ItemStack stack : itemStacks) {
            if (stack == null) {
                continue;
            }

            if (AFFECTED_ITEMS.contains(stack.getType())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasSilkTouch(ItemStack item) {

        return item.containsEnchantment(Enchantment.SILK_TOUCH);
    }

    private boolean hasFortune(ItemStack item) {

        return item.containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS);
    }

    private static final Set<Material> REPLACEABLE_TYPES = Set.of(
            Material.WATER, Material.OAK_PLANKS, Material.AIR
    );

    private void changeWater() {
        Material newType = Material.AIR;
        if (lastActivation == 0 || System.currentTimeMillis() - lastActivation >= lastActivationTime) {
            newType = Material.OAK_PLANKS;
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

                if (REPLACEABLE_TYPES.contains(block.getType())) {
                    block.setType(newType);
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

    private void ghost(final Player player, Material blockType) {

        try {
            if (ChanceUtil.getChance(player.getLocation().getBlockY())) {
                if (ChanceUtil.getChance(2)) {
                    switch (ChanceUtil.getRandom(6)) {
                        case 1:
                            ChatUtil.sendNotice(player, "Caspher the friendly ghost drops some bread.");
                            player.getWorld().dropItemNaturally(player.getLocation(),
                                    new ItemStack(Material.BREAD, ChanceUtil.getRandom(16)));
                            break;
                        case 2:
                            ChatUtil.sendNotice(player, "COOKIE gives you a cookie.");
                            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.COOKIE));
                            break;
                        case 3:
                            ChatUtil.sendNotice(player, "Caspher the friendly ghost appears.");
                            for (int i = 0; i < 8; i++) {
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        new ItemStack(Material.IRON_INGOT, ChanceUtil.getRandom(64)));
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandom(64)));
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(64)));
                            }
                            break;
                        case 4:
                            ChatUtil.sendNotice(player, "John gives you a new jacket.");
                            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.LEATHER_CHESTPLATE));
                            break;
                        case 5:
                            ChatUtil.sendNotice(player, "Tim teleports items to you.");
                            getContained(Item.class).forEach(i -> i.teleport(player));

                            // Add in some extra drops just in case the loot wasn't very juicy
                            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.IRON_INGOT, ChanceUtil.getRandom(64)));
                            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandom(64)));
                            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(64)));
                            break;
                        case 6:
                            ChatUtil.sendNotice(player, "Dan gives you a sparkling touch.");

                            Material type;
                            switch (ChanceUtil.getRandom(3)) {
                                case 1:
                                    type = Material.IRON_INGOT;
                                    break;
                                case 2:
                                    type = Material.GOLD_INGOT;
                                    break;
                                case 3:
                                    type = Material.DIAMOND;
                                    break;
                                default:
                                    type = Material.REDSTONE;
                                    break;
                            }

                            prayerComponent.influencePlayer(player,
                                    PrayerComponent.constructPrayer(player, new InventoryFX(type, 64), 5000));
                            break;
                        default:
                            break;
                    }
                } else {

                    if (ItemUtil.hasAncientArmour(player) && ChanceUtil.getChance(2)) {
                        ChatUtil.sendNotice(player, ChatColor.AQUA, "Your armour blocks an incoming ghost attack.");
                        return;
                    }

                    adminComponent.depowerPlayer(player);

                    Location modifiedLoc = null;

                    switch (ChanceUtil.getRandom(11)) {
                        case 1:
                            if (ChanceUtil.getChance(4)) {
                                if (blockType == Material.DIAMOND_ORE) {
                                    addToHitList(player);
                                    ChatUtil.sendWarning(player, "You ignite fumes in the air!");
                                    EditSession ess = WorldEdit.getInstance()
                                            .getEditSessionFactory()
                                            .getEditSession(new BukkitWorld(player.getWorld()), -1);
                                    try {
                                        ess.fillXZ(BukkitUtil.toVector(player.getLocation()), new BaseBlock(Material.FIRE), 20, 20, true);
                                    } catch (MaxChangedBlocksException ignored) {

                                    }
                                    for (int i = ChanceUtil.getRandom(24) + 20; i > 0; --i) {
                                        final boolean untele = i == 11;
                                        server.getScheduler().runTaskLater(inst, () -> {
                                            if (untele) {
                                                revertPlayer(player);
                                                removeFromHitList(player);
                                            }

                                            if (!contains(player)) return;

                                            Location l = LocationUtil.findRandomLoc(player.getLocation().getBlock(), 3, true, false);
                                            ExplosionStateFactory.createExplosion(l, 3, true, false);
                                        }, 12 * i);
                                    }
                                } else {
                                    addToHitList(player);
                                    player.chat("Who's a good ghost?!?!");
                                    server.getScheduler().runTaskLater(inst, () -> {
                                        player.chat("Don't hurt me!!!");
                                        server.getScheduler().runTaskLater(inst, () -> {
                                            player.chat("Nooooooooooo!!!");

                                            try {
                                                prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                                        PrayerType.CANNON, TimeUnit.MINUTES.toMillis(2)));
                                            } catch (UnsupportedPrayerException ex) {
                                                ex.printStackTrace();
                                            }
                                        }, 20);
                                    }, 20);
                                }
                                break;
                            }
                        case 2:
                            ChatUtil.sendWarning(player, "You find yourself falling from the sky...");
                            addToHitList(player);
                            modifiedLoc = new Location(player.getWorld(), player.getLocation().getX(), 350, player.getLocation().getZ());
                            break;
                        case 3:
                            ChatUtil.sendWarning(player, "George plays with fire, sadly too close to you.");
                            prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                    PrayerType.FIRE, TimeUnit.SECONDS.toMillis(45)));
                            break;
                        case 4:
                            ChatUtil.sendWarning(player, "Simon says pick up sticks.");
                            for (int i = 0; i < player.getInventory().getContents().length * 1.5; i++) {
                                player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.STICK, 64));
                            }
                            break;
                        case 5:
                            ChatUtil.sendWarning(player, "Ben dumps out your backpack.");
                            prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                    PrayerType.BUTTERFINGERS, TimeUnit.SECONDS.toMillis(10)));
                            break;
                        case 6:
                            ChatUtil.sendWarning(player, "Merlin attacks with a mighty rage!");
                            prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                    PrayerType.MERLIN, TimeUnit.SECONDS.toMillis(20)));
                            break;
                        case 7:
                            ChatUtil.sendWarning(player, "Dave tells everyone that your mining!");
                            Bukkit.broadcastMessage(ChatColor.GOLD + "The player: "
                                    + player.getDisplayName() + " is mining in the cursed mine!!!");
                            break;
                        case 8:
                            ChatUtil.sendWarning(player, "Dave likes your food.");
                            addToHitList(player);
                            prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                    PrayerType.STARVATION, TimeUnit.MINUTES.toMillis(15)));
                            break;
                        case 9:
                            ChatUtil.sendWarning(player, "Hallow declares war on YOU!");
                            for (int i = 0; i < ChanceUtil.getRangedRandom(10, 30); i++) {
                                Blaze blaze = getWorld().spawn(player.getLocation(), Blaze.class);
                                blaze.setTarget(player);
                                blaze.setRemoveWhenFarAway(true);
                            }
                            break;
                        case 10:
                            ChatUtil.sendWarning(player, "A legion of hell hounds appears!");
                            for (int i = 0; i < ChanceUtil.getRangedRandom(10, 30); i++) {
                                Wolf wolf = getWorld().spawn(player.getLocation(), Wolf.class);
                                wolf.setTarget(player);
                                wolf.setRemoveWhenFarAway(true);
                            }
                            break;
                        case 11:
                            if (blockType == Material.EMERALD_ORE) {
                                ChatUtil.sendNotice(player, "Dave got a chemistry set!");
                                addToHitList(player);
                                prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                        PrayerType.DEADLYPOTION, TimeUnit.MINUTES.toMillis(30)));
                            } else {
                                ChatUtil.sendWarning(player, "Dave says hi, that's not good.");
                                addToHitList(player);
                                prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                        PrayerType.SLAP, TimeUnit.MINUTES.toMillis(30)));
                                prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                        PrayerType.BUTTERFINGERS, TimeUnit.MINUTES.toMillis(30)));
                                prayerComponent.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                        PrayerType.FIRE, TimeUnit.MINUTES.toMillis(30)));
                            }
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

    private static final Set<Material> TRIGGER_BLOCKS = Set.of(
            Material.STONE_BUTTON, Material.TRIPWIRE
    );

    private static final Set<Action> TRIGGER_INTERACTIONS = Set.of(
            Action.PHYSICAL, Action.RIGHT_CLICK_BLOCK
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        final Block block = event.getClickedBlock();

        if (contains(block) && TRIGGER_BLOCKS.contains(block.getType()) && TRIGGER_INTERACTIONS.contains(event.getAction())) {
            long temp = lastActivation;
            lastActivation = System.currentTimeMillis();
            if (System.currentTimeMillis() - temp <= lastActivationTime * 5) {
                 lastActivation -= lastActivationTime * .4;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (adminComponent.isAdmin(player) || !contains(block)) {
            return;
        }

        ItemStack itemInHand = player.getItemInHand();

        if (EnvironmentUtil.isValuableOre(block) && ItemUtil.isPickaxe(itemInHand)) {
            Material type = block.getType();

            if (ChanceUtil.getChance(4)) {
                ItemStack rawDrop = EnvironmentUtil.getOreDrop(block.getType(), hasSilkTouch(itemInHand));

                if (inst.hasPermission(player, "aurora.tome.cursedsmelting") && !hasSilkTouch(itemInHand)) {
                    switch (type) {
                        case GOLD_ORE:
                            rawDrop.setType(Material.GOLD_INGOT);
                            break;
                        case IRON_ORE:
                            rawDrop.setType(Material.IRON_INGOT);
                            break;
                    }
                }

                if (hasFortune(itemInHand) && !EnvironmentUtil.isOre(rawDrop.getType())) {
                    rawDrop.setAmount(rawDrop.getAmount()
                            * ItemUtil.fortuneModifier(type, ItemUtil.fortuneLevel(itemInHand)));
                }
                rawDrop.setAmount(rawDrop.getAmount() * ChanceUtil.getRangedRandom(4, 8));

                player.getInventory().addItem(rawDrop);
                if (ModifierComponent.getModifierCenter().isActive(ModifierType.DOUBLE_CURSED_ORES)) {
                    player.getInventory().addItem(rawDrop.clone());
                }
            }
            event.setExpToDrop((70 - player.getLocation().getBlockY()) / 2);

            if (ChanceUtil.getChance(3000)) {
                ChatUtil.sendNotice(player, "You feel as though a spirit is trying to tell you something...");
                player.getInventory().addItem(BookUtil.Lore.Areas.theGreatMine());
            }

            if (ChanceUtil.getChance(10000)) {
                ChatUtil.sendNotice(player, "You find a dusty old book...");
                player.getInventory().addItem(CustomItemCenter.build(CustomItems.TOME_OF_CURSED_SMELTING));
            }

            eatFood(player);
            poison(player, 6);
            ghost(player, type);

            try {
                blockStateComponent.pushBlock(BlockStateKind.CURSED_MINE, player, block.getState());
                restorationUtil.blockAndLogEvent(event);

                highScoresComponent.update(player, ScoreTypes.CURSED_ORES_MINED, 1);
                return;
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();
            }
        }

        event.setCancelled(true);
        ChatUtil.sendWarning(player, "You cannot break this block for some reason.");
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

        if ((blockStateComponent.hasPlayerBrokenBlocks(BlockStateKind.CURSED_MINE, player)
                || isOnHitList(player)) && !accepted.contains(event.getCause())) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You have been tele-blocked!");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (contains(event.getBlockClicked())) {
            Player player = event.getPlayer();
            Block block = event.getBlockClicked();

            try {
                blockStateComponent.pushBlock(BlockStateKind.CURSED_MINE, event.getPlayer(), block.getState());
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();

                event.setCancelled(true);
            }
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

    private static EDBEExtractor<LivingEntity, Player, Arrow> extractor = new EDBEExtractor<>(
            LivingEntity.class,
            Player.class,
            Arrow.class
    );

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        CombatantPair<LivingEntity, Player, Arrow> result = extractor.extractFrom(event);

        if (result == null) return;

        Player player = result.getDefender();

        if (!contains(player)) return;
        if (ChanceUtil.getChance(5) && ItemUtil.isHoldingMasterSword(player) && ItemUtil.hasAncientArmour(player)) {
            EffectUtil.Master.ultimateStrength(player);
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

        if (isOnHitList(player) || contains(player)) {
            highScoresComponent.update(player, ScoreTypes.CURSED_MINE_DEATHS, 1);

            if (contains(player) && ChanceUtil.getChance(500)) {
                ChatUtil.sendNotice(player, "You feel as though a spirit is trying to tell you something...");
                event.getDrops().add(BookUtil.Lore.Areas.theGreatMine());
            }

            removeFromHitList(player);
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isOnHitList(player)) {
            player.setHealth(0);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (blockStateComponent.hasPlayerBrokenBlocks(BlockStateKind.CURSED_MINE, player) && contains(player)) {
            addToHitList(player);
        }
    }
}
