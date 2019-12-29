/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.homes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.commands.AsyncCommandHelper;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.RegionGroupFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.util.DomainInputResolver;
import com.sk89q.worldguard.protection.util.DomainInputResolver.UserLocatorPolicy;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.District;
import gg.packetloss.grindstone.economic.store.MarketComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.RegionUtil;
import gg.packetloss.grindstone.util.item.BookUtil;
import gg.packetloss.grindstone.util.region.RegionContainerClearer;
import gg.packetloss.grindstone.util.region.RegionValueEvaluator;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang3.Validate;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Home Manager", desc = "Home ECS")
@Depend(plugins = {"WorldEdit", "WorldGuard"}, components = MarketComponent.class)
public class HomeManagerComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private WorldEditPlugin WE;
    private WorldGuardPlugin WG;
    private Economy econ;

    private static Map<BaseBlock, BaseBlock> blockMapping = new HashMap<>();

    static {
        blockMapping.put(new BaseBlock(BlockID.GRASS), new BaseBlock(BlockID.STAINED_CLAY, 11));
        blockMapping.put(new BaseBlock(BlockID.SAND), new BaseBlock(BlockID.STAINED_CLAY, 3));
    }

    private static PlotOutliner outliner = new PlotOutliner(blockMapping, BlockType::isNaturalTerrainBlock);

    @Override
    public void enable() {

        registerCommands(Commands.class);
        setUpWorldEdit();
        setUpWorldGuard();
        setUpEconomy();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    private void setUpWorldEdit() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldEdit");

        // WorldEdit may not be loaded
        if (!(plugin instanceof WorldEditPlugin)) {
            return;
        }

        this.WE = (WorldEditPlugin) plugin;
    }

    private void setUpWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (!(plugin instanceof WorldGuardPlugin)) {
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

    public static boolean isEntityInPlayersHome(Player player, Entity entity, RegionManager manager) {
        for (ProtectedRegion aRegion : manager.getApplicableRegions(entity.getLocation())) {
            if (isPlayerHouse(aRegion, new BukkitPlayer(WorldGuardPlugin.inst(), player))) {
                return true;
            }
        }
        return false;
    }

    public boolean isEntityInPlayersHome(Player player, Entity entity) {
        RegionManager manager = WG.getRegionManager(entity.getWorld());
        return isEntityInPlayersHome(player, entity, manager);
    }

    private boolean isInAnyPlayerHome(Location location, RegionManager manager) {
        for (ProtectedRegion aRegion : manager.getApplicableRegions(location)) {
            if (isHouse(aRegion)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInAnyPlayerHome(Location location) {
        RegionManager manager = WG.getRegionManager(location.getWorld());
        return isInAnyPlayerHome(location, manager);
    }

    @EventHandler(ignoreCancelled = true)
    public void playerPreProcess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (event.getMessage().contains("%cur-house%")) {
            RegionManager manager = WG.getRegionManager(player.getWorld());

            for (ProtectedRegion region : manager.getApplicableRegions(player.getLocation())) {
                if (region.getId().endsWith("-s") && region.getFlag(DefaultFlag.PRICE) != null) {
                    event.setMessage(event.getMessage().replace("%cur-house%", region.getId()));
                    ChatUtil.sendNotice(player, "Injected region: " + region.getId() + " as current house.");
                    return;
                }
            }
        }
    }

    public class Commands {
        @Command(aliases = {"/home"}, desc = "Home Manager")
        @NestedCommand({NestedCommands.class})
        public void homeCmd(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {
        private final String CURRENT_OWNED_CHUNK_COUNT_FAILED = "We could not determine how many chunks you own currently!";

        @Command(aliases = {"info"}, usage = "", desc = "View information about your home",
                min = 0, max = 0)
        @CommandPermissions({"aurora.home.self.info"})
        public void infoHomeCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(player.getWorld());
            ProtectedRegion home = requireStoodInHome(player, manager);

            StringBuilder builtInfo = new StringBuilder();
            builtInfo.append(ChatColor.YELLOW + "Home Owner: ").append(sender.getName()).append(", ");
            builtInfo.append(ChatColor.GRAY + "District: ").append(getHomeDistrict(home).toProperName());

            if (home.getMembers().size() > 0) {
                builtInfo.append("\n");

                String friends = home.getMembers().toUserFriendlyString(WG.getProfileCache());
                builtInfo.append(ChatColor.BLUE + "Friends: ").append(friends);
            }

            boolean hasFlags = false;
            final StringBuilder s = new StringBuilder("\n" + ChatColor.BLUE + "Properties: ");
            for (Flag<?> flag : DefaultFlag.getFlags()) {
                Object val = home.getFlag(flag);
                Object group = null;

                if (val == null) {
                    continue;
                }

                if (hasFlags) {
                    s.append(", ");
                }

                RegionGroupFlag groupFlag = flag.getRegionGroupFlag();
                if (groupFlag != null) {
                    group = home.getFlag(groupFlag);
                }

                if (group == null) {
                    s.append(flag.getName()).append(": ").append(val);
                } else {
                    s.append(flag.getName()).append(" -g ").append(group).append(": ").append(val);
                }

                hasFlags = true;
            }
            if (hasFlags) {
                builtInfo.append(s.toString());
            }

            ChatUtil.sendNotice(sender,ChatColor.RESET, builtInfo.toString());
         }

        @Command(aliases = {"stats"}, usage = "", desc = "View stats about your homes",
                min = 0, max = 0)
        @CommandPermissions({"aurora.home.self.info"})
        public void statsHomeCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(player.getWorld());
            List<ProtectedRegion> houses = RegionUtil.getHouses(new BukkitPlayer(WG, player), manager);

            Optional<Integer> optOwnedChunks = RegionUtil.sumChunks(houses);
            int ownedChunks = optOwnedChunks.orElseThrow(() -> {
                return new CommandException(CURRENT_OWNED_CHUNK_COUNT_FAILED);
            });

            String builtInfo = ChatColor.YELLOW + "You own " + houses.size() + " houses." +
                    "\n    Total Chunks: " + ownedChunks;
            ChatUtil.sendNotice(sender,ChatColor.RESET, builtInfo);
        }

        @Command(aliases = {"pcchunks"}, usage = "<# of chunks>", desc = "Get the cost of buying x number of chunks",
                min = 1, max = 1)
        @CommandPermissions({"aurora.home.self.info"})
        public void buyChunk(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(player.getWorld());
            List<ProtectedRegion> houses = RegionUtil.getHouses(new BukkitPlayer(WG, player), manager);

            Optional<Integer> optOwnedChunks = RegionUtil.sumChunks(houses);
            int ownedChunks = optOwnedChunks.orElseThrow(() -> {
                return new CommandException(CURRENT_OWNED_CHUNK_COUNT_FAILED);
            });

            int acquisitionSize = args.getInteger(0);
            int newChunks = optOwnedChunks.get() + acquisitionSize;

            double price = RegionUtil.calcChunkPrice(newChunks) - RegionUtil.calcChunkPrice(ownedChunks);

            String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(price), "");

            ChatUtil.sendNotice(sender, acquisitionSize + " additional chunks will cost: " + priceString);
        }

        @Command(aliases = {"addplayer"}, usage = "<players...>", flags = "n", desc = "Add a player to your home",
                min = 1)
        @CommandPermissions({"aurora.home.self.add"})
        public void addMemberToHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            RegionManager manager = WG.getRegionManager(player.getWorld());
            ProtectedRegion region = requireStoodInHome(player, manager);

            // Resolve members asynchronously
            DomainInputResolver resolver = new DomainInputResolver(
                WG.getProfileService(), args.getParsedPaddedSlice(0, 0));
            resolver.setLocatorPolicy(args.hasFlag('n') ? UserLocatorPolicy.NAME_ONLY : UserLocatorPolicy.UUID_ONLY);

            // Then add it to the members
            ListenableFuture<DefaultDomain> future = Futures.transform(
                WG.getExecutorService().submit(resolver),
                resolver.createAddAllFunction(region.getMembers()));

            AsyncCommandHelper.wrap(future, WG, sender)
                .registerWithSupervisor("Adding members to your home")
                .sendMessageAfterDelay("(Please wait... querying player names...)")
                .thenRespondWith("Home updated with new members.", "Failed to add new members");
        }

        @Command(aliases = {"removeplayer"}, usage = "<players...>", flags = "n", desc = "Remove a player from your home",
                min = 1)
        @CommandPermissions({"aurora.home.self.remove"})
        public void removeMemberToHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            RegionManager manager = WG.getRegionManager(player.getWorld());
            ProtectedRegion region = requireStoodInHome(player, manager);

            // Resolve members asynchronously
            DomainInputResolver resolver = new DomainInputResolver(
                WG.getProfileService(), args.getParsedPaddedSlice(0, 0));
            resolver.setLocatorPolicy(args.hasFlag('n') ? UserLocatorPolicy.NAME_ONLY : UserLocatorPolicy.UUID_AND_NAME);

            // Then remove it from the members
            ListenableFuture<?> future = Futures.transform(
                WG.getExecutorService().submit(resolver),
                resolver.createRemoveAllFunction(region.getMembers()));

            AsyncCommandHelper.wrap(future, WG, sender)
                .registerWithSupervisor("Removing members from your home")
                .sendMessageAfterDelay("(Please wait... querying player names...)")
                .thenRespondWith("Home updated with members removed.", "Failed to remove members");
        }

        @Command(aliases = {"flag"}, usage = "<flag>", desc = "Flag a home",
                min = 1)
        @CommandPermissions({"aurora.home.self.flag"})
        public void flagHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);
            ProtectedRegion region = requireStoodInHome(player, WG.getRegionManager(player.getWorld()));

            player.performCommand("rg flag " + region.getId() + " " + args.getJoinedStrings(0));
        }

        @Command(aliases = {"rules"}, usage = "[district]", desc = "District Rules", min = 0, max = 1)
        public void homeRuleCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            District district;
            if (args.argsLength() == 0) {
                ProtectedRegion region = requireStoodInHome(player, WG.getRegionManager(player.getWorld()));
                district = getHomeDistrict(region);
            } else {
                district = District.fromName(args.getString(0)).orElseThrow(() -> {
                    return new CommandException("No district found by the given name.");
                });
            }

            if (!giveRuleBook(sender, sender.getName(), district)) {
                throw new CommandException("No district by that name found in this world.");
            }
        }

        @Command(aliases = {"buy"}, desc = "Buy a house", min = 0, max = 0, flags = "y")
        @CommandPermissions("aurora.auth.member")
        public void homeBuyCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(player.getWorld());
            ProtectedRegion region = null;

            for (ProtectedRegion aRegion : manager.getApplicableRegions(player.getLocation())) {
                if (aRegion.getId().endsWith("-s")) {
                    region = aRegion;
                    break;
                }
            }

            if (region == null) {
                throw new CommandException("No plots for purchase were found where you're standing." +
                        "\nAvailable plots will have a blue outline around them.");
            }

            // Get the price and send it to the player
            Double price = region.getFlag(DefaultFlag.PRICE);
            if (price == null) {
                throw new CommandException("This house cannot currently be bought.");
            }

            Optional<Integer> optOwnedChunks = RegionUtil.sumChunks(RegionUtil.getHouses(new BukkitPlayer(WG, player), manager));
            int ownedChunks = optOwnedChunks.orElseThrow(() -> {
                return new CommandException(CURRENT_OWNED_CHUNK_COUNT_FAILED);
            });

            Optional<Integer> optRequestedChunks = RegionUtil.countChunks(region);
            int requestedChunks = optRequestedChunks.orElseThrow(() -> {
                return new CommandException("We could not determine how many chunks you are trying to buy!");
            });

            int newChunks = ownedChunks + requestedChunks;

            price += RegionUtil.calcChunkPrice(newChunks) - RegionUtil.calcChunkPrice(ownedChunks);

            String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(price), "");
            ChatUtil.sendNotice(player, "This property will cost you: " + priceString + ".");

            // If they have used the flag y proceed to buy the house
            // otherwise inform them about how to buy the house
            if (args.hasFlag('y')) {
                if (!econ.has(player.getName(), price)) {
                    throw new CommandException("Sorry, you cannot currently afford this house.");
                }
                try {
                    String homeName = getHomeName(player, manager);
                    if (RegionUtil.renameRegion(manager, region.getId(), homeName, true)) {

                        region = manager.getRegion(homeName);
                        region.getOwners().addPlayer(new BukkitPlayer(WG, player));
                        region.setFlag(DefaultFlag.PRICE, null);
                        manager.addRegion(region);
                        manager.save();

                        outliner.revert(player.getWorld(), region);

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
        }

        @Command(aliases = {"sell"}, desc = "Sells your house", min = 0, max = 0, flags = "y")
        @CommandPermissions("aurora.auth.member")
        public void homeSellCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(player.getWorld());
            ProtectedRegion region = requireStoodInHome(player, manager);

            // Get the price and send it to the player
            CompletableFuture<Optional<Double>> optFutureBlockPrice = RegionUtil.calcBlockPrice(
                    region,
                    player.getWorld()
            );
            optFutureBlockPrice.thenAccept((optBlockPrice) -> {
                server.getScheduler().runTask(inst, () -> {
                    if (optBlockPrice.isEmpty()) {
                        ChatUtil.sendError(player, "Your house cannot currently be sold.");
                        return;
                    }

                    double blockPrice = optBlockPrice.get();
                    List<ProtectedRegion> houses = RegionUtil.getHouses(new BukkitPlayer(WG, player), manager);
                    Optional<Integer> optPreviousChunks = RegionUtil.sumChunks(houses);
                    if (optPreviousChunks.isEmpty()) {
                        ChatUtil.sendError(player, CURRENT_OWNED_CHUNK_COUNT_FAILED);
                        return;
                    }

                    Optional<Integer> optSellingChunks = RegionUtil.countChunks(region);
                    if (optSellingChunks.isEmpty()) {
                        ChatUtil.sendError(player, "We could not determine how many chunks are being sold!");
                        return;
                    }

                    int chunks = optPreviousChunks.get();
                    int newChunks = optPreviousChunks.get() - optSellingChunks.get();

                    double price = blockPrice + (RegionUtil.calcChunkPrice(chunks) - RegionUtil.calcChunkPrice(newChunks)) * .9;

                    String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(price), "");
                    ChatUtil.sendNotice(player, "Your will get: " + priceString + ".");

                    // If they have used the flag y proceed to sell the house
                    // otherwise inform them about how to sell their house
                    if (args.hasFlag('y')) {
                        try {
                            String newName = System.currentTimeMillis() + "-s";
                            if (RegionUtil.renameRegion(manager, region.getId(), newName, true)) {

                                // Set the price flag's value and then resave the database
                                ProtectedRegion newRegion = manager.getRegion(newName);
                                newRegion.setFlag(DefaultFlag.PRICE, blockPrice * 1.1);
                                manager.addRegion(newRegion);
                                manager.save();

                                outliner.outline(player.getWorld(), newRegion);

                                econ.depositPlayer(player.getName(), price);
                                ChatUtil.sendNotice(player, "Home successfully sold!");
                            } else {
                                throw new CommandException();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            ChatUtil.sendNotice(player, "Failed to sell your home.");
                        }
                    } else {
                        ChatUtil.sendNotice(player, "If you would like to sell your home please use /home sell -y");
                    }
                });
            });
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

            Player admin = PlayerUtil.checkPlayer(sender);
            String player, regionString, district;

            RegionManager manager = WG.getRegionManager(admin.getWorld());

            if (args.argsLength() == 2) {
                player = args.getString(0).toLowerCase();
                regionString = getHomeName(player, manager);
                district = args.getString(1).toLowerCase().replace("-district", "");
            } else {
                player = null;
                regionString = System.currentTimeMillis() + "-s";
                district = args.getString(0).toLowerCase().replace("-district", "");
            }

            ProtectedRegion region;

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

            CompletableFuture<Optional<Double>> blockPrice;
            if (player == null) {
                blockPrice = RegionUtil.calcBlockPrice(
                        region,
                        admin.getWorld()
                );
            } else {
                blockPrice = CompletableFuture.completedFuture(Optional.empty());
            }

            blockPrice.thenAccept((optBlockPrice) -> {
                server.getScheduler().runTask(inst, () -> {
                    if (optBlockPrice.isPresent()) {
                        Validate.isTrue(player == null);
                        region.setFlag(DefaultFlag.PRICE, optBlockPrice.get());
                    } else {
                        Validate.notNull(player);
                        region.getOwners().addPlayer(player);
                    }

                    region.setPriority(10);
                    ProtectedRegion districtRegion = manager.getRegion(district + "-district");
                    if (districtRegion == null) {
                        ChatUtil.sendError(sender, "Invalid district specified.");
                        return;
                    }

                    try {
                        region.setParent(districtRegion);
                    } catch (ProtectedRegion.CircularInheritanceException e) {
                        ChatUtil.sendError(sender, "Circular inheritance detected.");
                    }

                    manager.addRegion(region);
                    try {
                        manager.save();

                        outliner.outline(admin.getWorld(), region);
                    } catch (StorageException e) {
                        e.printStackTrace();
                        ChatUtil.sendError(sender, "Failed to create the region: " + regionString + ".");
                    }

                    giveRuleBook(sender, player, district);

                    ChatUtil.sendNotice(admin, "The home: " + regionString + " has been created successfully in "
                            + district + ".");
                    if (player != null) {
                        ChatUtil.sendNotice(player, "A home has been created for you by: " + admin.getDisplayName() + ".");
                        log.info(admin.getName() + " created a home for: " + player
                                + " in the district: " + district + ".");
                    }
                });
            });
        }

        @Command(aliases = {"updateprice"}, usage = "", desc = "Re evaluates all purchasable plots " +
                "in the location the admin is standing",
                min = 0, max = 0)
        @CommandPermissions({"aurora.home.admin.updateprice"})
        public void updateHomePriceCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player admin = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(admin.getWorld());

            List<CompletableFuture<Void>> regionsComplete = new ArrayList<>();

            for (ProtectedRegion region : manager.getApplicableRegions(admin.getLocation())) {
                if (region.getId().endsWith("-s") && region.getFlag(DefaultFlag.PRICE) != null) {
                    CompletableFuture<Optional<Double>> blockPrice = RegionUtil.calcBlockPrice(
                            region,
                            admin.getWorld()
                    );

                    regionsComplete.add(blockPrice.thenAccept((optBlockPrice) -> {
                        server.getScheduler().runTask(inst, () -> {
                            if (optBlockPrice.isEmpty()) {
                                ChatUtil.sendError(sender, "Couldn't recompute block price for " + region.getId());
                                return;
                            }
                            region.setFlag(DefaultFlag.PRICE, optBlockPrice.get());
                            outliner.outline(admin.getWorld(), region);
                        });
                    }));
                }
            }

            CompletableFuture.allOf(regionsComplete.toArray(new CompletableFuture[0])).thenAccept((ignored) -> {
                server.getScheduler().runTask(inst, () -> {
                    try {
                        manager.save();
                    } catch (StorageException e) {
                        e.printStackTrace();
                        ChatUtil.sendError(sender, "Failed to save the database.");
                        return;
                    }

                    ChatUtil.sendNotice(admin, "Region(s) recalculated successfully.");
                });
            });
        }

        @Command(aliases = {"pc"}, desc = "Price Checker",
                usage = "[player]",
                flags = "cf", min = 0, max = 1)
        @CommandPermissions({"aurora.home.admin.pc"})
        public void priceCheckHomeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player sendingPlayer = PlayerUtil.checkPlayer(sender);

            Selection selection = WE.getSelection(sendingPlayer);

            if (selection == null || selection.getRegionSelector().isDefined()) {
                throw new CommandException("Select a complete region with WorldEdit first.");
            }

            Player player = args.argsLength() == 0 ? null : InputUtil.PlayerParser.matchPlayerExactly(sender, args.getString(0));

            Region region = selection.getRegionSelector().getIncompleteRegion();
            Optional<Integer> optChunkCount = RegionUtil.countChunks(region);
            if (optChunkCount.isEmpty()) {
                throw new CommandException("Region type unsupported.");
            }

            List<CommandSender> target = Arrays.asList(
                    sender, player
            );

            double size = optChunkCount.get();
            ChatUtil.sendNotice(target, "Chunks: " + size);

            // Block Price
            World world = region.getWorld();

            CompletableFuture<Optional<Double>> blockPrice = RegionUtil.calcBlockPrice(region, ((BukkitWorld) world).getWorld());
            blockPrice.thenAccept((optBlockPrice) -> {
                server.getScheduler().runTask(inst, () -> {
                    double p1 = RegionUtil.calcChunkPrice(size);
                    String chunkPrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(p1), "");
                    ChatUtil.sendNotice(target, "Chunk Price: " + chunkPrice + ".");

                    double p2;
                    if (optBlockPrice.isPresent()) {
                        p2 = optBlockPrice.get();

                        String housePrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(p2), "");
                        ChatUtil.sendNotice(target, "Block Price: " + housePrice + ".");
                    } else {
                        p2 = 0;

                        ChatUtil.sendError(target, "Block Price: FAILED.");
                    }

                    double total = p1 + p2;
                    if (args.hasFlag('c')) {
                        String commission = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(total * .1), "");

                        ChatUtil.sendNotice(target, "Commission change: " + commission);
                        total *= 1.1;
                    }

                    String totalPrice = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(total), "");
                    ChatUtil.sendNotice(target, "Total Price: " + totalPrice + ".");
                });
            });
        }

        @Command(aliases = {"listhomes"}, desc = "List homes for a player",
                usage = "<player>",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.home.admin.listhomes"})
        public void listHomesCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player admin = PlayerUtil.checkPlayer(sender);

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args.getString(0));
            if (offlinePlayer == null) {
                throw new CommandException("No player found by that name.");
            }

            RegionManager manager = WG.getRegionManager(admin.getWorld());
            RegionUtil.getHouseStream(WG.wrapOfflinePlayer(offlinePlayer), manager).forEach((house) -> {
                ChatUtil.sendNotice(admin, house.getId());
            });
        }

        @Command(aliases = {"viewvalue"}, usage = "", desc = "", min = 0, max = 0)
        @CommandPermissions({"aurora.home.admin.viewvalue"})
        public void viewValueHomeCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player admin = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(admin.getWorld());

            RegionValueEvaluator evaluator = new RegionValueEvaluator(true);

            for (ProtectedRegion region : manager.getApplicableRegions(admin.getLocation())) {
                if (!isHouse(region)) {
                    continue;
                }

                Optional<Region> optConvertedRegion = RegionUtil.convert(region);
                if (optConvertedRegion.isEmpty()) {
                    continue;
                }

                ChatUtil.sendNotice(admin, "Found: " + region.getId());

                evaluator.walkRegion(optConvertedRegion.get(), admin.getWorld()).thenAccept((report) -> {
                    server.getScheduler().runTask(inst, () -> {
                        ChatUtil.sendNotice(admin, "Report for: " + region.getId());
                        ChatUtil.sendNotice(admin, "Item price: " + ChatUtil.TWO_DECIMAL_FORMATTER.format(report.getItemPriceCurrentState()));
                        ChatUtil.sendNotice(admin, "Maximum item value: " + ChatUtil.TWO_DECIMAL_FORMATTER.format(report.getMaximumItemValue()));
                        ChatUtil.sendNotice(admin, "Block price: " + ChatUtil.TWO_DECIMAL_FORMATTER.format(report.getBlockPrice()));
                        ChatUtil.sendNotice(admin, "Auto sell price: " + ChatUtil.TWO_DECIMAL_FORMATTER.format(report.getAutoSellPrice()));
                    });
                });
            }
        }

        @Command(aliases = {"walkinactive"}, usage = "", desc = "", min = 0, max = 0)
        @CommandPermissions({"aurora.home.admin.walkinactive"})
        public void walkInactiveHomeCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player admin = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(admin.getWorld());

            for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
                ProtectedRegion region = entry.getValue();
                if (!isHouse(region)) {
                    continue;
                }

                Optional<Region> optConvertedRegion = RegionUtil.convert(region);
                if (optConvertedRegion.isEmpty()) {
                    continue;
                }

                boolean recentlySeen = false;
                for (UUID playerID : region.getOwners().getUniqueIds()) {
                    long lastSeen = Bukkit.getOfflinePlayer(playerID).getLastPlayed();
                    long timeSinceLastSeen = System.currentTimeMillis() - lastSeen;
                    if (timeSinceLastSeen <= TimeUnit.DAYS.toMillis(365)) {
                        recentlySeen = true;
                    }
                }

                if (recentlySeen) {
                    continue;
                }

                ChatUtil.sendNotice(admin, "Found Inactive Home: " + region.getId());
            }
        }

        @Command(aliases = {"purgeinactive"}, usage = "", desc = "", flags = "vy", min = 0, max = 0)
        @CommandPermissions({"aurora.home.admin.purgeinactive"})
        public void purgeInactiveHomeCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player admin = PlayerUtil.checkPlayer(sender);

            RegionManager manager = WG.getRegionManager(admin.getWorld());

            RegionValueEvaluator evaluator = new RegionValueEvaluator(true);
            RegionContainerClearer containerClearer = new RegionContainerClearer();

            for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
                ProtectedRegion region = entry.getValue();
                if (!isHouse(region)) {
                    continue;
                }

                Optional<Region> optConvertedRegion = RegionUtil.convert(region);
                if (optConvertedRegion.isEmpty()) {
                    continue;
                }

                boolean recentlySeen = false;
                for (UUID playerID : region.getOwners().getUniqueIds()) {
                    long lastSeen = Bukkit.getOfflinePlayer(playerID).getLastPlayed();
                    long timeSinceLastSeen = System.currentTimeMillis() - lastSeen;
                    if (timeSinceLastSeen <= TimeUnit.DAYS.toMillis(365)) {
                        recentlySeen = true;
                    }
                }

                if (recentlySeen) {
                    continue;
                }

                ChatUtil.sendNotice(admin, "Found Inactive Home: " + region.getId());

                evaluator.walkRegion(optConvertedRegion.get(), admin.getWorld()).thenAccept((report) -> {
                    server.getScheduler().runTask(inst, () -> {
                        int ownerSize = region.getOwners().getPlayerDomain().size();
                        if (ownerSize > 1) {
                            ChatUtil.sendError(sender, region.getId() + " has multiple owners.");
                            return;
                        }

                        OfflinePlayer owner;
                        if (region.getOwners().getUniqueIds().size() == 1) {
                            owner = Bukkit.getOfflinePlayer(region.getOwners().getUniqueIds().iterator().next());
                        } else {
                            owner = Bukkit.getOfflinePlayer(region.getOwners().getPlayers().iterator().next());
                        }

                        if (owner == null) {
                            ChatUtil.sendError(sender, region.getId() + " offline owner not found.");
                            return;
                        }

                        List<ProtectedRegion> houses = RegionUtil.getHouses(WG.wrapOfflinePlayer(owner), manager);
                        Optional<Integer> optPreviousChunks = RegionUtil.sumChunks(houses);
                        if (optPreviousChunks.isEmpty()) {
                            return;
                        }

                        Optional<Integer> optSellingChunks = RegionUtil.countChunks(region);
                        if (optSellingChunks.isEmpty()) {
                            ChatUtil.sendError(sender, region.getId() + " we could not determine how many chunks are being sold.");
                            return;
                        }

                        int chunks = optPreviousChunks.get();
                        int newChunks = optPreviousChunks.get() - optSellingChunks.get();

                        double autoSellPrice = report.getAutoSellPrice();
                        double chunkPrice = (RegionUtil.calcChunkPrice(chunks) - RegionUtil.calcChunkPrice(newChunks)) * .9;
                        double price = autoSellPrice + chunkPrice;

                        String priceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(price), "");
                        ChatUtil.sendNotice(sender, "Owner will receive: " + priceString + " for: " + region.getId() + ".");

                        if (args.hasFlag('v')) {
                            ChatUtil.sendNotice(sender, "  Total Houses: " + houses.size());
                            ChatUtil.sendNotice(sender, "  Chunks from: " + chunks + " to: " + newChunks);
                            String autoSellPriceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(autoSellPrice), "");
                            ChatUtil.sendNotice(sender, "  Block/Item: " + autoSellPriceString);
                            String chunkPriceString = ChatUtil.makeCountString(ChatColor.YELLOW, econ.format(chunkPrice), "");
                            ChatUtil.sendNotice(sender, "  Chunk: " + chunkPriceString);
                        }

                        // If they have used the flag y proceed to sell the house
                        // otherwise inform them about how to sell their house
                        if (args.hasFlag('y')) {
                            try {
                                String newName = System.currentTimeMillis() + "-s";
                                if (RegionUtil.renameRegion(manager, region.getId(), newName, true)) {

                                    // Set the price flag's value and then resave the database
                                    ProtectedRegion newRegion = manager.getRegion(newName);
                                    newRegion.setFlag(DefaultFlag.PRICE, report.getBlockPrice() * 1.1);
                                    manager.addRegion(newRegion);
                                    manager.save();

                                    containerClearer.walkRegion(optConvertedRegion.get(), admin.getWorld());
                                    outliner.outline(admin.getWorld(), newRegion);

                                    econ.depositPlayer(owner, price);
                                } else {
                                    throw new CommandException();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                ChatUtil.sendNotice(sender, region.getId() + " failed to sell.");
                            }
                        }
                    });
                });
            }
        }

        @Command(aliases = {"help"}, desc = "Admin Help", min = 0, max = 0)
        @CommandPermissions({"aurora.home.admin.help"})
        public void adminHelpCmd(CommandContext args, CommandSender sender) throws CommandException {

            PlayerUtil.checkPlayer(sender).getInventory().addItem(BookUtil.Help.Admin.housing());
        }
    }

    public static String getHomeName(String player, RegionManager manager) {
        String name = player.toLowerCase() + "'s-%-house";
        String cName = "";
        for (int cur = 0; cName.isEmpty() || manager.hasRegion(cName); cur++) {
            cName = name.replace("%", String.valueOf(cur));
        }
        return cName;
    }

    public static Optional<ProtectedRegion> getStoodInHome(Player player, RegionManager manager) {
        for (ProtectedRegion aRegion : manager.getApplicableRegions(player.getLocation())) {
            if (isPlayerHouse(aRegion, new BukkitPlayer(WorldGuardPlugin.inst(), player))) {
                return Optional.of(aRegion);
            }
        }

        return Optional.empty();
    }

    public static ProtectedRegion requireStoodInHome(Player player, RegionManager manager) throws CommandException {
        return getStoodInHome(player, manager).orElseThrow(() -> {
            return new CommandException("You are not currently standing in a house you own.");
        });
    }

    public static District getHomeDistrict(ProtectedRegion region) {
        String districtName = region.getParent().getId().replace("-district", "");
        return District.fromName(districtName).get();
    }

    public static String getHomeName(Player player, RegionManager manager) {

        return getHomeName(player.getName(), manager);
    }
    public static boolean isHouse(ProtectedRegion region) {
        return region.getId().endsWith("-house");
    }

    public static boolean isPlayerHouse(ProtectedRegion region, LocalPlayer player) {
        return isHouse(region) && region.getOwners().contains(player);
    }

    public static boolean isPlayerHouse(ProtectedRegion region, Player player) {
        return isPlayerHouse(region, new BukkitPlayer(WorldGuardPlugin.inst(), player));
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
            tPlayer = InputUtil.PlayerParser.matchPlayerExactly(sender, player);
        } catch (CommandException ex) {
            tPlayer = null;
        }

        if (tPlayer == null) return false;
        tPlayer.getInventory().addItem(BookUtil.Rules.BuildingCode.server());
        tPlayer.getInventory().addItem(ruleBook);
        return true;
    }
}
