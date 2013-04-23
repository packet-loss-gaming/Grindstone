package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.SacrificeComponent;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.EggHatchEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class EnchantedForest extends AbstractRegionedArena implements MonitoredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private final Random random = new Random();
    private ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> map = new ConcurrentHashMap<>();

    public EnchantedForest(World world, ProtectedRegion region, AdminComponent adminComponent) {

        super(world, region);
        this.adminComponent = adminComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void forceRestoreBlocks() {

        BaseBlock b;
        for (Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e : map.entrySet()) {
            b = e.getValue().getValue();
            if (!e.getKey().getChunk().isLoaded()) e.getKey().getChunk().load();
            e.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
        }
        map.clear();
    }

    @Override
    public void run() {

        equalize();
        restoreBlocks();
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

    public void restoreBlocks() {

        int min = 1000 * 60 * 170;

        BaseBlock b;
        Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e;
        Iterator<Map.Entry<Location,AbstractMap.SimpleEntry<Long,BaseBlock>>> it = map.entrySet().iterator();

        while(it.hasNext()) {
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

    private List<ItemStack> getRandomDropSet(CommandSender player) {

        // Create the Sacrifice
        int amt, value;
        if (ChanceUtil.getChance(59)) {
            amt = 64;
            value = 5248;
        } else {
            amt = 8;
            value = 88;
        }

        // Sacrifice and make loot list
        List<ItemStack> loot;
        do {
            loot = SacrificeComponent.getCalculatedLoot(player, amt, value);
        } while (loot == null || loot.size() < 1);

        // Shuffle and return loot for variety
        Collections.shuffle(loot);
        return loot;
    }

    private void eatFood(Player player) {

        if (player.getSaturation() - 1 >= 0) {
            player.setSaturation(player.getSaturation() - 1);
        } else if (player.getFoodLevel() - 1 >= 0) {
            player.setFoodLevel(player.getFoodLevel() - 1);
        }
    }

    private void trick(final Player player) {

        if (ChanceUtil.getChance(256)) {
            final PlayerInventory pInv = player.getInventory();
            switch (ChanceUtil.getRandom(2)) {
                case 1:
                    boolean hasAxe = true;
                    switch (pInv.getItemInHand().getTypeId()) {
                        case ItemID.DIAMOND_AXE:
                            pInv.addItem(new ItemStack(ItemID.DIAMOND, 2), new ItemStack(ItemID.STICK, 2));
                            break;
                        case ItemID.GOLD_AXE:
                            pInv.addItem(new ItemStack(ItemID.GOLD_BAR, 2), new ItemStack(ItemID.STICK, 2));
                            break;
                        case ItemID.IRON_AXE:
                            pInv.addItem(new ItemStack(ItemID.IRON_BAR, 2), new ItemStack(ItemID.STICK, 2));
                            break;
                        case ItemID.WOOD_AXE:
                            pInv.addItem(new ItemStack(BlockID.WOOD, 2), new ItemStack(ItemID.STICK, 2));
                            break;
                        default:
                            hasAxe = false;
                            ChatUtil.sendWarning(player, "The fairy couldn't find an axe and instead throws a rock" +
                                    "at you.");
                            player.damage(7);
                            player.setVelocity(new Vector(
                                    random.nextDouble() * 2.0 - 1.5,
                                    random.nextDouble() * 1,
                                    random.nextDouble() * 2.0 - 1.5)
                            );
                    }

                    if (hasAxe) {
                        ChatUtil.sendWarning(player, "The fairy breaks your axe.");
                        server.getScheduler().runTaskLater(inst, new Runnable() {
                            @Override
                            public void run() {

                                player.getInventory().setItemInHand(null);
                            }
                        }, 1);
                    }
                    break;
                case 2:
                    // Make potion
                    ItemStack potion = new Potion(PotionType.INSTANT_DAMAGE).toItemStack(1);
                    PotionMeta pMeta = (PotionMeta) potion.getItemMeta();
                    pMeta.addCustomEffect(
                            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 600, 2), false);
                    pMeta.addCustomEffect(
                            new PotionEffect(PotionEffectType.REGENERATION, 20 * 600, 2), false);
                    pMeta.addCustomEffect(
                            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 2), false);
                    pMeta.addCustomEffect(
                            new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 2), false);
                    pMeta.addCustomEffect(
                            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2), false);
                    pMeta.setDisplayName(ChatColor.WHITE + "Extreme Combat Potion");
                    potion.setItemMeta(pMeta);

                    // Give potion
                    ChatUtil.sendWarning(player, "You might need this friend ;)");
                    getWorld().dropItemNaturally(player.getLocation(), potion);
                    int waves = 5 * ChanceUtil.getRandom(3);
                    for (int i = 0; i < waves; i++) {
                        server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                            @Override
                            public void run() {

                                ChatUtil.sendNotice(getContainedPlayers(), "Slimes away guys!");

                                BlockVector min = getRegion().getMinimumPoint();
                                BlockVector max = getRegion().getMaximumPoint();

                                short sOut = 1000;
                                com.sk89q.worldedit.Vector v;
                                for (int i = 0; i < 25 * ChanceUtil.getRandom(4); i++) {
                                    sOut--;
                                    if (sOut < 0) break;
                                    v = LocationUtil.pickLocation(min.getX(), max.getX(), min.getZ(), max.getZ());
                                    v = v.add(0, 83, 0);
                                    if (getRegion().contains(v.getBlockX(), v.getBlockY(), v.getBlockZ())) {
                                        Block b = getWorld().getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
                                        if (b.getTypeId() == BlockID.AIR
                                                || EnvironmentUtil.isShrubBlock(b.getTypeId())) {
                                            Slime s = (Slime) getWorld().spawnEntity(b.getLocation(), EntityType.SLIME);
                                            s.setSize(ChanceUtil.getRandom(8));
                                            s.setRemoveWhenFarAway(false);
                                            continue;
                                        }
                                    }
                                    i--;
                                }

                            }
                        }, 20 * 7 * (i + 1));
                    }
                    server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                        @Override
                        public void run() {

                            ChatUtil.sendNotice(getContainedPlayers(), "Release the god slime!");

                            BlockVector min = getRegion().getMinimumPoint();
                            BlockVector max = getRegion().getMaximumPoint();

                            short sOut = 1000;
                            com.sk89q.worldedit.Vector v;
                            for (int i = 0; i < ChanceUtil.getRandom(1); i++) {
                                sOut--;
                                if (sOut < 0) break;
                                v = LocationUtil.pickLocation(min.getX(), max.getX(), min.getZ(), max.getZ());
                                v = v.add(0, 83, 0);
                                if (getRegion().contains(v.getBlockX(), v.getBlockY(), v.getBlockZ())) {
                                    Block b = getWorld().getBlockAt(v.getBlockX(), v.getBlockY(), v.getBlockZ());
                                    if (BlockType.canPassThrough(b.getTypeId())) {
                                        Slime s = (Slime) getWorld().spawnEntity(b.getLocation(), EntityType.SLIME);
                                        s.setSize(16);
                                        s.setRemoveWhenFarAway(false);
                                        continue;
                                    }
                                }
                                i--;
                            }

                        }
                    }, 20 * 7 * (waves + 1));
                    break;

            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        ItemStack itemInHand = player.getItemInHand();

        if (!adminComponent.isAdmin(player)
                && contains(block)
                && (block.getTypeId() == BlockID.LOG || EnvironmentUtil.isShrubBlock(block))
                && itemInHand != null) {

            if (block.getTypeId() == BlockID.LOG) {
                short c = 0;
                for (ItemStack aItemStack : getRandomDropSet(player)) {
                    if (c >= 3) break;
                    getWorld().dropItemNaturally(block.getLocation(), aItemStack);
                    c++;
                }

                event.setExpToDrop(ChanceUtil.getRandom(4));
                eatFood(player);
                trick(player);
            }

            map.put(block.getLocation(), new AbstractMap.SimpleEntry<>(System.currentTimeMillis(),
                    new BaseBlock(block.getTypeId(), block.getData())));

        } else if (contains(block)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot break this block for some reason.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {

        if (contains(event.getBlock())) {

            Block block = event.getBlock();

            map.put(block.getLocation(), new AbstractMap.SimpleEntry<>(System.currentTimeMillis(),
                    new BaseBlock(block.getTypeId(), block.getData())));

            if (!ChanceUtil.getChance(14)) return;
            getWorld().dropItemNaturally(block.getLocation(), getRandomDropSet(server.getConsoleSender()).get(0));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {

        if (contains(event.getEntity())) {
            for (Location l : map.keySet()) {
                if (l.getBlock().equals(event.getEntity().getLocation().getBlock())) {
                    int is = event.getEntity().getItemStack().getTypeId();

                    if (is == BlockID.LOG || is == BlockID.SAPLING) {
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        if (contains(event.getItemDrop())) {
            for (Location l : map.keySet()) {
                if (event.getItemDrop().getLocation().getBlock().equals(l.getBlock())) {
                    int is = event.getItemDrop().getItemStack().getTypeId();

                    if (is == BlockID.LOG || is == BlockID.SAPLING) {
                        ChatUtil.sendError(event.getPlayer(), "You can't drop that here.");
                        event.setCancelled(true);
                    }
                    return;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {

        if (contains(event.getBlockClicked())) {

            final Block block = event.getBlockClicked();

            map.put(block.getLocation(), new AbstractMap.SimpleEntry<>(System.currentTimeMillis(),
                    new BaseBlock(block.getTypeId(), block.getData())));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {


        if (contains(event.getBlockClicked())) {
            event.setCancelled(true);
            ChatUtil.sendNotice(event.getPlayer(), ChatColor.DARK_RED, "You don't have permission for this area.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEggHatch(EggHatchEvent event) {

        if (contains(event.getEgg())) event.setCancelled(true);
    }

}
