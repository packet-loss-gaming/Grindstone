package com.skelril.aurora.easycommandsystem;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.databases.RegionDBUtil;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.RegionGroupFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.District;
import com.skelril.aurora.economic.store.AdminStoreComponent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.RegionUtil;
import com.skelril.aurora.util.item.BookUtil;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Home Manager", desc = "Home ECS")
@Depend(plugins = {"WorldEdit", "WorldGuard"}, components = AdminStoreComponent.class)
public class HomeManagerComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private AdminStoreComponent store;

    private WorldEditPlugin WE;
    private WorldGuardPlugin WG;
    private Economy econ;

    @Override
    public void enable() {

        registerCommands(Commands.class);
        setUpWorldEdit();
        setUpWorldGuard();
        setUpEconomy();
    }

    private void setUpWorldEdit() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldEdit");

        // WorldEdit may not be loaded
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            return;
        }

        this.WE = (WorldEditPlugin) plugin;
    }

    private void setUpWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return;
        }

        this.WG = (WorldGuardPlugin) plugin;
    }

    private void setUpEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        }
    }

    public class Commands {

        @Command(aliases = {"home"}, desc = "Home Manager")
        @NestedCommand({NestedCommands.class})
        public void homeCmd(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {

        @Command(aliases = {"info"}, usage = "", desc = "View info about your home",
                min = 0, max = 0)
        @CommandPermissions({"aurora.home.self.info"})
        public void infoHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player player = (Player) sender;

                RegionManager manager = WG.getRegionManager(player.getWorld());
                ProtectedRegion region = manager.getRegionExact(getHome(player));
                if (region == null) throw new CommandException("You do not have a home in this world!");
                District district = getDistrict(region.getParent().getId().replace("-district", ""));

                StringBuilder builtInfo = new StringBuilder();
                builtInfo.append(ChatColor.YELLOW + "Home Owner: ").append(sender.getName()).append(", ");
                builtInfo.append(ChatColor.GRAY + "District: ").append(district.toProperName());
                builtInfo.append("\n");
                builtInfo.append(ChatColor.AQUA + "Server Managers: ").append(District.GLOBAL.getManagersFriendly());
                builtInfo.append("\n");
                builtInfo.append(ChatColor.AQUA + "District Managers: ").append(district.getManagersFriendly());


                if (region.getMembers().size() > 0) {
                    builtInfo.append("\n");
                    builtInfo.append(ChatColor.BLUE + "Friends: ").append(region.getMembers().toUserFriendlyString());
                }

                boolean hasFlags = false;
                final StringBuilder s = new StringBuilder("\n" + ChatColor.BLUE + "Properties: ");
                for (Flag<?> flag : DefaultFlag.getFlags()) {
                    Object val = region.getFlag(flag), group = null;

                    if (val == null) {
                        continue;
                    }

                    if (hasFlags) {
                        s.append(", ");
                    }

                    RegionGroupFlag groupFlag = flag.getRegionGroupFlag();
                    if (groupFlag != null) {
                        group = region.getFlag(groupFlag);
                    }

                    if (group == null) {
                        s.append(flag.getName()).append(": ").append(String.valueOf(val));
                    } else {
                        s.append(flag.getName()).append(" -g ").append(String.valueOf(group)).append(": ")
                                .append(String.valueOf(val));
                    }

                    hasFlags = true;
                }
                if (hasFlags) {
                    builtInfo.append(s.toString());
                }

                ChatUtil.sendNotice(sender, ChatColor.RESET, builtInfo.toString());
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"addplayer"}, usage = "<player>", desc = "Add a player to your home",
                min = 1, max = 1)
        @CommandPermissions({"aurora.home.self.add"})
        public void addMemberToHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player player = (Player) sender;

                RegionManager manager = WG.getRegionManager(player.getWorld());
                ProtectedRegion region = manager.getRegionExact(getHome(player));
                if (region == null) throw new CommandException("You do not have a home in this world!");
                RegionDBUtil.addToDomain(region.getMembers(), args.getPaddedSlice(1, 0), 0);
                try {
                    manager.save();
                } catch (ProtectionDatabaseException e) {
                    ChatUtil.sendError(sender, "Failed to add player to your home.");
                }
                ChatUtil.sendNotice(player, "Home successfully updated!");
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"removeplayer"}, usage = "<player>", desc = "Remove a player from your home",
                min = 1, max = 1)
        @CommandPermissions({"aurora.home.self.remove"})
        public void removeMemberToHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player player = (Player) sender;

                RegionManager manager = WG.getRegionManager(player.getWorld());
                ProtectedRegion region = manager.getRegionExact(getHome(player));
                if (region == null) throw new CommandException("You do not have a home in this world!");
                RegionDBUtil.removeFromDomain(region.getMembers(), args.getPaddedSlice(1, 0), 0);
                try {
                    manager.save();
                } catch (ProtectionDatabaseException e) {
                    ChatUtil.sendError(sender, "Failed to remove players from your home.");
                }
                ChatUtil.sendNotice(player, "Home successfully updated!");
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"flag"}, usage = "<flag>", desc = "Flag a home",
                min = 1)
        @CommandPermissions({"aurora.home.self.flag"})
        public void flagHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player player = (Player) sender;
                String flag = args.getJoinedStrings(0);

                Bukkit.dispatchCommand(sender, "rg flag " + getHome(player) + " " + flag);
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"location", "loc"}, desc = "Home Manager")
        @NestedCommand({TeleportCommands.class})
        public void homeTeleportCmds(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"rules"}, usage = "[district]", desc = "District Rules", min = 0, max = 1)
        public void homeRuleCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) return;

            String district;

            if (args.argsLength() == 0) {
                RegionManager manager = WG.getRegionManager(((Player) sender).getWorld());
                ProtectedRegion home = manager.getRegion(getHome(sender.getName()));

                if (home == null) {
                    throw new CommandException("You don't live in a district and did not "
                            + "specify a district in this world.");
                } else {
                    district = home.getParent().getId().replace("-district", "");
                }
            } else {
                district = args.getString(0).toLowerCase().replace("-district", "");
            }

            if (!giveRuleBook(sender, sender.getName(), district)) {
                throw new CommandException("No district by that name found in this world.");
            }
        }

        @Command(aliases = {"buy"}, desc = "Buy a house", min = 0, max = 0, flags = "y")
        @CommandPermissions("aurora.auth.member")
        public void homeBuyCmd(CommandContext args, CommandSender sender) throws CommandException {


            if (sender instanceof Player) {
                Player player = (Player) sender;

                RegionManager manager = WG.getRegionManager(player.getWorld());
                ProtectedRegion region = manager.getRegionExact(getHome(player));
                if (region != null) throw new CommandException("You already have a home in this world!");

                ApplicableRegionSet applicable = manager.getApplicableRegions(player.getLocation());

                Iterator<ProtectedRegion> it = applicable.iterator();
                while (it.hasNext() && region == null) {
                    ProtectedRegion aRegion = it.next();
                    if (aRegion.getId().endsWith("-s")) {
                        region = aRegion;
                    }
                }

                if (region == null) {
                    throw new CommandException("You are not currently standing in any regions available for purchase.");
                }

                // Get the price and send it to the player
                Double price = region.getFlag(DefaultFlag.PRICE);

                if (price == null || price < 1) {
                    throw new CommandException("This house cannot currently be bought.");
                }

                String priceString = ChatUtil.makeCountString(ChatColor.YELLOW,
                        econ.format(price), " " + econ.currencyNamePlural());
                ChatUtil.sendNotice(player, "This property is worth: " + priceString + ".");

                // If they have used the flag y proceed to buy the house
                // otherwise inform them about how to buy the house
                if (args.hasFlag('y')) {
                    if (!econ.has(player.getName(), price)) {
                        throw new CommandException("Sorry, you cannot currently afford this house.");
                    }
                    try {
                        if (RegionUtil.renameRegion(manager, region.getId(), getHome(player), true)) {

                            region = manager.getRegion(getHome(player));
                            region.getOwners().addPlayer(player.getName());
                            region.setFlag(DefaultFlag.PRICE, null);
                            manager.addRegion(region);
                            manager.save();

                            econ.withdrawPlayer(player.getName(), price);
                            ChatUtil.sendNotice(player, "Home successfully purchased!");

                            // Give them a home owner's manual :P
                            giveRuleBook(sender, sender.getName(), region.getParent().getId().replace("-district", ""));
                        } else {
                            throw new CommandException();
                        }
                    } catch (Exception ex) {
                        throw new CommandException("Failed to purchase this home.");
                    }
                } else {
                    ChatUtil.sendNotice(player, "If you would like to buy this home please use /home buy -y");
                }
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"sell"}, desc = "Sells your house", min = 0, max = 0, flags = "y")
        @CommandPermissions("aurora.auth.member")
        public void homeSellCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player player = (Player) sender;

                RegionManager manager = WG.getRegionManager(player.getWorld());
                ProtectedRegion region = manager.getRegionExact(getHome(player));
                if (region == null) throw new CommandException("You do not have a home in this world!");

                // Get the price and send it to the player
                double price = RegionUtil.getPrice(store, region, new BukkitWorld(player.getWorld()), false);

                if (price < 1) {
                    throw new CommandException("Your house cannot currently be sold.");
                }

                String priceString = ChatUtil.makeCountString(ChatColor.YELLOW,
                        econ.format(price), " " + econ.currencyNamePlural());
                ChatUtil.sendNotice(player, "Your home is worth: " + priceString + ".");

                // If they have used the flag y proceed to sell the house
                // otherwise inform them about how to sell their house
                if (args.hasFlag('y')) {
                    try {
                        String newName = System.currentTimeMillis() + "-s";
                        if (RegionUtil.renameRegion(manager, region.getId(), newName, true)) {

                            // Set the price flag's value and then resave the database
                            region = manager.getRegion(newName);
                            region.setFlag(DefaultFlag.PRICE, price * 1.1);
                            manager.addRegion(region);
                            manager.save();

                            econ.depositPlayer(player.getName(), price);
                            ChatUtil.sendNotice(player, "Home successfully sold!");
                        } else {
                            throw new CommandException();
                        }
                    } catch (Exception ex) {
                        throw new CommandException("Failed to sell your home.");
                    }
                } else {
                    ChatUtil.sendNotice(player, "If you would like to sell your home please use /home sell -y");
                }
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }


        @Command(aliases = {"admin"}, desc = "Admin Commands")
        @NestedCommand({HomeAdminCommands.class})
        public void homeAdminCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class HomeAdminCommands {

        @Command(aliases = {"create"}, usage = "[player] <district>", desc = "Create a home",
                min = 1, max = 2)
        @CommandPermissions({"aurora.home.admin.create"})
        public void createHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {

                Player admin = (Player) sender;
                String player, regionString, district;

                if (args.argsLength() == 2) {
                    player = args.getString(0).toLowerCase();
                    regionString = getHome(player);
                    district = args.getString(1).toLowerCase().replace("-district", "");
                } else {
                    player = null;
                    regionString = System.currentTimeMillis() + "-s";
                    district = args.getString(0).toLowerCase().replace("-district", "");
                }

                ProtectedRegion region;

                RegionManager manager = WG.getRegionManager(admin.getWorld());

                if (manager.hasRegion(regionString)) throw new CommandException("That player already has a home.");

                Selection sel = WE.getSelection(admin);
                if (sel == null) throw new CommandException("Select a region with WorldEdit first.");

                if (sel instanceof Polygonal2DSelection) {
                    Polygonal2DSelection polySel = (Polygonal2DSelection) sel;
                    int minY = polySel.getNativeMinimumPoint().getBlockY();
                    int maxY = polySel.getNativeMaximumPoint().getBlockY();
                    region = new ProtectedPolygonalRegion(regionString, polySel.getNativePoints(), minY, maxY);
                } else if (sel instanceof CuboidSelection) {
                    BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
                    BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
                    region = new ProtectedCuboidRegion(regionString, min, max);
                } else {
                    throw new CommandException("The type of region selected in WorldEdit is unsupported.");
                }

                if (player == null) {
                    region.setFlag(DefaultFlag.PRICE,
                            RegionUtil.getPrice(store, region, new BukkitWorld(admin.getWorld()), true));
                } else {
                    region.getOwners().addPlayer(player);
                }
                region.setPriority(10);
                ProtectedRegion districtRegion = manager.getRegion(district + "-district");
                if (districtRegion == null) throw new CommandException("Invalid district specified.");
                try {
                    region.setParent(districtRegion);
                } catch (ProtectedRegion.CircularInheritanceException e) {
                    throw new CommandException("Circular inheritance detected.");
                }

                manager.addRegion(region);
                try {
                    manager.save();
                } catch (ProtectionDatabaseException e) {
                    throw new CommandException("Failed to create the region: " + regionString + ".");
                }

                giveRuleBook(sender, player, district);

                ChatUtil.sendNotice(admin, "The home: " + regionString + " has been created successfully in "
                        + district + ".");
                if (player != null) {
                    ChatUtil.sendNotice(player, "A home has been created for you by: " + admin.getDisplayName() + ".");
                    log.info(admin.getName() + " created a home for: " + player
                            + " in the district: " + district + ".");
                }
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"pc"}, desc = "Price Checker",
                usage = "[player]",
                flags = "cf", min = 0, max = 1)
        @CommandPermissions({"aurora.home.admin.pc"})
        public void priceCheckHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Selection selection = WE.getSelection((Player) sender);

                if (selection == null) throw new CommandException("Select a region with WorldEdit first.");

                if (!(selection instanceof Polygonal2DSelection || selection instanceof CuboidSelection)) {
                    throw new CommandException("The type of region selected in WorldEdit is unsupported.");
                }

                Player player = args.argsLength() == 0 ? null : PlayerUtil.matchPlayerExactly(sender, args.getString(0));

                int size = (selection.getLength() * selection.getWidth()) / (16 * 16);

                CommandSender[] target = new CommandSender[]{
                        sender, player
                };

                ChatUtil.sendNotice(target, "Chunks: " + size);

                double p1 = size <= 4 ? size * 3750 : (size * 10000) + (size * (size / 2) * 10000);

                String chunkPrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(p1), " " + econ.currencyNamePlural());
                ChatUtil.sendNotice(target, "Chunk Price: " + chunkPrice + ".");

                // Block Price
                double p2 = 0;

                if (!args.hasFlag('f')) {
                    Region region = selection.getRegionSelector().getIncompleteRegion();
                    LocalWorld world = region.getWorld();

                    if (region instanceof CuboidRegion) {

                        // Doing this for speed
                        Vector min = region.getMinimumPoint();
                        Vector max = region.getMaximumPoint();

                        int minX = min.getBlockX();
                        int minY = min.getBlockY();
                        int minZ = min.getBlockZ();
                        int maxX = max.getBlockX();
                        int maxY = max.getBlockY();
                        int maxZ = max.getBlockZ();

                        for (int x = minX; x <= maxX; ++x) {
                            for (int y = minY; y <= maxY; ++y) {
                                for (int z = minZ; z <= maxZ; ++z) {
                                    Vector pt = new Vector(x, y, z);

                                    p2 += store.priceCheck(world.getBlockType(pt), world.getBlockData(pt));
                                }
                            }
                        }
                    } else {
                        throw new CommandException("Not yet supported.");
                    }
                }

                String housePrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(p2), " " + econ.currencyNamePlural());
                ChatUtil.sendNotice(target, "Block Price: " + housePrice + ".");

                double total = p1 + p2;
                if (args.hasFlag('c')) {
                    String commission = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(total * .1), " " + econ.currencyNamePlural());

                    ChatUtil.sendNotice(target, "Commission change: " + commission);
                    total += total * .1;
                }

                String totalPrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(total), " " + econ.currencyNamePlural());
                ChatUtil.sendNotice(target, "Total Price: " + totalPrice + ".");
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"flag"}, usage = "<player> <flag>", desc = "Flag a home",
                min = 2)
        @CommandPermissions({"aurora.home.admin.flag"})
        public void flagHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                String player = args.getString(0);
                String flag = args.getJoinedStrings(1);

                Bukkit.dispatchCommand(sender, "rg flag " + getHome(player) + " " + flag);
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"move"}, usage = "<player> <newdistrict>", desc = "Move a home",
                min = 2, max = 2)
        @CommandPermissions({"aurora.home.admin.move"})
        public void moveHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player admin = (Player) sender;
                String player = args.getString(0).toLowerCase();
                String district = args.getString(1).toLowerCase().replace("-district", "");

                RegionManager manager = WG.getRegionManager(admin.getWorld());
                ProtectedRegion existing = manager.getRegionExact(getHome(player));
                if (existing == null) throw new CommandException("That player doesn't have a home.");
                Selection sel = WE.getSelection(admin);
                if (sel == null) throw new CommandException("Select a region with WorldEdit first.");

                ProtectedRegion region;

                // Detect the type of region from WorldEdit
                if (sel instanceof Polygonal2DSelection) {
                    Polygonal2DSelection polySel = (Polygonal2DSelection) sel;
                    int minY = polySel.getNativeMinimumPoint().getBlockY();
                    int maxY = polySel.getNativeMaximumPoint().getBlockY();
                    region = new ProtectedPolygonalRegion(getHome(player), polySel.getNativePoints(), minY, maxY);
                } else if (sel instanceof CuboidSelection) {
                    BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
                    BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
                    region = new ProtectedCuboidRegion(getHome(player), min, max);
                } else {
                    throw new CommandException("The type of region selected in WorldEdit is unsupported.");
                }

                region.setMembers(existing.getMembers());
                region.setOwners(existing.getOwners());
                region.setFlags(existing.getFlags());
                region.setPriority(existing.getPriority());
                ProtectedRegion districtRegion = manager.getRegion(district + "-district");
                if (districtRegion == null) throw new CommandException("Invalid district specified.");
                try {
                    region.setParent(districtRegion);
                } catch (ProtectedRegion.CircularInheritanceException e) {
                    throw new CommandException("Circular inheritance detected.");
                }

                manager.addRegion(region);
                try {
                    manager.save();
                } catch (ProtectionDatabaseException e) {
                    throw new CommandException("Failed to create a home for: " + player + ".");
                }

                giveRuleBook(sender, player, district);

                ChatUtil.sendNotice(admin, "The player: " + player + "'s house has been moved to: " + district + ".");
                ChatUtil.sendNotice(player, "Your home has been moved for you by: " + admin.getDisplayName() + ".");
                log.info(admin.getName() + " moved a home for: " + player + " into the district: " + district + ".");
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"remove"}, usage = "<player>", desc = "Remove a home", min = 1, max = 1)
        @CommandPermissions({"aurora.home.admin.remove"})
        public void removeHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {
                Player admin = (Player) sender;
                String player = args.getString(0).toLowerCase();

                RegionManager manager = WG.getRegionManager(admin.getWorld());
                ProtectedRegion region = manager.getRegionExact(getHome(player));
                if (region == null) throw new CommandException("That player doesn't have a home.");

                manager.removeRegion(region.getId());
                try {
                    manager.save();
                } catch (ProtectionDatabaseException e) {
                    throw new CommandException("Failed to remove the home of: " + player + ".");
                }
                ChatUtil.sendNotice(admin, "The player: " + player + "'s house has been removed.");
                ChatUtil.sendNotice(player, "Your home has been removed by: " + admin.getDisplayName() + ".");
                log.info(admin.getName() + " deleted the player: " + player + "'s home.");
            } else {
                throw new CommandException("You must be a player to use this command.");
            }
        }

        @Command(aliases = {"help"}, desc = "Admin Help", min = 0, max = 0)
        @CommandPermissions({"aurora.home.admin.help"})
        public void adminHelpCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) return;

            ((Player) sender).getInventory().addItem(BookUtil.Help.Admin.housing());
        }
    }

    private String getHome(String player) {

        return player.toLowerCase() + "'s-house";
    }

    private String getHome(Player player) {

        return getHome(player.getName());
    }

    private District getDistrict(String district) {

        district = district.toLowerCase();

        switch (district) {
            case "carpe-diem":
                return District.CARPE_DIEM;
            case "glacies-mare":
                return District.GLACIES_MARE;
            case "oblitus":
                return District.OBLITUS;
            case "vineam":
                return District.VINEAM;
        }
        return District.GLOBAL;
    }

    private boolean giveRuleBook(CommandSender sender, String player, String district) {

        district = district.toLowerCase();
        District aDistrict = getDistrict(district);

        return aDistrict != District.GLOBAL && giveRuleBook(sender, player, aDistrict);
    }

    private boolean giveRuleBook(CommandSender sender, String player, District district) {

        ItemStack ruleBook;
        switch (district) {
            case CARPE_DIEM:
                ruleBook = BookUtil.Rules.BuildingCode.carpeDiem();
                break;
            case GLACIES_MARE:
                ruleBook = BookUtil.Rules.BuildingCode.glaciesMare();
                break;
            case OBLITUS:
                ruleBook = BookUtil.Rules.BuildingCode.obiluts();
                break;
            case VINEAM:
                ruleBook = BookUtil.Rules.BuildingCode.vineam();
                break;
            default:
                return false;
        }

        Player tPlayer;
        try {
            tPlayer = PlayerUtil.matchPlayerExactly(sender, player);
        } catch (CommandException ex) {
            tPlayer = null;
        }

        if (tPlayer == null) return false;
        tPlayer.getInventory().addItem(BookUtil.Rules.BuildingCode.server());
        tPlayer.getInventory().addItem(ruleBook);
        return true;
    }

    public class TeleportCommands {

        @Command(aliases = {"set"}, desc = "Set your home")
        public void setHomeLoc(CommandContext args, CommandSender sender) {

            ChatUtil.sendNotice(sender, "To set your home, sleep in a bed during the night.");
        }

        @Command(aliases = {"teleport", "tp"}, desc = "Go to your home")
        public void teleportHome(CommandContext args, CommandSender sender) {

            ChatUtil.sendNotice(sender, "To go to your home, throw an ender pearl at your feet.");
        }

    }
}
