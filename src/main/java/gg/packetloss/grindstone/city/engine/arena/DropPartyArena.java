/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.SacrificeComponent;
import gg.packetloss.grindstone.events.entity.item.DropClearPulseEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.checker.RegionChecker;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.timer.CountdownTask;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DropPartyArena extends AbstractRegionedArena implements CommandTriggeredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private List<ItemStack> drops;
    private BukkitTask task = null;
    private long lastDropPulse = 0;
    private double dropPartyPulses = 0;

    public DropPartyArena(World world, ProtectedRegion region) {

        super(world, region);
        drops = new ArrayList<>();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        long ticks = TimeUtil.getTicksTill(20, 7);
        server.getScheduler().scheduleSyncRepeatingTask(CommandBook.inst(), this, ticks, 20 * 60 * 60 * 24 * 7);
    }

    @Override
    public void run() {

        drop(ChanceUtil.getRangedRandom(1460, 5836));
    }

    @Override
    public void disable() {

        // No disabling code
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {
        // Nothing to do
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.COMMAND_TRIGGERED;
    }

    private static final int DROP_PARTY_DELAY = 60;
    private static final Text DROP_PARTY_TEXT = Text.of(
            ChatColor.BLUE,
            "DROP PARTY",
            TextAction.Click.runCommand("/warp party-room"),
            TextAction.Hover.showText(Text.of("Click to teleport to the drop party"))
    );

    public void drop(int populatorValue) {
        drop(populatorValue, 24);
    }

    public void drop(int populatorValue, int modifier) {
        // Notify online players
        if (task == null) {
            CountdownTask dropPartyCountdown = new CountdownTask() {
                @Override
                public boolean matchesFilter(int seconds) {
                    return seconds > 0 && (seconds % 5 == 0 || seconds <= 10);
                }

                @Override
                public void performStep(int seconds) {
                    BaseComponent[] broadcastText = Text.of(
                            ChatColor.GOLD,
                            DROP_PARTY_TEXT,
                            " dropping items in ",
                            seconds,
                            " seconds!"
                    ).build();

                    Bukkit.broadcast(broadcastText);
                }

                @Override
                public void performFinal() {
                    BaseComponent[] broadcastText = Text.of(
                            ChatColor.GOLD,
                            DROP_PARTY_TEXT,
                            " now dropping items!!!"
                    ).build();

                    Bukkit.broadcast(broadcastText);
                }
            };

            TimedRunnable countdown = new TimedRunnable(dropPartyCountdown, DROP_PARTY_DELAY);
            BukkitTask task = server.getScheduler().runTaskTimer(inst, countdown, 0, 20);
            countdown.setTask(task);
        }

        // Setup region variable
        final CuboidRegion rg = new CuboidRegion(getRegion().getMinimumPoint(), getRegion().getMaximumPoint());

        // Use the SacrificeComponent to populate the drop party if a populator value is given
        final boolean populate = populatorValue > 0;
        if (populate) {
            int rawPlayerCount = server.getOnlinePlayers().size();

            int adjustedPlayerCount = Math.max(3, rawPlayerCount);
            for (int i = 0; i < adjustedPlayerCount * modifier; i++) {
                drops.add(CustomItemCenter.build(CustomItems.SCROLL_OF_SUMMATION));
                drops.add(CustomItemCenter.build(CustomItems.ODE_TO_THE_FROZEN_KING));
                drops.addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 64, populatorValue));
            }

            if (rawPlayerCount > 0) {
                drops.add(ItemUtil.makeSkull(CollectionUtil.getElement(server.getOnlinePlayers()).getName()));
            }
        }

        // Remove null drops and shuffle all other drops
        drops.removeAll(Collections.singleton(null));
        Collections.shuffle(drops);

        // Set a maximum drop count
        dropPartyPulses = drops.size() * .15;

        if (task != null) task.cancel();

        task = server.getScheduler().runTaskTimer(inst, () -> {
            if (lastDropPulse != 0 && System.currentTimeMillis() - lastDropPulse < TimeUnit.SECONDS.toMillis(3)) {
                return;
            }

            Iterator<ItemStack> it = drops.iterator();

            RegionChecker checker = new RegionChecker(rg);
            for (int k = 10; it.hasNext() && k > 0; k--) {

                // Pick a random Location
                Location l = LocationUtil.pickLocation(getWorld(), rg.getMaximumY(), checker);
                if (!getWorld().getChunkAt(l).isLoaded()) getWorld().getChunkAt(l).load(true);
                getWorld().dropItem(l, it.next());

                // Remove the drop
                it.remove();

                // Drop the xp
                if (populate) {
                    // Throw in some xp cause why not
                    for (int s = ChanceUtil.getRandom(5); s > 0; s--) {
                        ExperienceOrb e = getWorld().spawn(l, ExperienceOrb.class);
                        e.setExperience(8);
                    }
                }
            }

            // Cancel if we've ran out of drop party pulses or if there is nothing more to drop
            if (drops.size() < 1 || dropPartyPulses <= 0) {
                task.cancel();
                task = null;
            }

            dropPartyPulses--;
        }, 20 * DROP_PARTY_DELAY, 20 * 3);
    }

    @EventHandler
    public void onDropClearPulse(DropClearPulseEvent event) {

        if (task != null) {
            lastDropPulse = System.currentTimeMillis();
            ChatUtil.sendNotice(getContained(1, Player.class), "Drop Party temporarily suspended for: Drop Clear.");
            for (Entity entity : getContained(1, Item.class, ExperienceOrb.class)) {
                if (entity instanceof Item) {
                    drops.add(((Item) entity).getItemStack());
                }
                entity.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (task != null) {
            Player player = event.getPlayer();

            Text joinText = Text.of(
                    ChatColor.GOLD,
                    DROP_PARTY_TEXT,
                    " in progress!"
            );

            player.sendMessage(joinText.build());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Block block = event.getClickedBlock();

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && block.getTypeId() == BlockID.STONE_BUTTON
                && getRegion().getParent().contains(new Vector(block.getX(), block.getY(), block.getZ()).floor())) {

            Player player = event.getPlayer();

            if (task != null) {

                ChatUtil.sendError(player, "There is already a drop party in progress!");
                return;
            }

            // Scan for, and absorb chest contents
            Vector min = getRegion().getParent().getMinimumPoint();
            Vector max = getRegion().getParent().getMaximumPoint();

            int minX = min.getBlockX();
            int minY = min.getBlockY();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxY = max.getBlockY();
            int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        BlockState chest = getWorld().getBlockAt(x, y, z).getState();

                        if (chest instanceof Chest) {

                            Inventory chestInventory = ((Chest) chest).getBlockInventory();
                            Collections.addAll(drops, chestInventory.getContents());

                            chestInventory.clear();
                        }
                    }
                }
            }

            drop(0);
        }
    }
}
