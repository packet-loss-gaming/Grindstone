/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.items;

import com.destroystokyo.paper.event.inventory.PrepareResultEvent;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.PacketInterceptionComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.events.custom.item.HymnSingEvent;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.items.flight.FlightItemsComponent;
import gg.packetloss.grindstone.items.generic.AbstractItemFeatureImpl;
import gg.packetloss.grindstone.items.implementations.*;
import gg.packetloss.grindstone.items.implementations.combotools.*;
import gg.packetloss.grindstone.items.legacymigration.MigrationManager;
import gg.packetloss.grindstone.items.legacymigration.migrations.FearSwordMigration;
import gg.packetloss.grindstone.items.legacymigration.migrations.GodSwordMigration;
import gg.packetloss.grindstone.items.legacymigration.migrations.MasterSwordMigration;
import gg.packetloss.grindstone.items.legacymigration.migrations.UnleashedSwordMigration;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.ItemCondenser;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.world.managed.ManagedWorldComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;
import java.util.Set;


@ComponentInformation(friendlyName = "Global Items Component", desc = "Global Custom Item effects")
@Depend(components = {
        SessionComponent.class, AdminComponent.class, PacketInterceptionComponent.class, PrayerComponent.class,
        FlightItemsComponent.class, ManagedWorldComponent.class
})
public class GlobalItemsComponent extends BukkitComponent implements Listener {

    private static final CommandBook inst = CommandBook.inst();
    private static final Server server = CommandBook.server();

    @InjectComponent
    protected static AdminComponent admin;
    @InjectComponent
    protected static SessionComponent sessions;
    @InjectComponent
    protected static PacketInterceptionComponent packetInterceptor;
    @InjectComponent
    protected static PrayerComponent prayers;
    @InjectComponent
    protected static GuildComponent guilds;
    @InjectComponent
    protected static FlightItemsComponent flightItems;
    @InjectComponent
    protected static ManagedWorldComponent managedWorld;

    private static final ItemCondenser MONEY_CONDENSER = new ItemCondenser();

    static {
        MONEY_CONDENSER.addSupport(new ItemStack(Material.GOLD_NUGGET, 9), new ItemStack(Material.GOLD_INGOT, 1));
        MONEY_CONDENSER.addSupport(new ItemStack(Material.GOLD_INGOT, 9), new ItemStack(Material.GOLD_BLOCK, 1));
    }

    private static final ItemCondenser SUMMATION_CONDENSER = new ItemCondenser();

    static {
        // Bone
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.BONE_MEAL, 9), new ItemStack(Material.BONE_BLOCK, 1));

        // Coal
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.COAL, 9), new ItemStack(Material.COAL_BLOCK, 1));

        // Iron
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.RAW_IRON, 9), new ItemStack(Material.RAW_IRON_BLOCK, 1));
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.IRON_NUGGET, 9), new ItemStack(Material.IRON_INGOT, 1));
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.IRON_INGOT, 9), new ItemStack(Material.IRON_BLOCK, 1));

        // Copper
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.RAW_COPPER, 9), new ItemStack(Material.RAW_COPPER_BLOCK, 1));
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.COPPER_INGOT, 9), new ItemStack(Material.COPPER_BLOCK, 1));

        // Gold
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.RAW_GOLD, 9), new ItemStack(Material.RAW_GOLD_BLOCK, 1));
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.GOLD_NUGGET, 9), new ItemStack(Material.GOLD_INGOT, 1));
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.GOLD_INGOT, 9), new ItemStack(Material.GOLD_BLOCK, 1));

        // Redstone
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.REDSTONE, 9), new ItemStack(Material.REDSTONE_BLOCK, 1));

        // Lapis
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.LAPIS_LAZULI, 9), new ItemStack(Material.LAPIS_BLOCK, 1));

        // Diamond
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.DIAMOND, 9), new ItemStack(Material.DIAMOND_BLOCK, 1));

        // Emerald
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.EMERALD, 9), new ItemStack(Material.EMERALD_BLOCK, 1));

        // Netherite
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.NETHERITE_INGOT, 9), new ItemStack(Material.NETHERITE_BLOCK, 1));

        // Slime
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.SLIME_BALL, 9), new ItemStack(Material.SLIME_BLOCK, 1));

        // Wheat
        SUMMATION_CONDENSER.addSupport(new ItemStack(Material.WHEAT, 9), new ItemStack(Material.HAY_BLOCK, 1));
    }

    private MigrationManager migrationManager = new MigrationManager();

    @Override
    public void enable() {
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        loadResources();
        registerSpecWeapons();
        registerTools();
        registerHymns();
        registerPermissionBindingTomes();
        registerGuildOaths();
        registerGeneral();
        registerMigrations();
    }

    private <T extends Listener> T handle(T component) {
        CommandBook.registerEvents(component);
        return component;
    }

    private void loadResources() {
        AbstractItemFeatureImpl.applyResource(admin);
        AbstractItemFeatureImpl.applyResource(sessions);
        AbstractItemFeatureImpl.applyResource(prayers);
        AbstractItemFeatureImpl.applyResource(guilds);
        AbstractItemFeatureImpl.applyResource(flightItems);
        AbstractItemFeatureImpl.applyResource(managedWorld);
    }

    private void registerSpecWeapons() {
        // Delay by 1 tick to allow the packet interceptor to get setup.
        // This should be removable once libcomponents respects component dependencies
        server.getScheduler().runTaskLater(inst, () -> {
            WeaponSysImpl wepSys = handle(new WeaponSysImpl(packetInterceptor));

            wepSys.add(WeaponType.RANGED, CustomItems.MASTER_BOW, handle(new MasterBowImpl()));
            wepSys.add(WeaponType.RANGED, CustomItems.FEAR_BOW, handle(new FearBowImpl()));
            wepSys.add(WeaponType.RANGED, CustomItems.UNLEASHED_BOW, handle(new UnleashedBowImpl()));
            wepSys.add(WeaponType.RANGED, CustomItems.RED_BOW, handle(new RedBowImpl()));

            wepSys.add(WeaponType.MELEE, CustomItems.MASTER_SWORD, handle(new MasterSwordImpl()));
            wepSys.add(WeaponType.MELEE, CustomItems.MASTER_SHORT_SWORD, handle(new MasterSwordImpl()));
            wepSys.add(WeaponType.MELEE, CustomItems.FEAR_SWORD, handle(new FearSwordImpl()));
            wepSys.add(WeaponType.MELEE, CustomItems.FEAR_SHORT_SWORD, handle(new FearSwordImpl()));
            wepSys.add(WeaponType.MELEE, CustomItems.UNLEASHED_SWORD, handle(new UnleashedSwordImpl()));
            wepSys.add(WeaponType.MELEE, CustomItems.UNLEASHED_SHORT_SWORD, handle(new UnleashedSwordImpl()));
            wepSys.add(WeaponType.MELEE, CustomItems.RED_SWORD, handle(new RedSwordImpl()));
            wepSys.add(WeaponType.MELEE, CustomItems.PHANTOM_SABRE, handle(new PhantomSabreImpl()));
        }, 1);
    }

    private void registerTools() {
        handle(new LinearAxe());
        handle(new LinearPickaxe());
        handle(new LinearShovel());
        handle(new LinearBlockPlacer());
        handle(new RadialAxe());
        handle(new RadialPickaxe());
        handle(new RadialShovel());
    }

    private void registerHymns() {
        HymnImpl hymnImpl = handle(new HymnImpl());
        hymnImpl.addHymn(CustomItems.PHANTOM_HYMN, HymnSingEvent.Hymn.PHANTOM);
        hymnImpl.addHymn(CustomItems.CHICKEN_HYMN, HymnSingEvent.Hymn.CHICKEN);
        hymnImpl.addHymn(CustomItems.HYMN_OF_HARVEST, HymnSingEvent.Hymn.HARVEST);
        hymnImpl.addHymn(CustomItems.HYMN_OF_SUMMATION, HymnSingEvent.Hymn.SUMMATION);

        handle(new ChickenHymnImpl());
        handle(new HymnOfHarvestImpl());
        handle(new SummationHymnImpl(SUMMATION_CONDENSER));
    }

    private void registerPermissionBindingTomes() {
        PermissionBindingTomeImpl tomeImpl = handle(new PermissionBindingTomeImpl());
        tomeImpl.addTome(CustomItems.TOME_OF_CURSED_SMELTING, "aurora.tome.cursedsmelting");
        tomeImpl.addTome(CustomItems.TOME_OF_THE_CLEANLY, "aurora.tome.cleanly");
        tomeImpl.addTome(CustomItems.TOME_OF_SACRIFICE, "aurora.tome.sacrifice");
        tomeImpl.addTome(CustomItems.TOME_OF_DIVINITY, "aurora.tome.divinity");
        tomeImpl.addTome(CustomItems.TOME_OF_THE_UNDEAD, "aurora.tome.undead");
        tomeImpl.addTome(CustomItems.TOME_OF_LEGENDS, "aurora.tome.legends");
        tomeImpl.addTome(CustomItems.TOME_OF_LIFE, "aurora.tome.life");
    }

    private void registerGuildOaths() {
        GuildOathImpl oathImpl = handle(new GuildOathImpl());
        oathImpl.addOath(CustomItems.NINJA_OATH, GuildType.NINJA);
        oathImpl.addOath(CustomItems.ROGUE_OATH, GuildType.ROGUE);
    }

    private void registerGeneral() {
        handle(new AncientArmorImpl());
        handle(new AncientCrownImpl(MONEY_CONDENSER));
        handle(new BatBowImpl());
        handle(new ChickenBowImpl());
        handle(new ExecutionerAxeImpl());
        handle(new GodFishImpl());
        handle(new ImbuedCrystalImpl(MONEY_CONDENSER));
        handle(new MadMilkImpl());
        handle(new MagicBucketImpl());
        handle(new NecrosArmorImpl());
        handle(new NectricArmorImpl());
        handle(new PeacefulWarriorArmor());
        handle(new PhantomLinkImpl());
        handle(new PhantomPotionImpl());
        handle(new PixieDustImpl());
        handle(new PotionOfRestitutionImpl());
        handle(new RedFeatherImpl());
        handle(new SummationScrollImpl(SUMMATION_CONDENSER));
    }

    private void registerMigrations() {
        migrationManager.add(new FearSwordMigration());
        migrationManager.add(new UnleashedSwordMigration());
        migrationManager.add(new MasterSwordMigration());
        migrationManager.add(new GodSwordMigration());

        registerCommands(MigrationCommands.class);
    }

    private void updateResult(PrepareResultEvent event, ItemStack result) {
        event.setResult(result);

        event.getViewers().forEach((entity) -> {
            if (entity instanceof Player) {
                ((Player) entity).updateInventory();
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();

        ItemStack[] slots = { inventory.getItem(0), inventory.getItem(1) };

        // Always clear the result if we're using a custom item in the repair slot
        if (ItemUtil.isAuthenticCustomItem(slots[1])) {
            updateResult(event, null);
            return;
        }

        ItemStack result = event.getResult();
        if (result == null) {
            return;
        }

        // Always clear the slot if the result is a custom item, and the source isn't
        if (!ItemUtil.isAuthenticCustomItem(slots[0]) && ItemUtil.isAuthenticCustomItem(result)) {
            updateResult(event, null);
            return;
        }

        // If the source isn't a custom item, we're done
        if (!ItemUtil.isAuthenticCustomItem(slots[0])) {
            return;
        }

        // If the repair item isn't a book of enchanting, block the anvil interaction for the custom block
        if (slots[1] != null && slots[1].getType() != Material.ENCHANTED_BOOK) {
            updateResult(event, null);
            return;
        }

        // If the types don't match somehow, clear the type. At the time of writing I don't see any way this could
        // happen, but I could see it happening.
        if (result.getType() != slots[0].getType()) {
            updateResult(event, null);
            return;
        }

        ItemStack newResult = result.clone();

        ItemMeta meta = newResult.getItemMeta();

        Optional<String> optOriginalName = ItemNameCalculator.computeItemName(slots[0]);

        // This isn't a real custom item, clear the result
        if (optOriginalName.isEmpty()) {
            updateResult(event, null);
            return;
        }

        String originalName = optOriginalName.get().replaceFirst("grindstone:", "").toUpperCase();
        CustomItems item = CustomItems.valueOf(originalName);
        meta.setDisplayName(item.getColoredName());
        newResult.setItemMeta(meta);

        updateResult(event, newResult);
    }

    @EventHandler
    public void onPrepareResult(PrepareResultEvent event) {
        // Handled in the onPrepareAnvil handler
        if (event instanceof PrepareAnvilEvent) {
            return;
        }

        for (ItemStack itemStack : event.getInventory().getContents()) {
            if (ItemUtil.isAuthenticCustomItem(itemStack)) {
                updateResult(event, null);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraftCustomItem(CraftItemEvent event) {
        HumanEntity crafter = event.getWhoClicked();
        for (ItemStack itemStack : event.getInventory().getMatrix()) {
            if (ItemUtil.isAuthenticCustomItem(itemStack)) {
                ChatUtil.sendError(crafter, "You cannot use custom items as part of a crafting recipe.");
                event.setCancelled(true);
                return;
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
