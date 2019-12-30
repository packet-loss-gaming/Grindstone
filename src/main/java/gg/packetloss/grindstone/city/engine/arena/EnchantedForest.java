/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.grindstone.EggComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.egg.EggHatchEvent;
import gg.packetloss.grindstone.exceptions.UnstorableBlockStateException;
import gg.packetloss.grindstone.prayer.PrayerFX.ButterFingersFX;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.state.block.BlockStateComponent;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import gg.packetloss.grindstone.util.timer.IntegratedRunnable;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.ChatUtil.loonizeWord;

public class EnchantedForest extends AbstractRegionedArena implements MonitoredArena, Listener {
    private static final long TREE_RESTORE_DELAY = TimeUnit.HOURS.toMillis(3);
    private static final long SHRUBBERY_RESTORE_DELAY = TimeUnit.MINUTES.toMillis(8);

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;
    private EggComponent eggComponent;
    private BlockStateComponent blockStateComponent;

    private final Random random = new Random();
    private Set<Player> noTeeth = new HashSet<>();

    public EnchantedForest(World world, ProtectedRegion region, AdminComponent adminComponent,
                           EggComponent eggComponent, BlockStateComponent blockStateComponent) {

        super(world, region);
        this.adminComponent = adminComponent;
        this.eggComponent = eggComponent;
        this.blockStateComponent = blockStateComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void forceRestoreBlocks() {
        blockStateComponent.popAllBlocks(BlockStateKind.ENCHANTED_FOREST_TREES);
        blockStateComponent.popAllBlocks(BlockStateKind.ENCHANTED_FOREST_SHRUBBERY);
    }

    @Override
    public void run() {
        equalize();
        restoreBlocks();

        killKillerRabbits();
    }

    @Override
    public void disable() {

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
        blockStateComponent.popBlocksOlderThan(BlockStateKind.ENCHANTED_FOREST_TREES, TREE_RESTORE_DELAY);
        blockStateComponent.popBlocksOlderThan(BlockStateKind.ENCHANTED_FOREST_SHRUBBERY, SHRUBBERY_RESTORE_DELAY);
    }

    public void killKillerRabbits() {
        for (Rabbit rabbit : getContained(Rabbit.class)) {
            if (rabbit.getRabbitType() != Rabbit.Type.THE_KILLER_BUNNY) {
                continue;
            }

            if (rabbit.getLocation().add(0, -1, 0).getBlock().getType() == Material.GRASS) {
                continue;
            }

            rabbit.setHealth(0);
            SingleBlockParticleEffect.burstOfFlames(rabbit.getLocation());
        }
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
                    switch (pInv.getItemInHand().getType()) {
                        case DIAMOND_AXE:
                            pInv.addItem(new ItemStack(Material.DIAMOND, 2), new ItemStack(Material.STICK, 2));
                            break;
                        case GOLDEN_AXE:
                            pInv.addItem(new ItemStack(Material.GOLD_INGOT, 2), new ItemStack(Material.STICK, 2));
                            break;
                        case IRON_AXE:
                            pInv.addItem(new ItemStack(Material.IRON_INGOT, 2), new ItemStack(Material.STICK, 2));
                            break;
                        case WOODEN_AXE:
                            pInv.addItem(new ItemStack(Material.OAK_WOOD, 2), new ItemStack(Material.STICK, 2));
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

        if (adminComponent.isAdmin(player) || !contains(block)) {
            return;
        }

        if (EnvironmentUtil.isLog(block)) {
            short c = 0;
            for (ItemStack aItemStack : getRandomDropSet(player)) {
                if (c >= 3) break;
                getWorld().dropItemNaturally(block.getLocation(), aItemStack);
                c++;
            }

            event.setExpToDrop(ChanceUtil.getRandom(4));
            eatFood(player);
            trick(player);

            try {
                blockStateComponent.pushBlock(BlockStateKind.ENCHANTED_FOREST_TREES, player, block.getState());
                return;
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();
            }
        } else if (EnvironmentUtil.isShrubBlock(block)) {
            if (ChanceUtil.getChance(450)) {
                Rabbit rabbit = (Rabbit) getWorld().spawnEntity(block.getLocation(), EntityType.RABBIT);
                player.chat("Awwww a cute little bunny!");

                if (ChanceUtil.getChance(5)) {
                    rabbit.setRabbitType(Rabbit.Type.THE_KILLER_BUNNY);
                    rabbit.setTarget(player);
                    rabbit.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(50);

                    server.getScheduler().runTaskLater(inst, () -> {
                        player.chat("OH " + loonizeWord("four") + ChatColor.WHITE + "!!!");
                    }, 10);
                }
            }

            if (EnvironmentUtil.isDayTime(getWorld().getTime())) {
                if (!eggComponent.isEasterActive(player) && ChanceUtil.getChance(30)) {
                    eggComponent.dropEggs(EggComponent.EggType.EASTER, block.getLocation());
                }
            } else {
                if (!eggComponent.isHalloweenActive(player) && ChanceUtil.getChance(45)) {
                    eggComponent.dropEggs(EggComponent.EggType.HALLOWEEN, block.getLocation());
                }
            }

            try {
                blockStateComponent.pushBlock(BlockStateKind.ENCHANTED_FOREST_SHRUBBERY, player, block.getState());
                return;
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();
            }
        }

        event.setCancelled(true);
        ChatUtil.sendWarning(player, "You cannot break this block for some reason.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        if (contains(event.getBlock())) {
            Block block = event.getBlock();

            try {
                blockStateComponent.pushAnonymousBlock(BlockStateKind.ENCHANTED_FOREST_TREES, block.getState());

                if (!ChanceUtil.getChance(14)) return;
                getWorld().dropItemNaturally(block.getLocation(), getRandomDropSet(server.getConsoleSender()).get(0));
            } catch (UnstorableBlockStateException e) {
                e.printStackTrace();

                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(ItemSpawnEvent event) {

        Item item = event.getEntity();
        if (contains(item)) {
            Material type = item.getItemStack().getType();
            if (EnvironmentUtil.isLog(type) || EnvironmentUtil.isSapling(type)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        Item item = event.getItemDrop();
        if (contains(item)) {
            Material type = item.getItemStack().getType();
            if (EnvironmentUtil.isLog(type) || EnvironmentUtil.isSapling(type)) {
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
            Player player = event.getPlayer();
            Block block = event.getBlockClicked();

            try {
                blockStateComponent.pushBlock(BlockStateKind.ENCHANTED_FOREST_FLUIDS, player, block.getState());
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
    public void onItemConsume(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();
        if (noTeeth.contains(player) && event.getItem().getType() != Material.POTION) {
            ChatUtil.sendWarning(player, "You find it impossible to eat that without any teeth.");
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEggHatch(EggHatchEvent event) {

        if (contains(event.getEgg())) event.setCancelled(true);
    }
}
