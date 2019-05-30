/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.SacrificeComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.egg.EggHatchEvent;
import gg.packetloss.grindstone.prayer.PrayerFX.ButterFingersFX;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.restoration.BaseBlockRecordIndex;
import gg.packetloss.grindstone.util.restoration.BlockRecord;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class EnchantedForest extends AbstractRegionedArena implements MonitoredArena, PersistentArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private final Random random = new Random();
    private BaseBlockRecordIndex treeMap = new BaseBlockRecordIndex();
    private BaseBlockRecordIndex generalMap = new BaseBlockRecordIndex();
    private Set<Player> noTeeth = new HashSet<>();

    public EnchantedForest(World world, ProtectedRegion region, AdminComponent adminComponent) {

        super(world, region);
        this.adminComponent = adminComponent;

        reloadData();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void forceRestoreBlocks() {

        treeMap.revertAll();
        generalMap.revertAll();
    }

    @Override
    public void run() {

        equalize();
        restoreBlocks();
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
    public void equalize() { }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }

    public void restoreBlocks() {

        int mainMin = 1000 * 60 * 170;
        int secMin = mainMin / 20;

        treeMap.revertByTime(mainMin);
        generalMap.revertByTime(secMin);

        writeData(true);
    }

    private List<ItemStack> getRandomDropSet(CommandSender player) {

        // Create the Sacrifice
        int amt, value;
        if (ChanceUtil.getChance(59)) {
            amt = 64;
            value = 10496;
        } else {
            amt = 8;
            value = 176;
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
            switch (ChanceUtil.getRandom(5)) {
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
                                    random.nextDouble() * 2.0 - 1,
                                    random.nextDouble() * 1,
                                    random.nextDouble() * 2.0 - 1)
                            );
                    }

                    if (hasAxe) {
                        ChatUtil.sendWarning(player, "The fairy breaks your axe.");
                        server.getScheduler().runTaskLater(inst, () -> player.getInventory().setItemInHand(null), 1);
                    }
                    break;
                case 2:
                    player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 60, 2), true);
                    ChatUtil.sendWarning(player, "You cut your hand on the poisonous bark.");
                    break;
                case 3:
                    new ButterFingersFX().add(player);
                    ChatUtil.sendNotice(player, "The fairies throws your stuff all over the place");
                    break;
                case 4:
                    for (final Player aPlayer : getContained(Player.class)) {
                        ChatUtil.sendWarning(aPlayer, "The fairies turn rabid!");
                        IntegratedRunnable runnable = new IntegratedRunnable() {
                            @Override
                            public boolean run(int times) {
                                if (contains(aPlayer)) {
                                    aPlayer.setHealth(aPlayer.getHealth() - 1);
                                    aPlayer.playEffect(EntityEffect.HURT);
                                    ChatUtil.sendWarning(aPlayer, "A fairy tears through your flesh!");
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            public void end() {
                                ChatUtil.sendWarning(aPlayer, "The rabid fairies disperse.");
                            }
                        };
                        TimedRunnable timedRunnable = new TimedRunnable(runnable, 1);
                        timedRunnable.setTask(server.getScheduler().runTaskTimer(inst, timedRunnable, 0, 20));
                    }
                    break;
                case 5:
                    ChatUtil.sendWarning(player, "The tooth fairy takes your teeth!");
                    noTeeth.add(player);
                    server.getScheduler().runTaskLater(inst, () -> noTeeth.remove(player), 20 * 60 * 2);
                    break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (event.isFlying() && contains(player) && !adminComponent.isAdmin(player)) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, "You cannot fly here!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (!adminComponent.isAdmin(player)
                && contains(block)
                && (block.getTypeId() == BlockID.LOG || EnvironmentUtil.isShrubBlock(block))) {

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

                treeMap.addItem(new BlockRecord(block));
            } else {
                generalMap.addItem(new BlockRecord(block));
            }
        } else if (!adminComponent.isAdmin(player) && contains(block)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot break this block for some reason.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {

        if (contains(event.getBlock())) {

            Block block = event.getBlock();

            treeMap.addItem(new BlockRecord(block));

            if (!ChanceUtil.getChance(14)) return;
            getWorld().dropItemNaturally(block.getLocation(), getRandomDropSet(server.getConsoleSender()).get(0));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(ItemSpawnEvent event) {

        Item item = event.getEntity();
        if (contains(item)) {
            int typeId = item.getItemStack().getTypeId();
            if (typeId == BlockID.LOG || typeId == BlockID.SAPLING) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        Item item = event.getItemDrop();
        if (contains(item)) {
            int typeId = item.getItemStack().getTypeId();
            if (typeId == BlockID.LOG || typeId == BlockID.SAPLING) {
                event.setCancelled(true);
                ChatUtil.sendError(event.getPlayer(), "You cannot drop that here.");
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

            generalMap.addItem(new BlockRecord(block));
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
    public void onItemConsume(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();
        if (noTeeth.contains(player) && event.getItem().getTypeId() != ItemID.POTION) {
            ChatUtil.sendWarning(player, "You find it impossible to eat that without any teeth.");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {

        Entity ent = event.getEntity();

        if (ent instanceof Slime && contains(ent)) {

            Iterator<ItemStack> dropIterator = event.getDrops().iterator();
            while (dropIterator.hasNext()) {
                ItemStack next = dropIterator.next();
                if (next != null && next.getTypeId() == ItemID.SLIME_BALL) dropIterator.remove();
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onEggHatch(EggHatchEvent event) {

        if (contains(event.getEgg())) event.setCancelled(true);
    }

    @Override
    public void writeData(boolean doAsync) {

        Runnable run = () -> {
            IOUtil.toBinaryFile(getWorkingDir(), "trees", treeMap);
            IOUtil.toBinaryFile(getWorkingDir(), "general", generalMap);
        };

        if (doAsync) {
            server.getScheduler().runTaskAsynchronously(inst, run);
        } else {
            run.run();
        }
    }

    @Override
    public void reloadData() {

        File treeFile = new File(getWorkingDir().getPath() + "/trees.dat");
        File generalFile = new File(getWorkingDir().getPath() + "/general.dat");

        if (treeFile.exists()) {
            Object treeFileO = IOUtil.readBinaryFile(treeFile);

            if (treeFileO instanceof BaseBlockRecordIndex) {
                treeMap = (BaseBlockRecordIndex) treeFileO;
                log.info("Loaded: " + treeMap.size() + " tree records for: " + getId() + ".");
            } else {
                log.warning("Invalid block record file encountered: " + treeFile.getName() + "!");
                log.warning("Attempting to use backup file...");

                treeFile = new File(getWorkingDir().getPath() + "/old-" + treeFile.getName());

                if (treeFile.exists()) {

                    treeFileO = IOUtil.readBinaryFile(treeFile);

                    if (treeFileO instanceof BaseBlockRecordIndex) {
                        treeMap = (BaseBlockRecordIndex) treeFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + treeMap.size() + " tree records for: " + getId() + ".");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }

        if (generalFile.exists()) {
            Object generalFileO = IOUtil.readBinaryFile(generalFile);

            if (generalFileO instanceof BaseBlockRecordIndex) {
                generalMap = (BaseBlockRecordIndex) generalFileO;
                log.info("Loaded: " + generalMap.size() + " general records for: " + getId() + ".");
            } else {
                log.warning("Invalid block record file encountered: " + generalFile.getName() + "!");
                log.warning("Attempting to use backup file...");

                generalFile = new File(getWorkingDir().getPath() + "/old-" + generalFile.getName());

                if (generalFile.exists()) {

                    generalFileO = IOUtil.readBinaryFile(generalFile);

                    if (generalFileO instanceof BaseBlockRecordIndex) {
                        generalMap = (BaseBlockRecordIndex) generalFileO;
                        log.info("Backup file loaded successfully!");
                        log.info("Loaded: " + generalMap.size() + " general records for: " + getId() + ".");
                    } else {
                        log.warning("Backup file failed to load!");
                    }
                }
            }
        }
    }
}
