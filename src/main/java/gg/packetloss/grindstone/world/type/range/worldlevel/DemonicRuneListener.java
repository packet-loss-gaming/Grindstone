/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.sacrifice.SacrificeInformation;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

class DemonicRuneListener implements Listener {
    private final WorldLevelComponent parent;

    public DemonicRuneListener(WorldLevelComponent parent) {
        this.parent = parent;
    }

    public static class DemonicRuneState {
        private final int worldTier;
        private final int monsterTier;
        private final int combatTier;

        private DemonicRuneState(int worldTier, int monsterTier, int combatTier) {
            this.worldTier = worldTier;
            this.monsterTier = monsterTier;
            this.combatTier = combatTier;
        }

        private DemonicRuneState() {
            this(-1, -1, -1);
        }

        public static DemonicRuneState fromMonsterKill(int worldTier, double monsterTypeModifier, double percentDamageDone) {
            return new DemonicRuneState(
                worldTier,
                (int) (monsterTypeModifier * 100),
                (int) (percentDamageDone * 100)
            );
        }

        public int getWorldLevel() {
            return worldTier;
        }

        public double getTypeModifier() {
            return monsterTier / 100D;
        }

        public double getPercentDamageDone() {
            return combatTier / 100D;
        }
    }

    public static void setRuneState(ItemStack itemStack, DemonicRuneState runeState) {
        List<Map.Entry<String, String>> worldTier = new ArrayList<>();
        worldTier.add(new AbstractMap.SimpleEntry<>("World Tier", RomanNumeralUtil.toRoman(runeState.worldTier)));

        List<Map.Entry<String, String>> monsterTier = new ArrayList<>();
        monsterTier.add(new AbstractMap.SimpleEntry<>("Monster Tier", RomanNumeralUtil.toRoman(runeState.monsterTier)));

        List<Map.Entry<String, String>> combatTier = new ArrayList<>();
        combatTier.add(new AbstractMap.SimpleEntry<>("Combat Tier", RomanNumeralUtil.toRoman(runeState.combatTier)));

        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setLore(List.of(
            ItemUtil.saveLoreKeyValues(worldTier),
            ItemUtil.saveLoreKeyValues(monsterTier),
            ItemUtil.saveLoreKeyValues(combatTier)
        ));
        itemStack.setItemMeta(itemMeta);
    }

    public static DemonicRuneState getRuneState(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        List<String> lore = itemMeta.getLore();
        if (lore == null || lore.isEmpty()) {
            return new DemonicRuneState();
        }

        List<Map.Entry<String, String>> worldTierValues = ItemUtil.loadLoreKeyValues(lore.get(0));
        String worldTierStr = worldTierValues.get(0).getValue();
        if (worldTierStr == null) {
            return new DemonicRuneState();
        }

        List<Map.Entry<String, String>> monsterTierValues = ItemUtil.loadLoreKeyValues(lore.get(1));
        String monsterTierStr = monsterTierValues.get(0).getValue();
        if (monsterTierStr == null) {
            return new DemonicRuneState();
        }

        List<Map.Entry<String, String>> combatTierValues = ItemUtil.loadLoreKeyValues(lore.get(2));
        String combatTierStr = combatTierValues.get(0).getValue();
        if (combatTierStr == null) {
            return new DemonicRuneState();
        }

        int worldTier = RomanNumeralUtil.fromRoman(worldTierStr);
        int monsterTier = RomanNumeralUtil.fromRoman(monsterTierStr);
        int combatTier = RomanNumeralUtil.fromRoman(combatTierStr);

        return new DemonicRuneState(worldTier, monsterTier, combatTier);
    }

    private class DemonicPortalEntry {
        private final ArrayDeque<ItemStack> lootBuffer;

        private final UUID playerID;
        private final DemonicRuneState demonicRune;

        private int numberOfRunes;

        private DemonicPortalEntry(UUID playerID, DemonicRuneState demonicRune, int numberOfRunes) {
            this.playerID = playerID;
            this.demonicRune = demonicRune;
            this.numberOfRunes = numberOfRunes;

            // Create a memory optimized array deque
            this.lootBuffer = new ArrayDeque<>(getDropCountModifier());
        }

        private int getDropCountModifier() {
            int level = demonicRune.getWorldLevel();
            double typeModifier = demonicRune.getTypeModifier();
            double percentDamageDone = demonicRune.getPercentDamageDone();

            return parent.getDropCountModifier(level, typeModifier, percentDamageDone);
        }

        private double getDropValueModifier() {
            int level = demonicRune.getWorldLevel();
            double typeModifier = demonicRune.getTypeModifier();
            double percentDamageDone = demonicRune.getPercentDamageDone();

            return parent.getDropValueModifier(level, typeModifier, percentDamageDone);
        }

        private void fillBuffer() {
            Validate.isTrue(numberOfRunes > 0);

            // Handle sacrificial pit generated drops
            SacrificeInformation sacrificeInfo = new SacrificeInformation(
                CommandBook.server().getConsoleSender(),
                getDropCountModifier(),
                getDropValueModifier() * parent.getConfig().mobsDropTableSacrificeValue
            );

            // Populate the loot buffer
            lootBuffer.addAll(SacrificeComponent.getCalculatedLoot(sacrificeInfo).getItemStacks());

            // The system always expects something to be added after a rune is consumed. If nothing else
            // was added, add some gold nuggets.
            if (lootBuffer.isEmpty()) {
                lootBuffer.add(new ItemStack(Material.GOLD_NUGGET, ChanceUtil.getRandom(64)));
            }

            // Decrement the number of runes remaining
            --numberOfRunes;
        }

        public UUID getPlayerID() {
            return playerID;
        }

        /**
         * Converts the remaining demonic rune state into an ItemStack in the buffer for early shutdown.
         */
        public void addDemonicRuneToBuffer() {
            if (numberOfRunes == 0) {
                return;
            }

            // Build the stack
            ItemStack demonicRuneStack = CustomItemCenter.build(CustomItems.DEMONIC_RUNE);
            setRuneState(demonicRuneStack, demonicRune);
            demonicRuneStack.setAmount(numberOfRunes);

            // Clear the rune count so isEmpty() represents only the buffer.
            numberOfRunes = 0;

            lootBuffer.add(demonicRuneStack);
        }

        public ItemStack poll() {
            if (lootBuffer.isEmpty()) {
                fillBuffer();
            }

            return lootBuffer.poll();
        }

        public boolean isEmpty() {
            return lootBuffer.isEmpty() && numberOfRunes == 0;
        }
    }

    private final List<DemonicPortal> openPortals = new ArrayList<>();

    public void finishPortalsNow() {
        for (DemonicPortal openPortal : openPortals) {
            openPortal.finishNow();
        }
    }

    private class DemonicPortal {
        // Controls particle density, lower values are more particles
        private static final double PARTICLE_LOOP_INTERVAL = 0.05;

        private final Location portalLocation;
        private final List<DemonicPortalEntry> portalEntries = new ArrayList<>();

        private DemonicPortal(Location portalLocation) {
            this.portalLocation = portalLocation;

            spawn();
        }

        private void spawnEntryItem(DemonicPortalEntry entry) {
            EntityUtil.spawnProtectedItem(entry.poll(), entry.getPlayerID(), portalLocation);
        }

        public void finishNow() {
            for (DemonicPortalEntry entry : portalEntries) {
                // Add whatever remains of the initial rune to the buffer
                entry.addDemonicRuneToBuffer();
                // Drop everything immediately
                // (should be a max of config.mobsDropTableItemCountMax + 1)
                while (!entry.isEmpty()) {
                    spawnEntryItem(entry);
                }
            }
            portalEntries.clear();
        }

        private void spawn() {
            openPortals.add(this);

            TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

            taskBuilder.setAction((times) -> {
                // Do nothing if the chunk isn't loaded
                if (!LocationUtil.isChunkLoadedAt(portalLocation)) {
                    return false;
                }

                double radiusX = ChanceUtil.getRangedRandom(.1, 1.5);
                double radiusZ = ChanceUtil.getRangedRandom(.1, 1.5);

                for (double progress = Math.PI * 2; progress > 0; progress -= PARTICLE_LOOP_INTERVAL) {
                    double animationX = radiusX * Math.cos(progress);
                    double animationZ = radiusZ * Math.sin(progress);

                    portalLocation.getWorld().spawnParticle(
                        Particle.ENCHANTMENT_TABLE,
                        portalLocation.getX() + animationX,
                        portalLocation.getY() + .5,
                        portalLocation.getZ() + animationZ,
                        0
                    );
                }

                if (ChanceUtil.getChance(5) && areaHasRoomForMoreItems()) {
                    DemonicPortalEntry entry = CollectionUtil.getElement(portalEntries);
                    spawnEntryItem(entry);
                    if (entry.isEmpty()) {
                        portalEntries.remove(entry);
                    }
                }

                if (portalEntries.isEmpty()) {
                    // Must remove here, rather than in a finish action as the finish action will be delayed
                    // and it would be possible for something to be added while we're shutting this portal down.
                    openPortals.remove(this);
                    return true;
                }

                return false;
            });

            taskBuilder.build();
        }

        private boolean areaHasRoomForMoreItems() {
            return portalLocation.getNearbyEntitiesByType(Item.class, 3).stream().count() < 30;
        }

        public void addEntry(DemonicPortalEntry entry) {
            portalEntries.add(entry);
        }

        public Location getPortalLocation() {
            return portalLocation;
        }
    }

    private DemonicPortal getOrCreatePortal(Player player) {
        Location potentialPortalLoc = player.getLocation().add(0, 2, 0);
        for (DemonicPortal portal : openPortals) {
            if (LocationUtil.isWithinDistance(portal.getPortalLocation(), potentialPortalLoc, 5)) {
                return portal;
            }
        }

        return new DemonicPortal(potentialPortalLoc);
    }

    private void giveItemsViaPortal(Player player, ItemStack demonicRuneStack) {
        DemonicPortal portal = getOrCreatePortal(player);

        DemonicRuneState runeState = getRuneState(demonicRuneStack);
        portal.addEntry(new DemonicPortalEntry(player.getUniqueId(), runeState, demonicRuneStack.getAmount()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSacrificeItemEvent(PlayerSacrificeItemEvent event) {
        ItemStack sacrificedItemStack = event.getItemStack();
        if (!ItemUtil.isItem(sacrificedItemStack, CustomItems.DEMONIC_RUNE)) {
            return;
        }

        // Clear the item
        event.setItemStack(null);

        Player player = event.getPlayer();
        giveItemsViaPortal(player, sacrificedItemStack);
    }

    private static final List<PlayerTeleportEvent.TeleportCause> IGNORED_CAUSES = List.of(
        PlayerTeleportEvent.TeleportCause.UNKNOWN,
        PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (IGNORED_CAUSES.contains(event.getCause())) {
            return;
        }

        Player player = event.getPlayer();
        if (!ItemUtil.hasItem(player, CustomItems.DEMONIC_RUNE)) {
            return;
        }

        ChatUtil.sendError(
            player,
            "Your ",
            ItemNameCalculator.getSystemDisplayName(CustomItems.DEMONIC_RUNE),
            "(s) encumber you."
        );
        ChatUtil.sendError(player, "The teleport fails.");
        event.setCancelled(true);
    }

    @EventHandler
    public void onHopper(InventoryMoveItemEvent event) {
        if (ItemUtil.isItem(event.getItem(), CustomItems.DEMONIC_RUNE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Block block = Objects.requireNonNull(event.getClickedBlock());
        if (EnvironmentUtil.isPortableContainer(block) && ItemUtil.hasItem(player, CustomItems.DEMONIC_RUNE)) {
            ChatUtil.sendError(
                player,
                "It probably wouldn't be a good idea to open that while you have ",
                ItemNameCalculator.getSystemDisplayName(CustomItems.DEMONIC_RUNE),
                "(s)!"
            );
            event.setCancelled(true);
        }
    }
}
