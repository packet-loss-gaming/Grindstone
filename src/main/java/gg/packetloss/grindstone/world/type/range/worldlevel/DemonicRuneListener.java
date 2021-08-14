/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.range.worldlevel;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.events.PlayerSacrificeItemEvent;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.sacrifice.SacrificeComponent;
import gg.packetloss.grindstone.sacrifice.SacrificeInformation;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
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

    public static void setRuneTier(ItemStack itemStack, DemonicRuneState runeState) {
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

    public static DemonicRuneState getRuneWorldTier(ItemStack itemStack) {
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


    private void createDemonicPortal(Player player, ArrayDeque<ItemStack> loot) {
        Location lockedLocation = player.getLocation().add(0, 2, 0);

        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        final double loopInterval = 0.05; // controls particle density, lower values ar more particles

        taskBuilder.setNumberOfRuns(1);

        taskBuilder.setAction((times) -> {
            double radiusX = ChanceUtil.getRangedRandom(.1, 1.5);
            double radiusZ = ChanceUtil.getRangedRandom(.1, 1.5);

            for (double progress = Math.PI * 2; progress > 0; progress -= loopInterval) {
                double animationX = radiusX * Math.cos(progress);
                double animationZ = radiusZ * Math.sin(progress);

                lockedLocation.getWorld().spawnParticle(
                    Particle.ENCHANTMENT_TABLE,
                    lockedLocation.getX() + animationX,
                    lockedLocation.getY() + .5,
                    lockedLocation.getZ() + animationZ,
                    0
                );
            }

            if (ChanceUtil.getChance(5)) {
                ItemStack stack = loot.poll();
                EntityUtil.spawnProtectedItem(stack, player, lockedLocation);
            }

            return loot.isEmpty();
        });

        taskBuilder.build();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerSacrificeItemEvent(PlayerSacrificeItemEvent event) {
        ItemStack sacrificedItemStack = event.getItemStack();
        if (!ItemUtil.isItem(sacrificedItemStack, CustomItems.DEMONIC_RUNE)) {
            return;
        }

        // Clear the item
        event.setItemStack(null);

        DemonicRuneState runeState = getRuneWorldTier(sacrificedItemStack);

        int level = runeState.getWorldLevel();
        double typeModifier = runeState.getTypeModifier();
        double percentDamageDone = runeState.getPercentDamageDone();

        int dropCountModifier = parent.getDropCountModifier(level, typeModifier, percentDamageDone);
        double dropValueModifier = parent.getDropValueModifier(level, typeModifier, percentDamageDone);

        // Handle sacrificial pit generated drops
        SacrificeInformation sacrificeInfo = new SacrificeInformation(
            CommandBook.server().getConsoleSender(),
            dropCountModifier,
            dropValueModifier * parent.getConfig().mobsDropTableSacrificeValue
        );

        ArrayDeque<ItemStack> loot = new ArrayDeque<>();
        for (int i = 0; i < sacrificedItemStack.getAmount(); ++i) {
            loot.addAll(SacrificeComponent.getCalculatedLoot(sacrificeInfo).getItemStacks());
        }

        Player player = event.getPlayer();
        createDemonicPortal(player, loot);
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
        }
    }
}
