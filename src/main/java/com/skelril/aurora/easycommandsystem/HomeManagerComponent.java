package com.skelril.aurora.easycommandsystem;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.databases.RegionDBUtil;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.District;
import com.skelril.aurora.util.BookUtil;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Home Manager", desc = "Home ECS")
@Depend(plugins = {"WorldEdit", "WorldGuard"})
public class HomeManagerComponent extends BukkitComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private WorldEditPlugin WE;
    private WorldGuardPlugin WG;

    @Override
    public void enable() {

        registerCommands(Commands.class);
        setUpWorldEdit();
        setUpWorldGauard();
    }

    private void setUpWorldEdit() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldEdit");

        // WorldEdit may not be loaded
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) {
            return;
        }

        this.WE = (WorldEditPlugin) plugin;
    }

    private void setUpWorldGauard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return;
        }

        this.WG = (WorldGuardPlugin) plugin;
    }

    public class Commands {

        @Command(aliases = {"home"}, desc = "Home Manager")
        @NestedCommand({NestedCommands.class})
        public void homeCmd(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {

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

        @Command(aliases = {"admin"}, desc = "Admin Commands")
        @NestedCommand({HomeAdminCommands.class})
        public void homeAdminCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class HomeAdminCommands {

        @Command(aliases = {"create"}, usage = "<player> <district>", desc = "Create a home",
                min = 2, max = 2)
        @CommandPermissions({"aurora.home.admin.create"})
        public void createHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (sender instanceof Player) {

                Player admin = (Player) sender;
                String player = args.getString(0).toLowerCase();
                String district = args.getString(1).toLowerCase().replace("-district", "");

                ProtectedRegion region;

                RegionManager manager = WG.getRegionManager(admin.getWorld());

                if (manager.hasRegion(getHome(player))) throw new CommandException("That player already has a home.");

                Selection sel = WE.getSelection(admin);
                if (sel == null) throw new CommandException("Select a region with WorldEdit first.");

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

                region.getOwners().addPlayer(player);
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
                    throw new CommandException("Failed to create a home for: " + player + ".");
                }

                giveRuleBook(sender, player, district);

                ChatUtil.sendNotice(admin, "A home has been created successfully for: "
                        + player + " in the district: " + district + ".");
                ChatUtil.sendNotice(player, "A home has been created for you by: " + admin.getDisplayName() + ".");
                log.info(admin.getName() + " created a home for: " + player + " in the district: " + district + ".");
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

    private boolean giveRuleBook(CommandSender sender, String player, String district) {

        district = district.toLowerCase();

        switch (district) {
            case "carpe-diem":
                return giveRuleBook(sender, player, District.CARPE_DIEM);
            case "glacies-mare":
                return giveRuleBook(sender, player, District.GLACIES_MARE);
            case "oblitus":
                return giveRuleBook(sender, player, District.OBLITUS);
            case "vineam":
                return giveRuleBook(sender, player, District.VINEAM);
        }
        return false;
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
