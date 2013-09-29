package com.skelril.aurora.economic.store;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.database.UnloadableDatabase;
import com.skelril.aurora.util.item.ItemType;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Admin Store", desc = "Admin Store system.")
@Depend(plugins = {"WorldGuard"}, components = {AdminComponent.class})
public class AdminStoreComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminComponent adminComponent;

    private ItemStoreDatabase itemStoreDatabase;

    private ProtectedRegion region = null;
    private Economy econ;

    @Override
    public void enable() {

        File storeDirectory = new File(inst.getDataFolder().getPath() + "/store");
        if (!storeDirectory.exists()) storeDirectory.mkdir();

        itemStoreDatabase = new CSVItemStoreDatabase(storeDirectory);
        itemStoreDatabase.load();

        // Setup external systems
        setupEconomy();
        // Register commands
        registerCommands(Commands.class);

        // Get the region
        server.getScheduler().runTaskLater(inst, new Runnable() {
            @Override
            public void run() {

                try {
                    region = getWorldGuard().getGlobalRegionManager().get(Bukkit.getWorld("City")).getRegion("vineam-district-bank");
                } catch (UnknownPluginException e) {
                    e.printStackTrace();
                }
            }
        }, 1);
    }

    @Override
    public void reload() {

        itemStoreDatabase.load();
    }

    @Override
    public void disable() {

        if (itemStoreDatabase instanceof UnloadableDatabase) {
            ((UnloadableDatabase) itemStoreDatabase).unload();
        }
    }

    private final String NOT_AVAILIBLE = "No item by that name is currently available!";

    public class Commands {

        @Command(aliases = {"market", "mk", "ge"}, desc = "Admin Store commands")
        @NestedCommand({StoreCommands.class})
        public void storeCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class StoreCommands {

        @Command(aliases = {"buy", "b"},
                usage = "[-a amount] <item name>", desc = "Buy an item",
                flags = "a:", min = 1)
        public void buyCmd(CommandContext args, CommandSender sender) throws CommandException {

            String playerName = checkPlayer(sender);
            Player player = (Player) sender;

            String itemName = args.getJoinedStrings(0).toLowerCase();

            if (!hasItemOfName(itemName)) {
                ItemType type = ItemType.lookup(itemName);
                if (type == null) {
                    throw new CommandException(NOT_AVAILIBLE);
                }
                itemName = type.getName();
            }
            ItemPricePair itemPricePair = itemStoreDatabase.getItem(itemName);

            if (itemPricePair == null || !itemPricePair.isBuyable()) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            int amt = 1;
            if (args.hasFlag('a')) {
                amt = Math.max(1, args.getFlagInteger('a'));
            }
            double price = itemPricePair.getPrice() * amt;
            double rebate = 0;

            if (inst.hasPermission(sender, "aurora.market.rebate.onepointseven")) {
                rebate = price * .017;
            }

            if (!econ.has(playerName, price)) {
                throw new CommandException("You do not have enough money to purchase that item(s).");
            }

            // Get the items and add them to the inventory
            ItemStack[] itemStacks = getItem(itemPricePair.getName(), amt);
            for (ItemStack itemStack : itemStacks) {
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItem(player.getLocation(), itemStack);
                    continue;
                }
                player.getInventory().addItem(itemStack);
            }

            // Charge the money and send the sender some feedback
            econ.withdrawPlayer(playerName, price - rebate);
            itemStoreDatabase.logTransaction(playerName, itemName, amt);
            String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(price), " " + econ.currencyNamePlural());
            ChatUtil.sendNotice(sender, "Item(s) purchased for " + priceString + "!");
            if (rebate != 0) {
                String rebateString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(rebate), " " + econ.currencyNamePlural());
                ChatUtil.sendNotice(sender, "You get " + rebateString + " back.");
            }
        }

        @Command(aliases = {"sell", "s"},
                usage = "", desc = "Sell an item",
                flags = "", min = 0, max = 0)
        public void sellCmd(CommandContext args, CommandSender sender) throws CommandException {

            String playerName = checkPlayer(sender);
            final Player player = (Player) sender;

            ItemStack stack = player.getInventory().getItemInHand();
            if (stack == null || stack.getTypeId() == 0) {
                throw new CommandException("That's not a valid item!");
            }

            ItemMeta stackMeta = stack.getItemMeta();
            String itemName = stack.getTypeId() + ":" + stack.getDurability();
            if (stackMeta.hasDisplayName()) {
                itemName = stackMeta.getDisplayName();
                if (!ItemUtil.isAuthenticCustomItem(itemName)) {
                    throw new CommandException("You cannot sell items that have been named here!");
                }
                itemName = ChatColor.stripColor(itemName);
            }

            double percentageSale = 1;
            if (stack.getDurability() != 0 && !ItemType.usesDamageValue(stack.getTypeId())) {
                if (stack.getAmount() > 1) {
                    throw new CommandException(NOT_AVAILIBLE);
                }
                percentageSale = 1 - ((double) stack.getDurability() / (double) stack.getType().getMaxDurability());
            }

            if (!hasItemOfName(itemName)) {
                ItemType type = ItemType.lookup(itemName);
                if (type == null) {
                    throw new CommandException(NOT_AVAILIBLE);
                }
                itemName = type.getName();
            }

            ItemPricePair itemPricePair = itemStoreDatabase.getItem(itemName);

            if (itemPricePair == null || !itemPricePair.isSellable()) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            int amt = stack.getAmount();
            double payment = itemPricePair.getSellPrice() * amt * percentageSale;

            server.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {
                    player.getInventory().setItemInHand(null);

                }
            }, 1);
            econ.depositPlayer(playerName, payment);
            itemStoreDatabase.logTransaction(playerName, itemName, amt * -1);

            String paymentString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(payment), " " + econ.currencyNamePlural());
            ChatUtil.sendNotice(player, "Item(s) sold for: " + paymentString + "!");
        }

        @Command(aliases = {"list", "l"},
                usage = "[-p page] [filter...]", desc = "Get a list of items and their prices",
                flags = "p:", min = 0
        )
        public void listCmd(CommandContext args, CommandSender sender) throws CommandException {

            checkPlayer(sender, true);

            String filterString = args.argsLength() > 0 ? args.getJoinedStrings(0) : null;
            List<ItemPricePair> itemPricePairCollection = itemStoreDatabase.getItemList(filterString);
            Collections.sort(itemPricePairCollection);

            final int entryToShow = 9;
            final int listSize = itemPricePairCollection.size();

            // Page info
            int page = 0;
            int maxPage = listSize / entryToShow;

            if (args.hasFlag('p')) {
                page = Math.min(maxPage, Math.max(0, args.getFlagInteger('p') - 1));
            }

            // Viewable record info
            int min = entryToShow * page;
            int max = Math.min(listSize, min + entryToShow);

            // Result display
            ChatUtil.sendNotice(sender, ChatColor.GOLD, "Item List - Page (" + (page + 1) + "/" + (maxPage + 1) + ")");
            for (int i = min; i < max; i++) {
                ItemPricePair pair = itemPricePairCollection.get(i);
                String buy, sell;
                if (pair.isBuyable()) {
                    buy = ChatColor.WHITE + econ.format(pair.getPrice()) + ChatColor.YELLOW;
                } else {
                    buy = ChatColor.GRAY + "unavailable" + ChatColor.YELLOW;
                }
                if (pair.isSellable()) {
                    sell = ChatColor.WHITE + econ.format(pair.getSellPrice()) + ChatColor.YELLOW;
                } else {
                    sell = ChatColor.GRAY + "unavailable" + ChatColor.YELLOW;
                }
                ChatUtil.sendNotice(sender, ChatColor.BLUE + pair.getName().toUpperCase() + ChatColor.YELLOW + " (Quick Price: " + buy + " - " + sell + ")");
            }
        }

        @Command(aliases = {"lookup", "value", "info", "pc"},
                usage = "[item name]", desc = "Value an item",
                flags = "", min = 0)
        public void valueCmd(CommandContext args, CommandSender sender) throws CommandException {

            checkPlayer(sender, true);

            final Player player = (Player) sender;

            String itemName;
            double percentageSale = 1;
            if (args.argsLength() > 0) {
                itemName = args.getJoinedStrings(0).toLowerCase();
            } else {
                ItemStack stack = player.getInventory().getItemInHand();
                if (stack == null || stack.getTypeId() == 0) {
                    throw new CommandException("That's not a valid item!");
                }


                itemName = stack.getTypeId() + ":" + stack.getDurability();
                ItemMeta stackMeta = stack.getItemMeta();
                if (stackMeta.hasDisplayName()) {
                    itemName = stackMeta.getDisplayName();
                    if (!ItemUtil.isAuthenticCustomItem(itemName)) {
                        throw new CommandException(NOT_AVAILIBLE);
                    }
                    itemName = ChatColor.stripColor(itemName);
                }

                if (stack.getDurability() != 0 && !ItemType.usesDamageValue(stack.getTypeId())) {
                    if (stack.getAmount() > 1) {
                        throw new CommandException(NOT_AVAILIBLE);
                    }
                    percentageSale = 1 - ((double) stack.getDurability() / (double) stack.getType().getMaxDurability());
                }
            }

            if (!hasItemOfName(itemName)) {
                ItemType type = ItemType.lookup(itemName);
                if (type == null) {
                    throw new CommandException(NOT_AVAILIBLE);
                }
                itemName = type.getName();
            }

            ItemPricePair itemPricePair = itemStoreDatabase.getItem(itemName);

            if (itemPricePair == null) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            double paymentPrice = itemPricePair.getSellPrice() * percentageSale;

            String purchasePrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(itemPricePair.getPrice()), " " + econ.currencyNamePlural());
            String sellPrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(paymentPrice), " " + econ.currencyNamePlural());
            ChatUtil.sendNotice(player, ChatColor.GOLD, "Price Information for: " + ChatColor.BLUE + itemName.toUpperCase());
            if (itemPricePair.isBuyable()) {
                ChatUtil.sendNotice(player, "When you buy it you pay:");
                ChatUtil.sendNotice(player, " - " + purchasePrice + " each.");
            } else {
                ChatUtil.sendNotice(player, ChatColor.GRAY, "This item cannot be purchased.");
            }
            if (itemPricePair.isSellable()) {
                ChatUtil.sendNotice(player, "When you sell it you get:");
                ChatUtil.sendNotice(player, " - " + sellPrice + " each.");
                if (percentageSale != 1.0) {
                    sellPrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(itemPricePair.getSellPrice()), " " + econ.currencyNamePlural());
                    ChatUtil.sendNotice(player, " - " + sellPrice + " each when new.");
                }
            } else {
                ChatUtil.sendNotice(player, ChatColor.GRAY, "This item cannot be sold.");
            }
        }

        @Command(aliases = {"admin"}, desc = "Administrative Commands")
        @NestedCommand({AdminStoreCommands.class})
        public void AdministrativeCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class AdminStoreCommands {

        @Command(aliases = {"add"},
                usage = "[-p price] <item name>", desc = "Add an item to the database",
                flags = "bsp:", min = 1)
        @CommandPermissions("aurora.admin.adminstore.add")
        public void addCmd(CommandContext args, CommandSender sender) throws CommandException {

            String itemName = args.getJoinedStrings(0);

            if (!hasItemOfName(itemName)) {
                ItemType type = ItemType.lookup(itemName);
                if (type == null) {
                    throw new CommandException("No item by that name was found.");
                }
                itemName = type.getName();
            }

            boolean disableBuy = args.hasFlag('b');
            boolean disableSell = args.hasFlag('s');

            if (disableBuy && disableSell) {
                throw new CommandException("If you disable buying and selling, then what's the point in adding it dumb ass?");
            }

            double price = 10;
            if (args.hasFlag('p')) {
                price = Math.max(1, args.getFlagDouble('p'));
            }

            // Database operations
            ItemPricePair oldItem = itemStoreDatabase.getItem(itemName);
            itemStoreDatabase.addItem(sender.getName(), itemName, price, disableBuy, disableSell);
            itemStoreDatabase.save();

            // Notification
            String noticeString = oldItem == null ? " added with a price of " : " is now ";
            String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(price), " " + econ.currencyNamePlural());
            ChatUtil.sendNotice(sender, ChatColor.BLUE + itemName.toUpperCase() + ChatColor.YELLOW + noticeString + priceString + "!");
            if (disableBuy) {
                ChatUtil.sendNotice(sender, " - It cannot be purchased.");
            } else if (disableSell) {
                ChatUtil.sendNotice(sender, " - It cannot be sold.");
            }
        }

        @Command(aliases = {"remove"},
                usage = "<item name>", desc = "Value an item",
                flags = "", min = 1)
        @CommandPermissions("aurora.admin.adminstore.remove")
        public void removeCmd(CommandContext args, CommandSender sender) throws CommandException {

            String itemName = args.getJoinedStrings(0);

            if (!hasItemOfName(itemName)) {
                ItemType type = ItemType.lookup(itemName);
                if (type == null) {
                    throw new CommandException(NOT_AVAILIBLE);
                }
                itemName = type.getName();
            }

            if (itemStoreDatabase.getItem(itemName) == null) {
                throw new CommandException(NOT_AVAILIBLE);
            }

            itemStoreDatabase.removeItem(sender.getName(), itemName);
            itemStoreDatabase.save();
            ChatUtil.sendNotice(sender, ChatColor.BLUE + itemName.toUpperCase() + ChatColor.YELLOW + " has been removed from the database!");
        }
    }

    private static List<String> names = new ArrayList<>();

    static {
        names.add("cursed gold ore");
        names.add("cursed gold bar");

        names.add("pixie dust");

        names.add("god fish");

        names.add("gem of life");
        names.add("gem of darkness");
        names.add("imbued crystal");

        names.add("bat bow");

        names.add("magic bucket");

        names.add("unleashed sword");
        names.add("unleashed bow");

        names.add("fear sword");
        names.add("fear bow");

        names.add("master sword");
        names.add("master bow");

        names.add("divine combat potion");
        names.add("holy combat potion");
        names.add("extreme combat potion");

        names.add("potion of restitution");

        names.add("god sword");
        names.add("god bow");

        names.add("overseer's bow");

        names.add("god pickaxe");
        names.add("legendary god pickaxe");

        names.add("god helmet");
        names.add("god chestplate");
        names.add("god leggings");
        names.add("god boots");

        names.add("ancient crown");
        names.add("ancient helmet");
        names.add("ancient chestplate");
        names.add("ancient leggings");
        names.add("ancient boots");
    }

    private boolean hasItemOfName(String name) {

        for (String aName : names) {
            if (name.equalsIgnoreCase(aName)) return true;
        }
        return false;
    }

    private ItemStack[] getItem(String name, int amount) {

        name = name.toLowerCase();

        List<ItemStack> itemStacks = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            switch (name) {
                case "cursed gold ore":
                    itemStacks.add(ItemUtil.Misc.cursedGold(1, true));
                    break;
                case "cursed gold bar":
                    itemStacks.add(ItemUtil.Misc.cursedGold(1, false));
                    break;
                case "pixie dust":
                    itemStacks.add(ItemUtil.Misc.pixieDust(1));
                    break;
                case "god fish":
                    itemStacks.add(ItemUtil.Misc.godFish(1));
                    break;
                case "gem of life":
                    itemStacks.add(ItemUtil.Misc.gemOfLife(1));
                    break;
                case "gem of darkness":
                    itemStacks.add(ItemUtil.Misc.gemOfDarkness(1));
                    break;
                case "imbued crystal":
                    itemStacks.add(ItemUtil.Misc.imbuedCrystal(1));
                    break;
                case "bat bow":
                    itemStacks.add(ItemUtil.Misc.batBow());
                    break;
                case "magic bucket":
                    itemStacks.add(ItemUtil.Misc.magicBucket());
                    break;
                case "unleashed sword":
                    itemStacks.add(ItemUtil.Unleashed.makeSword());
                    break;
                case "unleashed bow":
                    itemStacks.add(ItemUtil.Unleashed.makeBow());
                    break;
                case "fear sword":
                    itemStacks.add(ItemUtil.Fear.makeSword());
                    break;
                case "fear bow":
                    itemStacks.add(ItemUtil.Fear.makeBow());
                    break;
                case "master sword":
                    itemStacks.add(ItemUtil.Master.makeSword());
                    break;
                case "master bow":
                    itemStacks.add(ItemUtil.Master.makeBow());
                    break;
                case "divine combat potion":
                    itemStacks.add(ItemUtil.CPotion.divineCombatPotion());
                    break;
                case "holy combat potion":
                    itemStacks.add(ItemUtil.CPotion.holyCombatPotion());
                    break;
                case "extreme combat potion":
                    itemStacks.add(ItemUtil.CPotion.extremeCombatPotion());
                    break;
                case "potion of restitution":
                    itemStacks.add(ItemUtil.MPotion.potionOfRestitution());
                    break;
                case "god sword":
                    itemStacks.add(ItemUtil.God.makeSword());
                    break;
                case "god bow":
                    itemStacks.add(ItemUtil.God.makeBow());
                    break;
                case "overseer's bow":
                    itemStacks.add(ItemUtil.Misc.overseerBow());
                    break;
                case "god pickaxe":
                    itemStacks.add(ItemUtil.God.makePickaxe(false));
                    break;
                case "legendary god pickaxe":
                    itemStacks.add(ItemUtil.God.makePickaxe(true));
                    break;
                case "god helmet":
                    itemStacks.add(ItemUtil.God.makeHelmet());
                    break;
                case "god chestplate":
                    itemStacks.add(ItemUtil.God.makeChest());
                    break;
                case "god leggings":
                    itemStacks.add(ItemUtil.God.makeLegs());
                    break;
                case "god boots":
                    itemStacks.add(ItemUtil.God.makeBoots());
                    break;
                case "ancient crown":
                    itemStacks.add(ItemUtil.Ancient.makeCrown());
                    break;
                case "ancient helmet":
                    itemStacks.add(ItemUtil.Ancient.makeHelmet());
                    break;
                case "ancient chestplate":
                    itemStacks.add(ItemUtil.Ancient.makeChest());
                    break;
                case "ancient leggings":
                    itemStacks.add(ItemUtil.Ancient.makeLegs());
                    break;
                case "ancient boots":
                    itemStacks.add(ItemUtil.Ancient.makeBoots());
                    break;
                default:
                    ItemType type = ItemType.lookup(name);
                    itemStacks.add(new ItemStack(type.getID(), 1, (short) type.getData()));
                    break;
            }
        }
        return itemStacks.toArray(new ItemStack[itemStacks.size()]);
    }

    private static Set<Integer> ignored = new HashSet<>();

    static {
        ignored.add(BlockID.AIR);
        ignored.add(BlockID.WATER);
        ignored.add(BlockID.STATIONARY_WATER);
        ignored.add(BlockID.LAVA);
        ignored.add(BlockID.STATIONARY_LAVA);
        ignored.add(BlockID.GRASS);
        ignored.add(BlockID.DIRT);
        ignored.add(BlockID.GRAVEL);
        ignored.add(BlockID.SAND);
        ignored.add(BlockID.SANDSTONE);
        ignored.add(BlockID.SNOW);
        ignored.add(BlockID.SNOW_BLOCK);
        ignored.add(BlockID.STONE);
        ignored.add(BlockID.BEDROCK);
    }

    public double priceCheck(int blockID, int data) {

        if (ignored.contains(blockID) || EnvironmentUtil.isValuableBlock(blockID)) return 0;

        ItemType type = ItemType.lookup(blockID + ":" + data);

        if (type == null) return 0;

        ItemPricePair itemPricePair = itemStoreDatabase.getItem(type.getName());

        if (itemPricePair == null) return 0;

        return itemPricePair.isBuyable() ? itemPricePair.getPrice() : 0;
    }

    public String checkPlayer(CommandSender sender) throws CommandException {

        return checkPlayer(sender, false);
    }

    private String checkPlayer(CommandSender sender, boolean onlyPlayer) throws CommandException {

        if (!(sender instanceof Player)) {
            throw new CommandException("You must be a player to use this command.");
        }

        if (onlyPlayer) return sender.getName();

        if (adminComponent.isAdmin((Player) sender)) {
            throw new CommandException("You cannot use this command while in admin mode.");
        }

        Vector v = BukkitUtil.toVector(((Player) sender).getLocation());
        if (!((Player) sender).getWorld().getName().equals("City") || region == null || !region.contains(v)) {
            throw new CommandException("You call out, but no one hears your offer.");
        }

        return sender.getName();
    }

    private WorldGuardPlugin getWorldGuard() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }

        return (WorldGuardPlugin) plugin;
    }

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        }

        return (econ != null);
    }
}
