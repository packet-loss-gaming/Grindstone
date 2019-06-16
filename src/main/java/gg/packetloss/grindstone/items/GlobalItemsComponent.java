/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.anticheat.AntiCheatCompatibilityComponent;
import gg.packetloss.grindstone.city.engine.combat.PvPComponent;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.implementations.*;
import gg.packetloss.grindstone.items.migration.MigrationManager;
import gg.packetloss.grindstone.items.migration.migrations.FearSwordMigration;
import gg.packetloss.grindstone.items.migration.migrations.UnleashedSwordMigration;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ItemCondenser;
import gg.packetloss.grindstone.util.item.InventoryUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;


@ComponentInformation(friendlyName = "Global Items Component", desc = "Global Custom Item effects")
@Depend(components = {SessionComponent.class, AdminComponent.class,
        AntiCheatCompatibilityComponent.class, PvPComponent.class,
        PrayerComponent.class
})
public class GlobalItemsComponent extends BukkitComponent implements Listener {

    private static final CommandBook inst = CommandBook.inst();
    private static final Server server = CommandBook.server();

    @InjectComponent
    protected static AdminComponent admin;
    @InjectComponent
    protected static SessionComponent sessions;
    @InjectComponent
    protected static PrayerComponent prayers;

    private static ItemCondenser goldCondenser = new ItemCondenser();

    static {
        goldCondenser.addSupport(new ItemStack(ItemID.GOLD_NUGGET, 9), new ItemStack(ItemID.GOLD_BAR, 1));
        goldCondenser.addSupport(new ItemStack(ItemID.GOLD_BAR, 9), new ItemStack(BlockID.GOLD_BLOCK, 1));
    }

    private static ItemCondenser summationCondenser = new ItemCondenser();

    static {
        // Coal
        summationCondenser.addSupport(new ItemStack(ItemID.COAL, 9), new ItemStack(BlockID.COAL_BLOCK, 1));

        // Iron
        summationCondenser.addSupport(new ItemStack(Material.IRON_NUGGET, 9), new ItemStack(Material.IRON_INGOT, 1));
        summationCondenser.addSupport(new ItemStack(ItemID.IRON_BAR, 9), new ItemStack(BlockID.IRON_BLOCK, 1));

        // Gold
        summationCondenser.addSupport(new ItemStack(ItemID.GOLD_NUGGET, 9), new ItemStack(ItemID.GOLD_BAR, 1));
        summationCondenser.addSupport(new ItemStack(ItemID.GOLD_BAR, 9), new ItemStack(BlockID.GOLD_BLOCK, 1));

        // Redstone
        summationCondenser.addSupport(new ItemStack(ItemID.REDSTONE_DUST, 9), new ItemStack(BlockID.REDSTONE_BLOCK, 1));

        // Lapis
        summationCondenser.addSupport(new ItemStack(ItemID.INK_SACK, 9, (byte) 4), new ItemStack(BlockID.LAPIS_LAZULI_BLOCK, 1));

        // Diamond
        summationCondenser.addSupport(new ItemStack(ItemID.DIAMOND, 9), new ItemStack(BlockID.DIAMOND_BLOCK, 1));

        // Emerald
        summationCondenser.addSupport(new ItemStack(ItemID.EMERALD, 9), new ItemStack(BlockID.EMERALD_BLOCK, 1));

        // Slime
        summationCondenser.addSupport(new ItemStack(Material.SLIME_BALL, 9), new ItemStack(Material.SLIME_BLOCK, 1));
    }

    private MigrationManager migrationManager = new MigrationManager();

    private JsonObject buildItemManifest() {
        JsonArray itemNames = new JsonArray();
        for (CustomItems item : CustomItems.values()) {
            itemNames.add(new JsonPrimitive(item.getSnakecaseName()));
        }
        JsonObject manifest = new JsonObject();
        manifest.add("names", itemNames);
        return manifest;
    }

    private void writeItemManifest() {
        String manifestFile = inst.getDataFolder().getPath() + "/item_manifest.json";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(manifestFile))) {
            writer.write(new Gson().toJson(buildItemManifest()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        loadResources();
        registerSpecWeapons();
        registerHymns();
        registerGeneral();
        registerMigrations();

        writeItemManifest();
    }

    private <T extends Listener> T handle(T component) {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(component);
        return component;
    }

    private void loadResources() {
        AbstractItemFeatureImpl.applyResource(admin);
        AbstractItemFeatureImpl.applyResource(sessions);
        AbstractItemFeatureImpl.applyResource(prayers);
    }

    private void registerSpecWeapons() {
        WeaponSysImpl wepSys = handle(new WeaponSysImpl());
        wepSys.add(WeaponType.RANGED, CustomItems.FEAR_BOW, handle(new FearBowImpl()));
        wepSys.add(WeaponType.RANGED, CustomItems.UNLEASHED_BOW, handle(new UnleashedBowImpl()));

        wepSys.add(WeaponType.MELEE, CustomItems.FEAR_SWORD, handle(new FearSwordImpl()));
        wepSys.add(WeaponType.MELEE, CustomItems.FEAR_SHORT_SWORD, handle(new FearSwordImpl()));
        wepSys.add(WeaponType.MELEE, CustomItems.UNLEASHED_SWORD, handle(new UnleashedSwordImpl()));
        wepSys.add(WeaponType.MELEE, CustomItems.UNLEASHED_SHORT_SWORD, handle(new UnleashedSwordImpl()));
    }

    private void registerHymns() {
        HymnImpl hymnImpl = handle(new HymnImpl());
        hymnImpl.addHymn(CustomItems.PHANTOM_HYMN, HymnSingEvent.Hymn.PHANTOM);
        hymnImpl.addHymn(CustomItems.CHICKEN_HYMN, HymnSingEvent.Hymn.CHICKEN);
        hymnImpl.addHymn(CustomItems.HYMN_OF_SUMMATION, HymnSingEvent.Hymn.SUMMATION);

        handle(new ChickenHymnImpl());
        handle(new SummationHymnImpl(summationCondenser));
    }

    private void registerGeneral() {
        handle(new AncientArmorImpl());
        handle(new AncientCrownImpl(goldCondenser));
        handle(new BatBowImpl());
        handle(new ChickenBowImpl());
        handle(new GodFishImpl());
        handle(new ImbuedCrystalImpl(goldCondenser));
        handle(new MadMilkImpl());
        handle(new MagicBucketImpl());
        handle(new MasterBowImpl());
        handle(new NecrosArmorImpl());
        handle(new NectricArmorImpl());
        handle(new PixieDustImpl());
        handle(new PotionOfRestitutionImpl());
        handle(new RedFeatherImpl());
        handle(new SummationScrollImpl(summationCondenser));
    }

    private void registerMigrations() {
        migrationManager.add(new FearSwordMigration());
        migrationManager.add(new UnleashedSwordMigration());

        registerCommands(MigrationCommands.class);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        InventoryType type = event.getInventory().getType();
        InventoryAction action = event.getAction();

        if (type.equals(InventoryType.ANVIL)) {
            if (action.equals(InventoryAction.NOTHING)) return;
            if (InventoryUtil.getMoveClicks().contains(event.getClick())) {
                event.setResult(Event.Result.DENY);
                ChatUtil.sendError(player, "You cannot move that here.");
                return;
            }

            int rawSlot = event.getRawSlot();

            if (rawSlot < 2) {
                if (InventoryUtil.getPlaceActions().contains(action) && ItemUtil.isNamed(cursorItem)) {
                    boolean isCustomItem = ItemUtil.isAuthenticCustomItem(cursorItem.getItemMeta().getDisplayName());

                    if (!isCustomItem) return;

                    event.setResult(Event.Result.DENY);
                    ChatUtil.sendError(player, "You cannot place that here.");
                }
            } else if (rawSlot == 2) {
                if (InventoryUtil.getPickUpActions().contains(action) && ItemUtil.isNamed(currentItem)) {
                    boolean isCustomItem = ItemUtil.isAuthenticCustomItem(currentItem.getItemMeta().getDisplayName());

                    if (!isCustomItem) return;

                    event.setResult(Event.Result.DENY);
                    ChatUtil.sendError(player, "You cannot name this item that name.");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getInventory().getType().equals(InventoryType.ANVIL)) {

            for (int i : event.getRawSlots()) {
                if (i + 1 <= event.getInventory().getSize()) {
                    event.setResult(Event.Result.DENY);
                    return;
                }
            }
        }
    }

    public class MigrationCommands {
        @Command(aliases = {"migrateitem"}, desc = "Updates a custom item with a configurable update path")
        public void migrateItemCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            PlayerInventory inventory = player.getInventory();
            ItemStack heldItem = inventory.getItemInMainHand();

            if (args.argsLength() == 0) {
                Set<String> upgradeOptions = migrationManager.getUpgradeOptions(heldItem);
                if (upgradeOptions.isEmpty()) {
                    throw new CommandException("This item cannot be upgraded currently.");
                }

                ChatUtil.sendNotice(player, ChatColor.GOLD + "Available upgrade options:");
                for (String upgradeOption : upgradeOptions) {
                    ChatUtil.sendNotice(player, " - " + ChatColor.BLUE + upgradeOption.toUpperCase());
                }

                return;
            }

            String selectedUpgrade = args.getRemainingString(0);
            Optional<ItemStack> optNewItem = migrationManager.upgradeItem(heldItem, selectedUpgrade);
            if (optNewItem.isEmpty()) {
                throw new CommandException("Invalid upgrade specified or no upgrades currently available.");
            }

            inventory.setItemInMainHand(optNewItem.get());
        }
    }
}
