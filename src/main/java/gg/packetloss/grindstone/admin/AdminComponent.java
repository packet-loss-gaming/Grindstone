/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.GodComponent;
import com.sk89q.commandbook.InfoComponent;
import com.sk89q.commandbook.commands.PaginatedResult;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.NinjaComponent;
import gg.packetloss.grindstone.RogueComponent;
import gg.packetloss.grindstone.events.DumpPlayerInventoryEvent;
import gg.packetloss.grindstone.events.PlayerAdminModeChangeEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.database.IOUtil;
import gg.packetloss.grindstone.util.item.InventoryUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import gg.packetloss.grindstone.util.player.PlayerState;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang3.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@ComponentInformation(friendlyName = "Admin", desc = "Player Administration commands.")
@Depend(plugins = {"WorldEdit, Vault"}, components = {NinjaComponent.class, RogueComponent.class, GodComponent.class})
public class AdminComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private NinjaComponent ninjaComponent;
    @InjectComponent
    private RogueComponent rogueComponent;
    @InjectComponent
    private GodComponent godComponent;

    private final String stateDir = inst.getDataFolder().getPath() + "/admin/states/";
    String profilesDirectory = stateDir + "/profiles/";

    private static Permission permission = null;
    private final ConcurrentHashMap<UUID, PlayerState> playerState = new ConcurrentHashMap<>();

    @Override
    public void enable() {
        registerCommands(Commands.class);
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        setupPermissions();
    }

    private WorldEditPlugin worldEdit() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldEdit");

        // WorldEdit may not be loaded
        if (plugin == null || !(plugin instanceof WorldEditPlugin)) return null;

        return (WorldEditPlugin) plugin;
    }

    private boolean setupPermissions() {

        RegisteredServiceProvider<Permission> permissionProvider = server.getServicesManager().getRegistration(net
                .milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) permission = permissionProvider.getProvider();

        return (permission != null);
    }

    private boolean isDisabledBlock(Block block) {

        return isDisabledBlock(block.getTypeId());
    }

    private boolean isDisabledBlock(int block) {

        for (int tryBlock : worldEdit().getLocalConfiguration().disallowedBlocks) {
            if (block == tryBlock) return true;
        }
        return false;
    }

    public boolean hasInventoryLoaded(UUID playerID) {
        return playerState.get(playerID) != null;
    }

    private File getUUIDStateFile(UUID playerID) {
        return new File(stateDir + "/" + playerID + ".dat");
    }

    boolean hasPersistedInventory(UUID playerID) {
        return getUUIDStateFile(playerID).exists();
    }

    /**
     * A thread safe method to load an inventory into the system
     *
     * @param playerID - The ID of the player who should be loaded
     */
    public void loadInventory(final UUID playerID) {
        Object o = IOUtil.readBinaryFile(getUUIDStateFile(playerID));
        Validate.isInstanceOf(PlayerState.class, o);

        PlayerState aPlayerState = (PlayerState) o;
        playerState.put(playerID, aPlayerState);
    }

    public void loadInventoryIfUnloaded(UUID playerID) {
        // If the inventory is already loaded, we're good.
        if (hasInventoryLoaded(playerID)) {
            return;
        }

        // Try and load the player's inventory, they may have just logged in before
        // we were listening for admin logins.
        if (hasPersistedInventory(playerID)) {
            loadInventory(playerID);
        }
    }

    public void unloadInventory(final UUID playerID) {
        playerState.remove(playerID);
    }

    public void writeInventory(UUID playerID) {
        writeInventory(playerState.get(playerID));
    }

    /**
     * Writes a player's inventory on a seperate thread
     *
     * @param state - The player state to write
     */
    public void writeInventory(final PlayerState state) {


        server.getScheduler().runTaskAsynchronously(inst,
                () -> IOUtil.toBinaryFile(new File(stateDir), state.getOwnerName(), state));
    }

    /**
     * This method is used to determine if a player can enter Admin Mode.
     *
     * @param player - The player to check
     * @return - true if the player can enter Admin Mode
     */
    public boolean canEnterAdminMode(Player player) {
        return permission.playerHas(player, "aurora.admin.adminmode");
    }

    /**
     * This method is used to determine if the player is in Admin Mode.
     *
     * @param player - The player to check
     * @return - true if the player is in Admin Mode
     */
    public boolean isAdmin(Player player) {
        return permission.playerInGroup(player, "Admin");
    }

    /**
     * This method is used to make a player enter a level of Admin Mode.
     *
     * @param player     - The player to execute this for
     * @return - true if the player entered a level of Admin Mode
     */
    public boolean admin(Player player) {
        Validate.isTrue(canEnterAdminMode(player));

        if (!isAdmin(player)) {
            PlayerAdminModeChangeEvent event = new PlayerAdminModeChangeEvent(player, true);
            server.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                playerState.put(player.getUniqueId(), GeneralPlayerUtil.makeComplexState(player));
                permission.playerAddGroup((World) null, player.getName(), "Admin");
                writeInventory(player.getUniqueId());
            }
        }
        return isAdmin(player);
    }

    // This is only used internally because no one should leave an Admin Mode without being depowered.
    private boolean depermission(Player player) {
        if (isAdmin(player)) {
            PlayerAdminModeChangeEvent event = new PlayerAdminModeChangeEvent(player, false);
            server.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                // Clear their inventory
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);

                // Make a last attempt to ensure the inventory is loaded. While the deadmin command
                // will run this call as well, this is here for cases where automatic depermission
                // occurs.
                loadInventoryIfUnloaded(player.getUniqueId());

                // Restore their inventory if they have one stored
                if (playerState.containsKey(player.getUniqueId())) {
                    PlayerState identity = playerState.get(player.getUniqueId());

                    // Restore the contents
                    player.getInventory().setArmorContents(identity.getArmourContents());
                    player.getInventory().setContents(identity.getInventoryContents());
                    player.setHealth(Math.min(player.getMaxHealth(), identity.getHealth()));
                    player.setFoodLevel(identity.getHunger());
                    player.setSaturation(identity.getSaturation());
                    player.setExhaustion(identity.getExhaustion());
                    player.setLevel(identity.getLevel());
                    player.setExp(identity.getExperience());
                    player.updateInventory();

                    playerState.remove(player.getUniqueId());
                }

                // Change Permissions
                permission.playerRemoveGroup((World) null, player.getName(), "Admin");
                Validate.isTrue(!isAdmin(player));
            }
        }
        return !isAdmin(player);
    }

    /**
     * This method is used when removing permissions is not required just the current admin powers.
     *
     * @param player - The player to disable power for
     * @return - true if all active powers have been disabled
     */
    public boolean depowerPlayer(final Player player) {

        if (worldEdit().getSession(player).hasSuperPickAxe()) worldEdit().getSession(player).disableSuperPickAxe();
        if (godComponent.hasGodMode(player)) godComponent.disableGodMode(player);
        if (!player.getGameMode().equals(GameMode.SURVIVAL)) player.setGameMode(GameMode.SURVIVAL);
        if (GeneralPlayerUtil.takeFlightSafely(player)) {
            server.getScheduler().runTaskLater(inst, () -> GeneralPlayerUtil.findSafeSpot(player), 1);
        }
        return true;
    }

    /**
     * This method is used when removing a player from Admin Mode.
     *
     * @param player - The player to remove from Admin Mode
     * @return - true if all active powers and elevated permission levels have been removed
     */
    public boolean deadmin(Player player) {
        return depowerPlayer(player) && depermission(player);
    }

    /**
     * This method is used when removing a player's guild powers. Currently this only effects the {@link NinjaComponent}
     * and the {@link RogueComponent}.
     *
     * @param player - The player to disable guild powers for
     * @return - true if all active guild powers have been disabled
     */
    public boolean deguildPlayer(Player player) {
        if (ninjaComponent.isNinja(player)) ninjaComponent.unninjaPlayer(player);
        if (rogueComponent.isRogue(player)) rogueComponent.deroguePlayer(player);
        return true;
    }

    /**
     * This method is used when removing a player's guild and admin powers. This method applies to all guilds that the
     * deguildPlayer method supports.
     *
     * @param player - The player to remove from Admin Mode and remove guild and admin powers for
     * @return - true if all active guild powers have been disabled
     */
    public boolean standardizePlayer(Player player) {
        return deguildPlayer(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID playerID = event.getUniqueId();
        if (hasPersistedInventory(playerID)) {
            loadInventory(playerID);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        unloadInventory(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInventoryDump(DumpPlayerInventoryEvent event) {
        Player player = event.getPlayer();

        if (isAdmin(player)) {
            event.setCancelled(true);
            ChatUtil.sendNotice(player, "Inventory dump prevented due to use of admin mode.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();

        if (isAdmin(player)) {
            event.setCancelled(true);
        }
    }

    private static Set<InventoryType> accepted = new HashSet<>();

    static {
        accepted.add(InventoryType.PLAYER);
        accepted.add(InventoryType.CRAFTING);
        accepted.add(InventoryType.CREATIVE);
        accepted.add(InventoryType.ENCHANTING);
        accepted.add(InventoryType.WORKBENCH);
        accepted.add(InventoryType.ANVIL);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (isAdmin(player) && !accepted.contains(event.getInventory().getType())) {
            if (event.getAction().equals(InventoryAction.NOTHING)) return;
            if (InventoryUtil.getAcceptedActions().contains(event.getAction())) {
                if (event.getRawSlot() + 1 > event.getInventory().getSize()) {
                    return;
                }
            }

            event.setResult(Event.Result.DENY);
            ChatUtil.sendWarning(player, "You cannot do this while in admin mode.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (isAdmin(player) && !accepted.contains(event.getInventory().getType())) {
            for (int i : event.getRawSlots()) {
                if (i + 1 <= event.getInventory().getSize()) {
                    event.setResult(Event.Result.DENY);
                    ChatUtil.sendWarning(player, "You cannot do this while in admin mode.");
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (isDisabledBlock(block) || block.getTypeId() == BlockID.STONE_BRICK && block.getData() == 3) {
            block.breakNaturally(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();
        if (state instanceof Dispenser) {
            BlockFace dir = ((org.bukkit.material.Dispenser) state.getData()).getFacing();
            Location location = block.getRelative(dir).getLocation();
            Set<Player> found = block.getWorld().getPlayers().stream()
                    .filter(p -> p.getLocation().distanceSquared(location) < 1.5 * 1.5)
                    .collect(Collectors.toSet());

            for (Player p : found) {
                if (isAdmin(p)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    private void handleExplosion(List<Block> blockList) {
        blockList.removeIf(EnvironmentUtil::isValuableBlock);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (isAdmin(player)) {
            event.getDrops().clear();
            EnvironmentUtil.generateRadialEffect(player.getLocation(), Effect.POTION_BREAK);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onApocalypseSpawn(ApocalypsePersonalSpawnEvent event) {
        if (isAdmin(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onWhoisLookup(InfoComponent.PlayerWhoisEvent event) {

        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            event.addWhoisInformation("Is Admin", isAdmin(player));
        }
    }

    public class Commands {

        @Command(aliases = {"randomnumber"}, desc = "Simulate damage on the currently held item",
                usage = "[number]", min = 0, max = 1)
        public void randomCmd(CommandContext args, CommandSender sender) throws CommandException {

            int random = 100;
            if (args.argsLength() > 0) {
                random = args.getInteger(0);
            }

            final int origin = random;
            random = ChanceUtil.getRandom(random);

            ChatUtil.sendNotice(sender, "Number: " + random + " / " + origin);
        }

        @Command(aliases = {"simulatedamage"}, desc = "Simulate damage on the currently held item")
        @CommandPermissions("aurora.admin.simulation.damage")
        public void simulateDamageCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            ItemStack is = player.getInventory().getItemInHand();
            is.setDurability((short) Math.max(0, is.getData().getItemType().getMaxDurability() - 20));
            player.setItemInHand(is);
            ChatUtil.sendNotice(player, "Damage simulated!");
        }

        /*
        @Command(aliases = {"boom"},
                usage = "<size>", desc = "Create a bomb of any size",
                flags = "", min = 1, max = 1)
        @CommandPermissions("aurora.admin.boom")
        public void boomCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            try {
                float size = Float.parseFloat(args.getString(0));
                player.getWorld().createExplosion(player.getLocation(), size);
                ChatUtil.sendNotice(player, "Boom!");
            } catch (NumberFormatException ex) {
                throw new CommandException("Please enter a valid number");
            }
        }
        */

        @Command(aliases = {"user"}, desc = "User Management Commands")
        @NestedCommand({NestedAdminCommands.class})
        public void userManagementCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"profiles", "p"}, desc = "Profile Commands")
        @NestedCommand({NestedProfileCommands.class})
        public void profileCommands(CommandContext args, CommandSender sender) throws CommandException {

        }


        @Command(aliases = {"admin", "alivemin"},
                usage = "", desc = "Enter Admin Mode",
                flags = "", min = 0, max = 0)
        public void adminModeCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            if (!canEnterAdminMode(player)) {
                throw new CommandPermissionsException();
            }

            if (isAdmin(player)) {
                throw new CommandException("You were already in admin mode!");
            }

            if (admin(player)) {
                ChatUtil.sendNotice(sender, "You have entered admin mode.");
            } else {
                throw new CommandException("You fail to enter admin mode.");
            }
        }

        /**
         * @return true if there's a problem
         */
        private boolean checkInventoryLoaded(UUID playerID) {
            loadInventoryIfUnloaded(playerID);

            // We have a problem if the inventory is still not loaded.
            return !hasInventoryLoaded(playerID);
        }

        @Command(aliases = {"deadmin"},
                usage = "", desc = "Leave Admin Mode",
                flags = "k", min = 0, max = 0)
        public void deadminModeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            if (!isAdmin(player)) {
                throw new CommandException("You were not in admin mode!");
            }

            UUID playerID = player.getUniqueId();
            if (!args.hasFlag('k') && checkInventoryLoaded(playerID)) {
                throw new CommandException("Your inventory is not loaded! \nLeaving admin mode will result in item loss! " +
                  "\nUse \"/deadmin -k\" to ignore this warning and continue anyways.");
            }

            if (deadmin(player)) {
                ChatUtil.sendNotice(sender, "You have left admin mode.");
            } else {
                throw new CommandException("You fail to leave admin mode.");
            }
        }
    }

    public class NestedAdminCommands {

        @Command(aliases = {"modify", "mod", "permissions", "perm"}, desc = "Permissions Commands")
        @NestedCommand({NestedPermissionsCommands.class})
        public void userManagementCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedProfileCommands {

        @Command(aliases = {"save"},
                usage = "<profile name>", desc = "Save an inventory as a profile",
                flags = "o", min = 1, max = 1)
        @CommandPermissions({"aurora.admin.profiles.save"})
        public void profileSaveCmd(CommandContext args, CommandSender sender) throws CommandException {

            final Player player = PlayerUtil.checkPlayer(sender);

            final File profileDir = new File(profilesDirectory);
            final String profileName = args.getString(0);

            File file = IOUtil.getBinaryFile(profileDir, profileName);

            if (file.exists() && !args.hasFlag('o')) {
                throw new CommandException("A profile by that name already exist!");
            }

            server.getScheduler().runTaskAsynchronously(inst,
                    () -> IOUtil.toBinaryFile(profileDir, profileName, GeneralPlayerUtil.makeComplexState(player)));
            ChatUtil.sendNotice(sender, "Profile: " + profileName + ", saved!");
        }

        @Command(aliases = {"load"},
                usage = "<profile name> [target]", desc = "Load a saved inventory profile",
                flags = "ef", min = 1, max = 2)
        @CommandPermissions({"aurora.admin.profiles.load"})
        public void profileLoadCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player;

            if (args.argsLength() > 1) {
                player = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(1));
            } else {
                player = PlayerUtil.checkPlayer(sender);
            }

            if (!isAdmin(player)) {
                throw new CommandException("You can only use this command while in Admin Mode!");
            }

            final File profileDir = new File(profilesDirectory);
            final String profileName = args.getString(0);

            File file = IOUtil.getBinaryFile(profileDir, profileName);

            if (!file.exists()) {
                throw new CommandException("A profile by that name doesn't exist!");
            }

            Object o = IOUtil.readBinaryFile(file);

            if (o instanceof PlayerState) {
                PlayerState identity = (PlayerState) o;

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                player.setHealth(Math.min(player.getMaxHealth(), identity.getHealth()));
                player.setFoodLevel(identity.getHunger());
                player.setSaturation(identity.getSaturation());
                player.setExhaustion(identity.getExhaustion());
                if (args.hasFlag('e')) {
                    player.setLevel(identity.getLevel());
                    player.setExp(identity.getExperience());
                }
            } else {
                throw new CommandException("The profile: " + profileName + ", is corrupt!");
            }
            ChatUtil.sendNotice(sender, "Profile loaded, and successfully applied!");
        }

        @Command(aliases = {"list"},
                usage = "[-p page] [prefix]", desc = "List saved inventory profiles",
                flags = "p:", min = 0, max = 1)
        @CommandPermissions({"aurora.admin.profiles.list"})
        public void profileListCmd(CommandContext args, CommandSender sender) throws CommandException {

            new PaginatedResult<File>(ChatColor.GOLD + "Profiles") {
                @Override
                public String format(File file) {
                    return file.getName().replace(".dat", "");
                }
            }.display(
                    sender,
                    Arrays.asList(new File(profilesDirectory).listFiles((dir, name) ->
                            (args.argsLength() < 1 || name.startsWith(args.getString(0))) && name.endsWith(".dat"))),
                    args.getFlagInteger('p', 1)
            );
        }

        @Command(aliases = {"delete"},
                usage = "<profile name>", desc = "Delete a saved inventory profile",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.admin.profiles.delete"})
        public void profileDeleteCmd(CommandContext args, CommandSender sender) throws CommandException {

            File file = IOUtil.getBinaryFile(new File(profilesDirectory), args.getString(0));

            if (!file.exists()) {
                throw new CommandException("A profile by that name doesn't exist!");
            }

            if (!file.delete()) {
                throw new CommandException("That profile couldn't be deleted!");
            }
            ChatUtil.sendNotice(sender, "Profile deleted!");
        }
    }

    public class NestedPermissionsCommands {

        @Command(aliases = {"set"},
                usage = "<player> <group>", desc = "Modify a player's permissions",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.admin.user.modify.change"})
        public void userGroupSetCmd(CommandContext args, CommandSender sender) {

            String player = args.getString(0).toLowerCase();
            String group = args.getString(1);

            // Modify Permissions Group
            for (String aGroup : permission.getPlayerGroups((World) null, player)) {
                if (aGroup.equalsIgnoreCase("platinum") || aGroup.equalsIgnoreCase("admin")) continue;
                permission.playerRemoveGroup((World) null, player, aGroup);
            }

            boolean successful = permission.playerAddGroup((World) null, player, group);

            // Tell Admin
            if (successful) {
                ChatUtil.sendNotice(sender, "The player: " + player + " is now in the group: " + group + ".");
            } else {
                ChatUtil.sendError(sender, "The player: " + player + "'s group could not be set to the group: "
                        + group + ".");
            }
        }

        @Command(aliases = {"add"},
                usage = "<player> <group>", desc = "Modify a player's permissions",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.admin.user.modify.add"})
        public void userGroupAddCmd(CommandContext args, CommandSender sender) {

            String player = args.getString(0).toLowerCase();
            String group = args.getString(1);

            // Modify Permissions Group
            boolean successful = permission.playerAddGroup((World) null, player, group);

            // Tell Admin
            if (successful) {
                ChatUtil.sendNotice(sender, "The player: " + player + " is now in the group: " + group + ".");
            } else {
                ChatUtil.sendError(sender, "The player: " + player + " is now in the group: " + group + ".");
            }
        }

        @Command(aliases = {"remove", "rem", "del"},
                usage = "<player> <group>", desc = "Modify a player's permissions",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.admin.user.modify.remove"})
        public void userGroupRemoveCmd(CommandContext args, CommandSender sender) {

            String player = args.getString(0).toLowerCase();
            String group = args.getString(1);

            // Modify Permissions Group
            boolean successful = permission.playerRemoveGroup((World) null, player, group);

            // Tell Admin
            if (successful) {
                ChatUtil.sendNotice(sender, "The player: " + player + " has left the group: " + group + ".");
            } else {
                ChatUtil.sendError(sender, "The player: " + player + " could not be removed from the group: "
                        + group + ".");
            }
        }
    }
}