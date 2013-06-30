package com.skelril.aurora.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.GodComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.skelril.aurora.NinjaComponent;
import com.skelril.aurora.RogueComponent;
import com.skelril.aurora.events.PlayerAdminModeChangeEvent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.item.ItemUtil;
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
import org.bukkit.entity.Entity;
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

import java.util.*;
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

    private static Permission permission = null;
    private final List<String> sysops = new ArrayList<>();
    private final HashMap<String, PlayerState> playerState = new HashMap<>();
    private final HashMap<String, AdminPlayerState> offlinePlayerState = new HashMap<>();

    @Override
    public void enable() {

        registerCommands(Commands.class);
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        setupPermissions();
    }

    @Override
    public void disable() {

        saveInventories();
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

    private void saveInventories() {

        for (Player player : server.getOnlinePlayers()) {

            if (playerState.containsKey(player.getName())) deadmin(player, true);
        }
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

        if (sysops.contains(player.getName())) {
            return AdminState.SYSOP;
        } else if (permission.playerInGroup((World) null, player.getName(), "Admin")) {
            return AdminState.ADMIN;
        } else if (permission.playerInGroup((World) null, player.getName(), "Moderator")) {
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
                playerState.put(player.getName(), new PlayerState(player.getName(),
                        player.getInventory().getContents(),
                        player.getInventory().getArmorContents(),
                        player.getHealth(),
                        player.getFoodLevel(),
                        player.getSaturation(),
                        player.getExhaustion(),
                        player.getLevel(),
                        player.getExp()));
                switch (adminState) {
                    case SYSOP:
                        sysops.add(player.getName());
                    case ADMIN:
                        permission.playerAddGroup((World) null, player.getName(), "Admin");
                        break;
                    case MODERATOR:
                        permission.playerAddGroup((World) null, player.getName(), "Moderator");
                        break;
                    default:
                        break;
                }
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
                    player.setHealth(identity.getHealth());
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
                            sysops.remove(player.getName());
                        case ADMIN:
                            permission.playerRemoveGroup((World) null, player.getName(), "Admin");
                            break;
                        case MODERATOR:
                            permission.playerRemoveGroup((World) null, player.getName(), "Moderator");
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
        if (player.getGameMode().equals(GameMode.CREATIVE)) player.setGameMode(GameMode.SURVIVAL);
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
        if (sysops.contains(player.getName()) && !force) return false;
        return depermission(player) && depowerPlayer(player);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {

        final Player player = event.getPlayer();

        if (!((Entity) player).isOnGround() && (isAdmin(player) || offlinePlayerState.containsKey(player.getName()))) {
            if (offlinePlayerState.containsKey(player.getName())) {

                AdminPlayerState identity = offlinePlayerState.get(player.getName());

                // Restore Admin State
                admin(player, identity.getAdminState());

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                player.setHealth(identity.getHealth());
                player.setFoodLevel(identity.getHunger());
                player.setSaturation(identity.getSaturation());
                player.setExhaustion(identity.getExhaustion());
                player.setLevel(identity.getLevel());
                player.setExp(identity.getExperience());

                offlinePlayerState.remove(player.getName());
            }

            server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

                @Override
                public void run() {

                    LocationUtil.toGround(player);
                }
            }, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        if (playerState.containsKey(player.getName())) {

            offlinePlayerState.put(player.getName(), new AdminPlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    getAdminState(player),
                    player.getHealth(),
                    player.getFoodLevel(),
                    player.getSaturation(),
                    player.getExhaustion(),
                    player.getLevel(),
                    player.getExp()));
            deadmin(player, true);
        }
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
    private static Set<InventoryAction> acceptedActions = new HashSet<>();

    static {
        accepted.add(InventoryType.PLAYER);
        accepted.add(InventoryType.CRAFTING);
        accepted.add(InventoryType.CREATIVE);
        accepted.add(InventoryType.ENCHANTING);
        accepted.add(InventoryType.WORKBENCH);
        accepted.add(InventoryType.ANVIL);

        acceptedActions.add(InventoryAction.SWAP_WITH_CURSOR);
        acceptedActions.add(InventoryAction.CLONE_STACK);

        acceptedActions.add(InventoryAction.PLACE_ALL);
        acceptedActions.add(InventoryAction.PICKUP_SOME);
        acceptedActions.add(InventoryAction.PLACE_ONE);

        acceptedActions.add(InventoryAction.PICKUP_ALL);
        acceptedActions.add(InventoryAction.PICKUP_HALF);
        acceptedActions.add(InventoryAction.PICKUP_SOME);
        acceptedActions.add(InventoryAction.PICKUP_ONE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (isSysop(player)) return;

        //InventoryType.SlotType st = event.getSlotType();
        if (isAdmin(player) && !accepted.contains(event.getInventory().getType())) {

            if (event.getAction().equals(InventoryAction.NOTHING)) return;
            if (acceptedActions.contains(event.getAction())) {
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (EnvironmentUtil.isValuableBlock(block) || isAdmin(player) && isDisabledBlock(block)
                || block.getTypeId() == BlockID.STONE_BRICK && block.getData() == 3) {
            block.breakNaturally(null);
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

    public class Commands {

        @Command(aliases = {"simulatedamage"}, desc = "Simulate damage on the currently held item")
        @CommandPermissions("aurora.admin.simulation.damage")
        public void simulateDamageCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            Player player = (Player) sender;

            ItemStack is = player.getInventory().getItemInHand();
            is.setDurability((short) Math.max(0, is.getData().getItemType().getMaxDurability() - 20));
            player.setItemInHand(is);
            ChatUtil.sendNotice(player, "Damage simulated!");
        }

        @Command(aliases = {"user"}, desc = "User Management Commands")
        @NestedCommand({NestedAdminCommands.class})
        public void userManagementCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"admin", "alivemin"},
                usage = "[-c \"command\"]", desc = "Enter Admin Mode",
                flags = "ec:", min = 0, max = 0)
        public void adminModeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

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
                    ChatUtil.sendWarning(sender, "You fail to enter admin mode.");
                }
            } else {
                ChatUtil.sendError(sender, "You were already in admin mode!");
            }
        }

        @Command(aliases = {"deadmin"},
                usage = "", desc = "Leave Admin Mode",
                flags = "", min = 0, max = 0)
        public void deadminModeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            Player player = (Player) sender;

            if (isAdmin(player)) {
                if (deadmin(player, true)) {
                    ChatUtil.sendNotice(sender, "You have left admin mode.");
                } else {
                    ChatUtil.sendWarning(sender, "You fail to leave admin mode.");
                }
            } else {
                ChatUtil.sendError(sender, "You were not in admin mode!");
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

            ItemStack[] armour = new ItemStack[4];
            armour[0] = new ItemStack(ItemID.DIAMOND_HELMET);
            armour[1] = new ItemStack(ItemID.DIAMOND_CHEST);
            armour[2] = new ItemStack(ItemID.DIAMOND_PANTS);
            armour[3] = new ItemStack(ItemID.DIAMOND_BOOTS);

            ItemMeta meta = armour[0].getItemMeta();
            meta.setDisplayName(ChatColor.BLUE + "God Helmet");
            armour[0].setItemMeta(meta);

            meta = armour[1].getItemMeta();
            meta.setDisplayName(ChatColor.BLUE + "God Chestplate");
            armour[1].setItemMeta(meta);

            meta = armour[2].getItemMeta();
            meta.setDisplayName(ChatColor.BLUE + "God Leggings");
            armour[2].setItemMeta(meta);

            meta = armour[3].getItemMeta();
            meta.setDisplayName(ChatColor.BLUE + "God Boots");
            armour[3].setItemMeta(meta);

            armour[0].addEnchantment(Enchantment.OXYGEN, 3);
            armour[0].addEnchantment(Enchantment.WATER_WORKER, 1);

            armour[3].addEnchantment(Enchantment.PROTECTION_FALL, 4);

            for (ItemStack itemStack : armour) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                player.getInventory().addItem(itemStack);
            }

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

            ItemStack itemStack = new ItemStack(ItemID.BOW);
            itemStack.addEnchantment(Enchantment.ARROW_DAMAGE, 5);
            itemStack.addEnchantment(Enchantment.ARROW_FIRE, 1);
            itemStack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
            itemStack.addEnchantment(Enchantment.ARROW_KNOCKBACK, 2);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(ChatColor.RED + "God Bow");
            itemStack.setItemMeta(itemMeta);
            player.getInventory().addItem(itemStack);

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

            ItemStack itemStack = new ItemStack(ItemID.DIAMOND_SWORD);
            itemStack.addEnchantment(Enchantment.DAMAGE_ALL, 5);
            itemStack.addEnchantment(Enchantment.DAMAGE_ARTHROPODS, 5);
            itemStack.addEnchantment(Enchantment.DAMAGE_UNDEAD, 5);
            itemStack.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            itemStack.addEnchantment(Enchantment.KNOCKBACK, 2);
            itemStack.addEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
            ItemMeta itemMeta = itemStack.getItemMeta();
            itemMeta.setDisplayName(ChatColor.RED + "God Sword");
            itemStack.setItemMeta(itemMeta);
            player.getInventory().addItem(itemStack);

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
    }

    public class LostAncientItem {

        @Command(aliases = {"armor"},
                usage = "<player>", desc = "Return a player's Ancient Armour",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.ancient.armor"})
        public void lostArmorCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            ItemStack[] armour = new ItemStack[4];
            armour[0] = new ItemStack(ItemID.CHAINMAIL_HELMET);
            armour[1] = new ItemStack(ItemID.CHAINMAIL_CHEST);
            armour[2] = new ItemStack(ItemID.CHAINMAIL_PANTS);
            armour[3] = new ItemStack(ItemID.CHAINMAIL_BOOTS);

            ItemMeta meta = armour[0].getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Ancient Helmet");
            armour[0].setItemMeta(meta);

            meta = armour[1].getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Ancient Chestplate");
            armour[1].setItemMeta(meta);

            meta = armour[2].getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Ancient Leggings");
            armour[2].setItemMeta(meta);

            meta = armour[3].getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Ancient Boots");
            armour[3].setItemMeta(meta);

            armour[0].addEnchantment(Enchantment.OXYGEN, 3);
            armour[0].addEnchantment(Enchantment.WATER_WORKER, 1);

            armour[3].addEnchantment(Enchantment.PROTECTION_FALL, 4);

            for (ItemStack itemStack : armour) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                itemStack.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                itemStack.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
                player.getInventory().addItem(itemStack);
            }

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

    public class LostMiscItem {

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