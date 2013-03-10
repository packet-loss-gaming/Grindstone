package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.blocks.SkullBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.exceptions.UnsupportedPrayerException;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class CursedMine extends AbstractRegionedArena implements MonitoredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;
    private PrayerComponent prayerComponent;

    private WorldGuardPlugin worldGuard;

    private ConcurrentHashMap<Player, ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long,
            BaseBlock>>> map = new ConcurrentHashMap<>();
    private List<Player> daveHitList = new ArrayList<>();

    private final int[] items = new int[] {
            BlockID.IRON_BLOCK, BlockID.IRON_ORE, ItemID.IRON_BAR,
            BlockID.GOLD_BLOCK, BlockID.GOLD_ORE, ItemID.GOLD_BAR, ItemID.GOLD_NUGGET,
            BlockID.REDSTONE_ORE, BlockID.GLOWING_REDSTONE_ORE, ItemID.REDSTONE_DUST,
            BlockID.LAPIS_LAZULI_BLOCK, BlockID.LAPIS_LAZULI_ORE, ItemID.INK_SACK,
            BlockID.DIAMOND_BLOCK, BlockID.DIAMOND_ORE, ItemID.DIAMOND,
            BlockID.EMERALD_BLOCK, BlockID.EMERALD_ORE, ItemID.EMERALD
    };

    public CursedMine(World world, ProtectedRegion region, AdminComponent adminComponent,
                      PrayerComponent prayerComponent) {

        super(world, region);
        this.adminComponent = adminComponent;
        this.prayerComponent = prayerComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        setUpWorldGuard();
    }

    @Override
    public void forceRestoreBlocks() {

        BaseBlock b;
        for (ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e : map.values()) {
            for (Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> se : e.entrySet()) {
                b = se.getValue().getValue();
                if (!se.getKey().getChunk().isLoaded()) se.getKey().getChunk().load();
                se.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
            }
        }
        map.clear();
    }

    @Override
    public void run() {

        equalize();
        drain();
        sweepFloor();
        randomRestore();
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

        int min;
        BaseBlock b;
        for (Map.Entry<Player, ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long,
                BaseBlock>>> e : map.entrySet()) {
            min = ChanceUtil.getRangedRandom(9000, 60000);
            for (Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> se : e.getValue().entrySet()) {
                if ((System.currentTimeMillis() - se.getValue().getKey()) > min) {
                    b = se.getValue().getValue();
                    if (!se.getKey().getChunk().isLoaded()) se.getKey().getChunk().load();
                    se.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
                    e.getValue().remove(se.getKey());
                    if (!EnvironmentUtil.isOre(b.getType())) continue;
                    for (int i = 0; i < 20; i++) getWorld().playEffect(se.getKey(), Effect.MOBSPAWNER_FLAMES, 0);
                }
            }
            if (e.getValue().isEmpty()) {
                map.remove(e.getKey());
            }
        }
    }

    public void revertPlayer(Player player) {

        BaseBlock b;
        if (map.containsKey(player)) {
            for (Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e : map.get(player).entrySet()) {
                b = e.getValue().getValue();
                if (!e.getKey().getChunk().isLoaded()) e.getKey().getChunk().load();
                e.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
                if (!EnvironmentUtil.isOre(b.getType())) continue;
                for (int i = 0; i < 20; i++) getWorld().playEffect(e.getKey(), Effect.MOBSPAWNER_FLAMES, 0);
            }
        }
    }

    public void sweepFloor() {

        for (Item item : getWorld().getEntitiesByClass(Item.class)) {

            if (!contains(item)) continue;

            int id = item.getItemStack().getTypeId();
            for (int aItem : items) {
                if (aItem == id) {
                    item.getItemStack().setAmount((int) (item.getItemStack().getAmount() * .8));
                    break;
                }
            }
        }
    }

    public void drain() {

        for (Player player : getContainedPlayers()) {
            try {
                PlayerInventory pInventory = player.getInventory();

                for (int i = 0; i < (ItemUtil.countFilledSlots(pInventory.getContents()) / 2) - 2 || i < 1; i++) {

                    if (ChanceUtil.getChance(15) && checkInventory(player, pInventory.getContents())) {
                        ChatUtil.sendNotice(player, "Divine intervention protects some of your items.");
                        continue;
                    }

                    // Iron
                    pInventory.removeItem(new ItemStack(BlockID.IRON_BLOCK, ChanceUtil.getRandom(2)));
                    pInventory.removeItem(new ItemStack(BlockID.IRON_ORE, ChanceUtil.getRandom(4)));
                    pInventory.removeItem(new ItemStack(ItemID.IRON_BAR, ChanceUtil.getRandom(8)));

                    // Gold
                    pInventory.removeItem(new ItemStack(BlockID.GOLD_BLOCK, ChanceUtil.getRandom(2)));
                    pInventory.removeItem(new ItemStack(BlockID.GOLD_ORE, ChanceUtil.getRandom(4)));
                    pInventory.removeItem(new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRandom(10)));
                    pInventory.removeItem(new ItemStack(ItemID.GOLD_NUGGET, ChanceUtil.getRandom(80)));

                    // Redstone
                    pInventory.removeItem(new ItemStack(BlockID.REDSTONE_ORE, ChanceUtil.getRandom(2)));
                    pInventory.removeItem(new ItemStack(BlockID.GLOWING_REDSTONE_ORE, ChanceUtil.getRandom(2)));
                    pInventory.removeItem(new ItemStack(ItemID.REDSTONE_DUST, ChanceUtil.getRandom(34)));

                    // Lap
                    pInventory.removeItem(new ItemStack(BlockID.LAPIS_LAZULI_BLOCK, ChanceUtil.getRandom(2)));
                    pInventory.removeItem(new ItemStack(BlockID.LAPIS_LAZULI_ORE, ChanceUtil.getRandom(4)));
                    pInventory.removeItem(new ItemStack(ItemID.INK_SACK, ChanceUtil.getRandom(34), (short) 4));

                    // Diamond
                    pInventory.removeItem(new ItemStack(BlockID.DIAMOND_BLOCK, ChanceUtil.getRandom(2)));
                    pInventory.removeItem(new ItemStack(BlockID.DIAMOND_ORE, ChanceUtil.getRandom(4)));
                    pInventory.removeItem(new ItemStack(ItemID.DIAMOND, ChanceUtil.getRandom(16)));

                    // Emerald
                    //pInventory.removeItem(new ItemStack(BlockID.EMERALD_BLOCK, ChanceUtil.getRandom(2)));
                    //pInventory.removeItem(new ItemStack(BlockID.EMERALD_ORE, ChanceUtil.getRandom(4)));
                    //pInventory.removeItem(new ItemStack(ItemID.EMERALD, ChanceUtil.getRandom(12)));
                }
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " has avoided the drain.");
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

    private void ghost(Player player) {

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
                            player.getWorld().dropItemNaturally(player.getLocation(),
                                    new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRandom(512)));
                            player.getWorld().dropItemNaturally(player.getLocation(),
                                    new ItemStack(ItemID.IRON_BAR, ChanceUtil.getRandom(512)));
                            player.getWorld().dropItemNaturally(player.getLocation(),
                                    new ItemStack(ItemID.DIAMOND, ChanceUtil.getRandom(512)));
                            break;
                        case 4:
                            ChatUtil.sendNotice(player, "John gives you a new jacket.");
                            player.getWorld().dropItemNaturally(player.getLocation(),
                                    new ItemStack(ItemID.LEATHER_CHEST));
                            break;
                        default:
                            break;
                    }
                } else {

                    if (ItemUtil.hasAncientArmour(player) && ChanceUtil.getChance(2)) {
                        ChatUtil.sendNotice(player, "Your armour blocks an incoming ghost attack.");
                        return;
                    }

                    Location modifiedLoc = null;

                    switch (ChanceUtil.getRandom(10)) {
                        case 1:
                            ChatUtil.sendWarning(player, "Dave decides to play racket ball...");
                            ChatUtil.sendWarning(player, "Using you as the ball!");
                            daveHitList.add(player);
                            modifiedLoc = new Location(player.getWorld(), player.getLocation().getX(), 100,
                                    player.getLocation().getZ());
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.ROCKET, TimeUnit.MINUTES.toMillis(30)));
                            break;
                        case 2:
                            ChatUtil.sendWarning(player, "Dave says hi, that's not good.");
                            daveHitList.add(player);
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.SLAP, TimeUnit.MINUTES.toMillis(30)));
                            break;
                        case 3:
                            ChatUtil.sendWarning(player, "George plays with fire, sadly too close to you.");
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.FIRE, TimeUnit.MINUTES.toMillis(2)));
                            break;
                        case 4:
                            ChatUtil.sendWarning(player, "Simon says pick up sticks.");
                            for (int i = 0; i < player.getInventory().getContents().length; i++) {
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
                                    PrayerType.ALONZO, TimeUnit.SECONDS.toMillis(20)));
                            break;
                        case 7:
                            ChatUtil.sendWarning(player, "Dave tells everyone that your mining!");
                            Bukkit.broadcastMessage(ChatColor.GOLD + "The player: "
                                    + player.getDisplayName() + " is mining in the cursed mine!!!");
                            break;
                        case 8:
                            ChatUtil.sendWarning(player, "Dave likes your food.");
                            daveHitList.add(player);
                            prayerComponent.uninfluencePlayer(player);
                            prayerComponent.influencePlayer(player, prayerComponent.constructPrayer(player,
                                    PrayerType.STARVATION, TimeUnit.MINUTES.toMillis(15)));
                            break;
                        case 9:
                            ChatUtil.sendWarning(player, "1alonzo4 declares war on YOU!");
                            for (int i = 0; i < ChanceUtil.getRandom(30); i++) {
                                Blaze blaze = getWorld().spawn(player.getLocation(), Blaze.class);
                                blaze.setTarget(player);
                            }
                            break;
                        case 10:
                            ChatUtil.sendWarning(player, "Hell hounds appear out of the floor!");
                            for (int i = 0; i < ChanceUtil.getRandom(30); i++) {
                                Wolf wolf = getWorld().spawn(player.getLocation(), Wolf.class);
                                wolf.setAngry(true);
                                wolf.setTarget(player);
                            }
                            break;
                        default:
                            break;
                    }

                    if (modifiedLoc != null) player.teleport(modifiedLoc,
                            PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
                }
            }
        } catch (UnsupportedPrayerException ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
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

            ItemStack rawDrop = EnvironmentUtil.getOreDrop(block.getTypeId(), hasSilkTouch(itemInHand));

            if (inst.hasPermission(player, "aurora.mining.burningember") && !hasSilkTouch(itemInHand)) {
                switch (typeId) {
                    case BlockID.GOLD_ORE:
                        rawDrop.setTypeId(ItemID.GOLD_BAR);
                        break;
                    case BlockID.IRON_ORE:
                        rawDrop.setTypeId(ItemID.IRON_BAR);
                        break;
                }
            }

            if (hasFortune(itemInHand) && !EnvironmentUtil.isOre(rawDrop.getTypeId())) {
                rawDrop.setAmount(rawDrop.getAmount()
                        * ChanceUtil.getRangedRandom(4, 8) * ItemUtil.fortuneMultiplier(itemInHand));
            } else {
                rawDrop.setAmount(rawDrop.getAmount() * ChanceUtil.getRangedRandom(4, 16));
            }

            event.setExpToDrop((70 - player.getLocation().getBlockY()) / 2);
            if (ChanceUtil.getChance(4)) player.getInventory().addItem(rawDrop);

            eatFood(player);
            poison(player, 6);
            ghost(player);

            if (!map.containsKey(event.getPlayer())) {
                map.put(event.getPlayer(), new ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long, BaseBlock>>());
            }
            map.get(event.getPlayer()).put(block.getLocation(),
                    new AbstractMap.SimpleEntry<>(System.currentTimeMillis(),
                            new BaseBlock(block.getTypeId(), block.getData())));

        } else if (contains(block)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot break this block for some reason.");
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {

        if (contains(event.getEntity())) {
            for (ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e : map.values()) {
                for (Location l : e.keySet()) {
                    if (l.getBlock().equals(event.getEntity().getLocation().getBlock())) {
                        int is = event.getEntity().getItemStack().getTypeId();

                        for (int aItem : items) {
                            if (is == aItem) event.setCancelled(true);
                        }
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        if (contains(event.getItemDrop())) {
            for (ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e : map.values()) {
                for (Location l : e.keySet()) {
                    if (event.getItemDrop().getLocation().getBlock().equals(l.getBlock())) {

                        int is = event.getItemDrop().getItemStack().getTypeId();

                        for (int aItem : items) {
                            if (is == aItem) {
                                ChatUtil.sendError(event.getPlayer(), "You can't drop that here.");
                                event.setCancelled(true);
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();

        if (!adminComponent.isAdmin(player) && contains(event.getBlock())
                && !inst.hasPermission(player, "aurora.mine.builder")) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, ChatColor.DARK_RED, "You don't have permission for this area.");
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();

        if (map.containsKey(player) && !daveHitList.contains(player)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You have been tele-blocked!");
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event) {

        if (contains(event.getBlockClicked())) {

            final Block block = event.getBlockClicked();

            if (!map.containsKey(event.getPlayer())) {
                map.put(event.getPlayer(), new ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long, BaseBlock>>());
            }
            map.get(event.getPlayer()).put(block.getLocation(),
                    new AbstractMap.SimpleEntry<>(System.currentTimeMillis(),
                            new BaseBlock(block.getTypeId(), block.getData())));
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {

        if (contains(event.getBlockClicked())) {
            event.setCancelled(true);
            ChatUtil.sendNotice(event.getPlayer(), ChatColor.DARK_RED, "You don't have permission for this area.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        Player player = event.getPlayer();

        if (event.getCause().getEffect().getType().isHoly() && contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {

        if (event.getEntity() instanceof Player) {

            Player player = (Player) event.getEntity();

            if (!contains(player)) return;
            if (ChanceUtil.getChance(7) && ItemUtil.hasMasterSword(player) && ItemUtil.hasAncientArmour(player)) {
                EffectUtil.Master.ultimateStrength(player);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        revertPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity().getPlayer();
        revertPlayer(player);

        if (daveHitList.contains(player) || contains(player)) {

            if (contains(player) && ChanceUtil.getChance(50)) {
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
