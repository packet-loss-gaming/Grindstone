package com.skelril.aurora.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.GodComponent;
import com.sk89q.commandbook.InfoComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.skelril.aurora.NinjaComponent;
import com.skelril.aurora.RogueComponent;
import com.skelril.aurora.events.PlayerAdminModeChangeEvent;
import com.skelril.aurora.exceptions.PlayerOnlyCommandException;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.database.IOUtil;
import com.skelril.aurora.util.database.InventoryAuditLogger;
import com.skelril.aurora.util.item.InventoryUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.player.GeneralPlayerUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
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

    private InventoryAuditLogger auditor;

    private final String stateDir = inst.getDataFolder().getPath() + "/admin/states/";
    private final FilenameFilter stateFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {

            return !name.startsWith("old-") && name.endsWith(".dat");
        }
    };
    String profilesDirectory = stateDir + "/profiles/";

    private static Permission permission = null;
    private final ConcurrentHashMap<String, PlayerState> playerState = new ConcurrentHashMap<>();

    @Override
    public void enable() {

        File auditorDirectory = new File(inst.getDataFolder().getPath() + "/admin");
        if (!auditorDirectory.exists()) auditorDirectory.mkdir();
        auditor = new InventoryAuditLogger(auditorDirectory);

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

    public boolean hasInventoryLoaded(String player) {

        return playerState.containsKey(player) && playerState.get(player) != null;
    }

    public void loadInventories() {

        File workingDir = new File(stateDir);

        if (!workingDir.exists()) return;

        for (File file : workingDir.listFiles(stateFilter)) {

            Object o = IOUtil.readBinaryFile(file);
            if (o instanceof PlayerState) {
                PlayerState aPlayerState = (PlayerState) o;
                playerState.put(aPlayerState.getOwnerName(), aPlayerState);
            } else {
                log.warning("Invalid player state file encountered: " + file.getName() + "!");
            }
        }
    }

    /**
     * A thread safe method to load an inventory into the system
     *
     * @param playerName - The player who should be loaded
     */
    public void loadInventory(final String playerName) {

        File file = new File(stateDir + "/" + playerName + ".dat");

        if (!file.exists()) return;

        Object o = IOUtil.readBinaryFile(file);
        if (o instanceof PlayerState) {
            PlayerState aPlayerState = (PlayerState) o;
            playerState.put(aPlayerState.getOwnerName(), aPlayerState);
        } else {
            log.warning("Invalid player state file encountered: " + file.getName() + "!");
        }
    }

    public void unloadInventory(final String playerName) {

        if (playerState.containsKey(playerName)) {
            playerState.remove(playerName);
        }
    }

    public void writeInventories() {

        for (PlayerState state : playerState.values()) {
            writeInventory(state);
        }
    }

    public void writeInventory(String playerName) {

        if (!playerState.containsKey(playerName)) return;

        writeInventory(playerState.get(playerName));
    }

    /**
     * Writes a player's inventory on a seperate thread
     *
     * @param state - The player state to write
     */
    public void writeInventory(final PlayerState state) {


        server.getScheduler().runTaskAsynchronously(inst, new Runnable() {
            @Override
            public void run() {

                IOUtil.toBinaryFile(new File(stateDir), state.getOwnerName(), state);
            }
        });
    }

    public InventoryAuditLogger getInventoryDumpLogger() {

        return auditor;
    }

    /**
     * This method is used to determine if the player is in Admin Mode.
     *
     * @param player - The player to check
     * @return - true if the player is in Admin Mode
     */
    public boolean isAdmin(Player player) {

        return !getAdminState(player).equals(AdminState.MEMBER);
    }

    public boolean isSysop(Player player) {

        return getAdminState(player).equals(AdminState.SYSOP);
    }

    /**
     * This method is used to determine the {@link AdminState} of the player.
     *
     * @param player - The player to check
     * @return - The {@link AdminState} of the player
     */
    public AdminState getAdminState(Player player) {

        if (permission.has((World) null, player.getName(), "aurora.admin.adminmode.sysop.active")) {
            return AdminState.SYSOP;
        } else if (permission.playerInGroup((World) null, player.getName(), "Admin")) {
            return AdminState.ADMIN;
        } else if (permission.playerInGroup((World) null, player.getName(), "Mod")) {
            return AdminState.MODERATOR;
        } else {
            return AdminState.MEMBER;
        }
    }

    /**
     * This method is used to make a player enter a level of Admin Mode.
     *
     * @param player     - The player to execute this for
     * @param adminState - The {@link AdminState} to attempt to achieve
     * @return - true if the player entered a level of Admin Mode
     */
    public boolean admin(Player player, AdminState adminState) {

        if (!isAdmin(player)) {
            PlayerAdminModeChangeEvent event = new PlayerAdminModeChangeEvent(player, adminState);
            server.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                playerState.put(player.getName(), GeneralPlayerUtil.makeComplexState(player));
                switch (adminState) {
                    case SYSOP:
                        permission.playerAdd((World) null, player.getName(), "aurora.admin.adminmode.sysop.active");
                    case ADMIN:
                        permission.playerAddGroup((World) null, player.getName(), "Admin");
                        break;
                    case MODERATOR:
                        permission.playerAddGroup((World) null, player.getName(), "Mod");
                        break;
                    default:
                        break;
                }

                writeInventory(player.getName());
            }
        }
        return isAdmin(player);
    }

    // This is only used internally because no one should leave an Admin Mode without being depowered.
    private boolean depermission(Player player) {

        if (isAdmin(player)) {
            PlayerAdminModeChangeEvent event = new PlayerAdminModeChangeEvent(player, AdminState.MEMBER);
            server.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                // Clear their inventory
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);

                // Restore their inventory if they have one stored
                if (playerState.containsKey(player.getName())) {

                    PlayerState identity = playerState.get(player.getName());

                    // Restore the contents
                    player.getInventory().setArmorContents(identity.getArmourContents());
                    player.getInventory().setContents(identity.getInventoryContents());
                    player.setHealth(Math.min(player.getMaxHealth(), identity.getHealth()));
                    player.setFoodLevel(identity.getHunger());
                    player.setSaturation(identity.getSaturation());
                    player.setExhaustion(identity.getExhaustion());
                    player.setLevel(identity.getLevel());
                    player.setExp(identity.getExperience());

                    playerState.remove(player.getName());
                }

                // Change Permissions
                do {
                    switch (getAdminState(player)) {
                        case SYSOP:
                            permission.playerRemove((World) null, player.getName(), "aurora.admin.adminmode.sysop.active");
                        case ADMIN:
                            permission.playerRemoveGroup((World) null, player.getName(), "Admin");
                            break;
                        case MODERATOR:
                            permission.playerRemoveGroup((World) null, player.getName(), "Mod");
                            break;
                        default:
                            return false;
                    }
                } while (isAdmin(player));
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
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFallDistance(0F);

            server.getScheduler().runTaskLater(inst, new Runnable() {
                @Override
                public void run() {
                    Location toBlock = LocationUtil.findFreePosition(player.getLocation());
                    boolean done = toBlock != null && player.teleport(toBlock);
                    short max = 200;

                    while (!done && max > 0) {

                        max--;

                        if (toBlock == null) toBlock = player.getLocation();

                        toBlock = LocationUtil.findRandomLoc(toBlock, 1, false, false);

                        Location testBlock = LocationUtil.findFreePosition(toBlock);
                        done = testBlock != null && player.teleport(testBlock);
                    }

                    if (max <= 0) {
                        player.teleport(player.getWorld().getSpawnLocation());
                        ChatUtil.sendError(player, "Failed to locate a safe location, teleporting to spawn!");
                    }
                }
            }, 1);
        }
        if (player.getGameMode().equals(GameMode.CREATIVE)) player.setGameMode(GameMode.SURVIVAL);
        return true;
    }

    /**
     * This method is used when removing a player from Admin Mode. Their {@link AdminState} will be set to the
     * lowest level possible.
     *
     * @param player - The player to remove from Admin Mode
     * @return - true if all active powers and elevated permission levels have been removed
     */
    public boolean deadmin(Player player) {

        return deadmin(player, false);
    }

    public boolean deadmin(Player player, boolean force) {

        //noinspection SimplifiableIfStatement
        if (isSysop(player) && !force) return false;
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
     * @return - true if all active guild and admin powers have been disabled
     */
    public boolean standardizePlayer(Player player) {

        return standardizePlayer(player, false);
    }

    public boolean standardizePlayer(Player player, boolean force) {

        return deadmin(player, force) && deguildPlayer(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {

        loadInventory(event.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        unloadInventory(event.getPlayer().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        if (isSysop(player)) return;

        if (isAdmin(player)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot drop items while in admin mode.");
        }
    }

    @EventHandler
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

        if (isSysop(player)) return;

        //InventoryType.SlotType st = event.getSlotType();
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

        if (isSysop(player)) return;

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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (isSysop(player)) return;

        if (isAdmin(player) && block.getTypeId() == BlockID.JUKEBOX
                && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            ChatUtil.sendWarning(player, "You cannot use this while in admin mode.");
        } else if (isAdmin(player) && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && player.getItemInHand().getTypeId() == ItemID.SPAWN_EGG) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot use this while in admin mode.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();

        if (isSysop(player)) return;

        if (isAdmin(player) && player.getItemInHand().getTypeId() == ItemID.SPAWN_EGG) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot use this while in admin mode.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (EnvironmentUtil.isValuableBlock(block) || isAdmin(player) && isDisabledBlock(block)
                || block.getTypeId() == BlockID.STONE_BRICK && block.getData() == 3) {
            // Temporary work around
            if (isAdmin(player) && block.getTypeId() == BlockID.TNT) {
                block.setTypeId(0);
                event.setCancelled(true);
            } else {
                block.breakNaturally(null);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (isSysop(player)) return;

        if (EnvironmentUtil.isValuableBlock(block) && !isAdmin(player) || isAdmin(player) && isDisabledBlock(block)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot place that block.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {

        for (Block block : event.blockList()) {
            if (EnvironmentUtil.isValuableBlock(block)) {
                event.blockList().clear();
                return;
            }
        }
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
    public void onWhoisLookup(InfoComponent.PlayerWhoisEvent event) {

        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            event.addWhoisInformation("User Level", getAdminState(player));
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

            if (!(sender instanceof Player)) throw new PlayerOnlyCommandException();

            Player player = (Player) sender;

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

            if (!(sender instanceof Player)) throw new PlayerOnlyCommandException();

            Player player = (Player) sender;

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

        @Command(aliases = {"profiles"}, desc = "Profile Commands")
        @NestedCommand({NestedProfileCommands.class})
        public void profileCommands(CommandContext args, CommandSender sender) throws CommandException {

        }


        @Command(aliases = {"admin", "alivemin"},
                usage = "[-c \"command\"]", desc = "Enter Admin Mode",
                flags = "ec:", min = 0, max = 0)
        public void adminModeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new PlayerOnlyCommandException();

            Player player = (Player) sender;

            if (!isAdmin(player)) {
                boolean admin;
                if (args.hasFlag('e') && inst.hasPermission(player, "aurora.admin.adminmode.sysop")) {
                    admin = admin(player, AdminState.SYSOP);
                } else if (inst.hasPermission(player, "aurora.admin.adminmode.admin")) {
                    admin = admin(player, AdminState.ADMIN);
                } else if (inst.hasPermission(player, "aurora.admin.adminmode.moderator")) {
                    admin = admin(player, AdminState.MODERATOR);
                } else {
                    throw new CommandPermissionsException();
                }

                if (args.hasFlag('c') && admin) {
                    String command = args.getFlag('c');
                    player.performCommand(command);
                    deadmin(player, true);
                    ChatUtil.sendNotice(sender, "Ran command: " + command + ".");
                } else if (admin) {
                    ChatUtil.sendNotice(sender, "You have entered admin mode.");
                } else {
                    throw new CommandException("You fail to enter admin mode.");
                }
            } else {
                throw new CommandException("You were already in admin mode!");
            }
        }

        @Command(aliases = {"deadmin"},
                usage = "", desc = "Leave Admin Mode",
                flags = "k", min = 0, max = 0)
        public void deadminModeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new PlayerOnlyCommandException();

            Player player = (Player) sender;

            if (isAdmin(player)) {
                if (!args.hasFlag('k') && !hasInventoryLoaded(player.getName())) {
                    throw new CommandException("Your inventory is not loaded! \nLeaving admin mode will result in item loss! " +
                            "\nUse \"/deadmin -k\" to ignore this warning and continue anyways.");
                }
                if (deadmin(player, true)) {
                    ChatUtil.sendNotice(sender, "You have left admin mode.");
                } else {
                    throw new CommandException("You fail to leave admin mode.");
                }
            } else {
                throw new CommandException("You were not in admin mode!");
            }
        }
    }

    public class NestedAdminCommands {

        @Command(aliases = {"modify", "mod", "permissions", "perm"}, desc = "Permissions Commands")
        @NestedCommand({NestedPermissionsCommands.class})
        public void userManagementCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"lost"}, desc = "Permissions Commands")
        @NestedCommand({NestedLostItemCommands.class})
        public void lostItemCommands(CommandContext args, CommandSender sender) throws CommandException {

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

            server.getScheduler().runTaskAsynchronously(inst, new Runnable() {
                @Override
                public void run() {

                    IOUtil.toBinaryFile(profileDir, profileName, GeneralPlayerUtil.makeComplexState(player));
                }
            });
            ChatUtil.sendNotice(sender, "Profile: " + profileName + ", saved!");
        }

        @Command(aliases = {"load"},
                usage = "<profile name>", desc = "Load a saved inventory profile",
                flags = "ef", min = 1, max = 1)
        @CommandPermissions({"aurora.admin.profiles.load"})
        public void profileLoadCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            if (!isAdmin(player) && !(args.hasFlag('f')
                    && inst.hasPermission(player, "aurora.admin.adminmode.sysop"))) {
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

    public class NestedLostItemCommands {

        @Command(aliases = {"god"}, desc = "Lost God items")
        @NestedCommand({LostGodItem.class})
        public void lostGodCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"ancient"}, desc = "Lost Ancient items")
        @NestedCommand({LostAncientItem.class})
        public void lostAncientCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"master"}, desc = "Lost Master items")
        @NestedCommand({LostMasterItem.class})
        public void lostMasterCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"fear"}, desc = "Lost Fear items")
        @NestedCommand({LostFearItem.class})
        public void lostFearCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"unleashed", "unl"}, desc = "Lost Unleashed items")
        @NestedCommand({LostUnleashedItem.class})
        public void lostUnleashedCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"donation"}, desc = "Lost Donation items")
        @NestedCommand({LostDonationItem.class})
        public void lostDonationCommands(CommandContext args, CommandSender sender) throws CommandException {

        }


        @Command(aliases = {"admin"}, desc = "Lost Admin items")
        @NestedCommand({LostAdminItem.class})
        public void lostAdminCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"red"}, desc = "Lost Red items")
        @NestedCommand({LostRedItem.class})
        public void lostRedCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"misc"}, desc = "Lost Grave Yard items")
        @NestedCommand({LostMiscItem.class})
        public void lostMiscCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class LostGodItem {

        @Command(aliases = {"armor"},
                usage = "<player>", desc = "Return a player's God Armor",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.armor"})
        public void lostArmorCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(
                    ItemUtil.God.makeHelmet(), ItemUtil.God.makeChest(),
                    ItemUtil.God.makeLegs(), ItemUtil.God.makeBoots()
            );

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given new god armour.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's God Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.God.makeBow());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god bow.");
        }

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's God Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.God.makeSword());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god sword.");
        }

        @Command(aliases = {"pickaxe", "pick"},
                usage = "<player>", desc = "Return a player's God Pickaxe",
                flags = "l", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.pickaxe"})
        public void lostPickaxeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.God.makePickaxe(args.hasFlag('l')));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god pickaxe.");
        }

        @Command(aliases = {"axe"},
                usage = "<player>", desc = "Return a player's God Axe",
                flags = "l", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.god.axe"})
        public void lostAxeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.God.makeAxe(args.hasFlag('l')));

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god axe.");
        }
    }

    public class LostAncientItem {

        @Command(aliases = {"crown"},
                usage = "<player>", desc = "Return a player's Ancient Crown",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.ancient.crown"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Ancient.makeCrown());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new ancient crown.");
        }

        @Command(aliases = {"armor"},
                usage = "<player>", desc = "Return a player's Ancient Armour",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.ancient.armor"})
        public void lostArmorCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(
                    ItemUtil.Ancient.makeHelmet(), ItemUtil.Ancient.makeChest(),
                    ItemUtil.Ancient.makeLegs(), ItemUtil.Ancient.makeBoots()
            );

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given new ancient armour.");
        }
    }

    public class LostMasterItem {

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's Master Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.master.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Master.makeSword());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new master sword.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's Master Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.master.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Master.makeBow());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new master sword.");
        }
    }

    public class LostFearItem {

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's Fear Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.fear.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Fear.makeSword());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new fear sword.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's Fear Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.fear.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Fear.makeBow());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new fear bow.");
        }
    }

    public class LostUnleashedItem {

        @Command(aliases = {"sword"},
                usage = "<player>", desc = "Return a player's Unleashed Sword",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.unleashed.sword"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Unleashed.makeSword());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new unleashed sword.");
        }

        @Command(aliases = {"bow"},
                usage = "<player>", desc = "Return a player's Unleashed Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.unleashed.bow"})
        public void lostBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Unleashed.makeBow());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new unleashed bow.");
        }
    }

    public class LostRedItem {

        @Command(aliases = {"feather"},
                usage = "<player>", desc = "Return a player's Red Feather",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.red.feather"})
        public void lostSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Red.makeFeather());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new red feather.");
        }
    }

    public class LostMiscItem {

        @Command(aliases = {"potionofrestitution"},
                usage = "<player>", desc = "Give a player a Potion of Restitution",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.potionofrestitution"})
        public void lostPotionOfRestitutionCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));
            player.getInventory().addItem(ItemUtil.MPotion.potionOfRestitution());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given a Potion of Restitution.");
        }

        @Command(aliases = {"gemofdarkness"},
                usage = "<player> [amount]", desc = "Give a player some Gems of Darkness",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.gemofdarkness"})
        public void lostGemOfDarknessCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = ItemUtil.Misc.gemOfDarkness(Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Gems of Darkness.");
        }

        @Command(aliases = {"pixiedust"},
                usage = "<player> [amount]", desc = "Give a player some Pixie Dust",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.phantomgold"})
        public void lostPixieDustCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = ItemUtil.Misc.pixieDust(Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Pixie Dust.");
        }

        @Command(aliases = {"phantomgold"},
                usage = "<player> [amount]", desc = "Give a player some Phantom Gold",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.phantomgold"})
        public void lostPhantomGoldCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = ItemUtil.Misc.phantomGold(Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Phantom Gold.");
        }

        @Command(aliases = {"phantomclock"},
                usage = "<player> [amount]", desc = "Give a player some Phantom Clocks",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.phantomclock"})
        public void lostPhantomClockCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = ItemUtil.Misc.phantomClock(Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Phantom Clocks.");
        }

        @Command(aliases = {"gemoflife"},
                usage = "<player> [amount]", desc = "Give a player some Gems of Life",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.gemoflife"})
        public void lostGemOfLifeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = ItemUtil.Misc.gemOfLife(Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Gems of Life.");
        }

        @Command(aliases = {"imbuedcrystal"},
                usage = "<player> [amount]", desc = "Give a player some Imbued Crystals",
                flags = "", min = 1, max = 2)
        @CommandPermissions({"aurora.lost.misc.imbuedcrystal"})
        public void lostImbuedCrystalCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            int amount = 1;
            if (args.argsLength() > 1) {
                amount = args.getInteger(1);
            }

            ItemStack stack = ItemUtil.Misc.imbuedCrystal(Math.max(0, Math.min(64, amount)));
            player.getInventory().addItem(stack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been given " + stack.getAmount() + " Imbued Crystals.");
        }

        @Command(aliases = {"batbow"},
                usage = "<player>", desc = "Return a player's Bat Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.bow.bat"})
        public void lostBatBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Misc.batBow());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been a new Bat Bow.");
        }

        @Command(aliases = {"chickenbow"},
                usage = "<player>", desc = "Return a player's Chicken Bow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.bow.chicken"})
        public void lostChickenBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Misc.chickenBow());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been a new Chicken Bow.");
        }

        @Command(aliases = {"magicbucket"},
                usage = "<player>", desc = "Return a player's Magic Bucket",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.misc.magicbucket"})
        public void lostMagicBucketCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            player.getInventory().addItem(ItemUtil.Misc.magicBucket());

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: "
                    + player.getDisplayName() + " has been a new Magic Bucket.");
        }
    }

    public class LostDonationItem {

        @Command(aliases = {"butterboots"},
                usage = "<player>", desc = "Return a player's butter boots",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.donation.butterboots"})
        public void lostButterBootsCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            ItemStack bootStack = new ItemStack(ItemID.GOLD_BOOTS);
            ItemMeta butterMeta = bootStack.getItemMeta();
            butterMeta.setDisplayName(ChatColor.GOLD + "Butter Boots");
            bootStack.setItemMeta(butterMeta);
            player.getInventory().addItem(bootStack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given new butter boots.");
        }
    }

    public class LostAdminItem {

        @Command(aliases = {"pwngbow"},
                usage = "<player>", desc = "Return a player's pwngbow",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.admin.pwngbow"})
        public void lostPwngBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            ItemStack pwngBowStack = new ItemStack(Material.BOW);
            ItemMeta pwngBow = pwngBowStack.getItemMeta();
            pwngBow.addEnchant(Enchantment.ARROW_DAMAGE, 10000, true);
            pwngBow.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
            pwngBow.setDisplayName(ChatColor.DARK_PURPLE + "Pwng Bow");
            pwngBowStack.setItemMeta(pwngBow);
            player.getInventory().addItem(pwngBowStack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new pwng bow.");
        }
    }
}