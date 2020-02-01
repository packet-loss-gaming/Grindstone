/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.events.entity.item.DropClearPulseEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.custom.ItemFamily;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.checker.RegionChecker;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.timer.CountdownTask;
import gg.packetloss.grindstone.util.timer.TimedRunnable;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DropPartyArena extends AbstractRegionedArena implements CommandTriggeredArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private Deque<ItemStack> drops;
    private BukkitTask task = null;
    private long lastDropPulse = 0;
    private long dropPartyStart = 0;

    public DropPartyArena(World world, ProtectedRegion region) {

        super(world, region);
        drops = new ArrayDeque<>();

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

    private static final List<CustomItems> PARTY_BOX_OPTIONS = List.of(
            CustomItems.WHITE_PARTY_BOX,
            CustomItems.ORANGE_PARTY_BOX,
            CustomItems.MAGENTA_PARTY_BOX,
            CustomItems.LIGHT_BLUE_PARTY_BOX,
            CustomItems.YELLOW_PARTY_BOX,
            CustomItems.LIME_PARTY_BOX,
            CustomItems.PINK_PARTY_BOX,
            CustomItems.GRAY_PARTY_BOX,
            CustomItems.LIGHT_GRAY_PARTY_BOX,
            CustomItems.CYAN_PARTY_BOX,
            CustomItems.PURPLE_PARTY_BOX,
            CustomItems.BLUE_PARTY_BOX,
            CustomItems.BROWN_PARTY_BOX,
            CustomItems.GREEN_PARTY_BOX,
            CustomItems.RED_PARTY_BOX,
            CustomItems.BLACK_PARTY_BOX
    );

    private void dropPartyBox(Location location, ItemStack item) {
        ItemStack partyBox = CustomItemCenter.build(CollectionUtil.getElement(PARTY_BOX_OPTIONS));

        BlockStateMeta itemMeta = (BlockStateMeta) partyBox.getItemMeta();

        ShulkerBox shulkerBox = (ShulkerBox) itemMeta.getBlockState();
        shulkerBox.getInventory().addItem(item);
        itemMeta.setBlockState(shulkerBox);

        partyBox.setItemMeta(itemMeta);

        location.getWorld().dropItem(location, partyBox);
    }

    private void openPartyBox(Location location, ItemStack partyBox) {
        BlockStateMeta itemMeta = (BlockStateMeta) partyBox.getItemMeta();

        ShulkerBox shulkerBox = (ShulkerBox) itemMeta.getBlockState();
        shulkerBox.getInventory().forEach((item) -> {
            location.getWorld().dropItem(location, item);
        });
    }

    private static FireworkEffect fireworkEffect;

    static {
        FireworkEffect.Builder builder = FireworkEffect.builder();
        builder.flicker(true);
        builder.trail(true);
        builder.withColor(Color.GREEN, Color.BLUE, Color.RED);
        builder.with(FireworkEffect.Type.BALL);

        fireworkEffect = builder.build();
    }

    private List<Firework> taskedFirework = new ArrayList<>();

    private void createFireworkExplosion(Location l) {
        Firework firework = l.getWorld().spawn(l, Firework.class);

        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(fireworkEffect);
        meta.setPower(0);
        firework.setFireworkMeta(meta);

        taskedFirework.add(firework);

        server.getScheduler().runTaskLater(inst, firework::detonate, 2);
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
        List<ItemStack> newDrops = new ArrayList<>();
        final boolean populate = populatorValue > 0;
        if (populate) {
            for (int k = 0; k < server.getMaxPlayers() * modifier; k++) {
                for (int i = 10; i > 0; --i) {
                    newDrops.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 5));
                }

                newDrops.add(CustomItemCenter.build(CustomItems.SCROLL_OF_SUMMATION));
                newDrops.add(CustomItemCenter.build(CustomItems.ODE_TO_THE_FROZEN_KING));
                newDrops.addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 64, populatorValue));
            }

            Collection<? extends Player> onlinePlayers = server.getOnlinePlayers();
            if (!onlinePlayers.isEmpty()) {
                newDrops.add(ItemUtil.makeSkull(CollectionUtil.getElement(onlinePlayers)));
            }
        }

        // Remove null drops and shuffle all other drops
        newDrops.removeAll(Collections.singleton(null));
        Collections.shuffle(newDrops);

        // Add new drops to the drop queue
        drops.addAll(newDrops);
        dropPartyStart = System.currentTimeMillis();

        if (task != null) task.cancel();

        RegionChecker checker = new RegionChecker(rg);
        task = server.getScheduler().runTaskTimer(inst, () -> {
            if (lastDropPulse != 0 && System.currentTimeMillis() - lastDropPulse < TimeUnit.SECONDS.toMillis(3)) {
                return;
            }

            for (int i = 3 + (getContained(1, Player.class).size() * 2); i > 0; --i) {
                // Pick a random Location
                Location l = LocationUtil.pickLocation(getWorld(), rg.getMaximumY(), checker);
                if (!LocationUtil.isChunkLoadedAt(l)) {
                    break;
                }

                createFireworkExplosion(l);
            }

            // Cancel if there is nothing more to drop or the drop party is automatic and has been running too long
            long runningTime = System.currentTimeMillis() - dropPartyStart;
            boolean runningTooLong = populate && runningTime > TimeUnit.MINUTES.toMillis(10);
            if (drops.size() < 1 || runningTooLong) {
                if (populate) {
                    drops.clear();
                }

                task.cancel();
                task = null;
            }
        }, 20 * DROP_PARTY_DELAY, 30);
    }

    public void drop(int populatorValue) {
        drop(populatorValue, 24);
    }

    @EventHandler
    public void onFireworkTask(FireworkExplodeEvent event) {
        Firework firework = event.getEntity();
        if (!taskedFirework.remove(firework)) {
            return;
        }

        if (ChanceUtil.getChance(5)) {
            ItemStack drop = drops.poll();
            if (drop != null) {
                if (drop.getType() == Material.EXPERIENCE_BOTTLE) {
                    for (int i = drop.getAmount(); i > 0; --i) {
                        firework.getLocation().getWorld().spawn(firework.getLocation(), ThrownExpBottle.class);
                    }

                    return;
                }

                dropPartyBox(firework.getLocation(), drop);
                return;
            }
        }

        dropPartyBox(firework.getLocation(), new ItemStack(Material.GOLD_NUGGET, ChanceUtil.getRandom(18)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        Item item = event.getItem();

        if (ItemUtil.isInItemFamily(item.getItemStack(), ItemFamily.PARTY_BOX)) {
            // Override item pickup
            event.setCancelled(true);
            item.remove();

            ItemStack stack = item.getItemStack();
            openPartyBox(item.getLocation(), stack);
        }
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

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && block.getType() == Material.STONE_BUTTON
                && getRegion().getParent().contains(WorldEditBridge.toBlockVec3(block))) {

            Player player = event.getPlayer();

            if (task != null) {

                ChatUtil.sendError(player, "There is already a drop party in progress!");
                return;
            }

            // Scan for, and absorb chest contents
            RegionWalker.walk(getRegion().getParent(), (x, y, z) -> {
                BlockState chest = getWorld().getBlockAt(x, y, z).getState();

                if (chest instanceof Chest) {

                    Inventory chestInventory = ((Chest) chest).getBlockInventory();
                    Collections.addAll(drops, chestInventory.getContents());

                    chestInventory.clear();
                }
            });

            drop(0);
        }
    }
}
