package us.arrowcraft.aurora.admin;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.GodComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.World;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import us.arrowcraft.aurora.NinjaComponent;
import us.arrowcraft.aurora.RogueComponent;
import us.arrowcraft.aurora.events.PlayerAdminModeChangeEvent;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.EnvironmentUtil;
import us.arrowcraft.aurora.util.LocationUtil;
import us.arrowcraft.aurora.util.player.PlayerState;

import java.util.HashMap;
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
    public void reload() {

        saveInventories();
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

            if (playerState.containsKey(player.getName())) deadmin(player);
        }
    }

    /**
     * This method is used to determine if the player is in Admin Mode.
     *
     * @param player - The player to check
     *
     * @return - true if the player is in Admin Mode
     */
    public boolean isAdmin(Player player) {

        return !getAdminState(player).equals(AdminState.MEMBER);
    }

    /**
     * This method is used to determine the {@link AdminState} of the player.
     *
     * @param player - The player to check
     *
     * @return - The {@link AdminState} of the player
     */
    public AdminState getAdminState(Player player) {

        if (permission.playerInGroup((World) null, player.getName(), "Admin")) {
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
     *
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
     *
     * @return - true if all active powers have been disabled
     */
    public boolean depowerPlayer(Player player) {

        if (worldEdit().getSession(player).hasSuperPickAxe()) worldEdit().getSession(player).disableSuperPickAxe();
        if (godComponent.hasGodMode(player)) godComponent.disableGodMode(player);
        if (player.getGameMode().equals(GameMode.CREATIVE)) player.setGameMode(GameMode.SURVIVAL);
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFallDistance(0);
        }
        return true;
    }

    /**
     * This method is used when removing a player from Admin Mode. Their {@link AdminState} will be set to the
     * lowest level possible.
     *
     * @param player - The player to remove from Admin Mode
     *
     * @return - true if all active powers and elevated permission levels have been removed
     */
    public boolean deadmin(Player player) {

        return depermission(player) && depowerPlayer(player);
    }

    /**
     * This method is used when removing a player's guild powers. Currently this only effects the {@link NinjaComponent}
     * and the {@link RogueComponent}.
     *
     * @param player - The player to disable guild powers for
     *
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
     *
     * @return - true if all active guild and admin powers have been disabled
     */
    public boolean standardizePlayer(Player player) {

        return deadmin(player) && deguildPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {

        final Player player = event.getPlayer();

        if (!player.isFlying() && (isAdmin(player) || offlinePlayerState.containsKey(player.getName()))) {
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
            deadmin(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

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

    @EventHandler
    public void onPlayerClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) return;

        //InventoryType.SlotType st = event.getSlotType();
        if (isAdmin((Player) event.getWhoClicked())
                && (event.getCurrentItem().getTypeId() != 0 || event.getCursor().getTypeId() != 0)
                && !(event.getInventory().getType().equals(InventoryType.PLAYER)
                || event.getInventory().getType().equals(InventoryType.CREATIVE)
                || event.getInventory().getType().equals(InventoryType.CRAFTING))) {
            event.setResult(Event.Result.DENY);
            ChatUtil.sendWarning((Player) event.getWhoClicked(), "You cannot do this while in admin mode.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

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
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (EnvironmentUtil.isValuableBlock(block)
                || isAdmin(player) && isDisabledBlock(block)
                || block.getTypeId() == BlockID.STONE_BRICK && block.getData() == 3) {
            block.breakNaturally(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (EnvironmentUtil.isValuableBlock(block) && !isAdmin(player)
                || isAdmin(player) && isDisabledBlock(block)) {
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

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (isAdmin(player)) {
            event.getDrops().clear();
            EnvironmentUtil.generateRadialEffect(player.getLocation(), Effect.POTION_BREAK);
        }
    }

    public class Commands {

        @Command(aliases = {"user"}, desc = "User Management Commands")
        @NestedCommand({NestedAdminCommands.class})
        public void userManagementCommands(CommandContext args, CommandSender sender) throws CommandException {

        }

        @Command(aliases = {"admin", "alivemin"},
                usage = "", desc = "Enter Admin Mode",
                flags = "", min = 0, max = 0)
        public void adminModeCmd(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            Player player = (Player) sender;

            if (!isAdmin(player)) {
                boolean admin;
                if (inst.hasPermission(player, "aurora.admin.adminmode.admin")) {
                    admin = admin(player, AdminState.ADMIN);
                } else if (inst.hasPermission(player, "aurora.admin.adminmode.moderator")) {
                    admin = admin(player, AdminState.MODERATOR);
                } else {
                    throw new CommandPermissionsException();
                }

                if (admin) {
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
                if (deadmin(player)) {
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
        @NestedCommand({LostItemCommands.class})
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

    public class LostItemCommands {

        @Command(aliases = {"godarmour"},
                usage = "<player>", desc = "Modify a player's permissions",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.arm"})
        public void lostGodArmourCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            ItemStack[] armour = new ItemStack[4];
            armour[0] = new ItemStack(ItemID.DIAMOND_HELMET);
            armour[1] = new ItemStack(ItemID.DIAMOND_CHEST);
            armour[2] = new ItemStack(ItemID.DIAMOND_PANTS);
            armour[3] = new ItemStack(ItemID.DIAMOND_BOOTS);

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

        @Command(aliases = {"godbow"},
                usage = "<player>", desc = "Modify a player's permissions",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.bow"})
        public void lostGodBowCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            ItemStack itemStack = new ItemStack(ItemID.BOW);
            itemStack.addEnchantment(Enchantment.ARROW_DAMAGE, 5);
            itemStack.addEnchantment(Enchantment.ARROW_FIRE, 1);
            itemStack.addEnchantment(Enchantment.ARROW_INFINITE, 1);
            itemStack.addEnchantment(Enchantment.ARROW_KNOCKBACK, 2);

            player.getInventory().addItem(itemStack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god bow.");
        }

        @Command(aliases = {"godsword"},
                usage = "<player>", desc = "Modify a player's permissions",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.lost.sword"})
        public void lostGodSwordCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.matchPlayerExactly(sender, args.getString(0));

            ItemStack itemStack = new ItemStack(ItemID.DIAMOND_SWORD);
            itemStack.addEnchantment(Enchantment.DAMAGE_ALL, 5);
            itemStack.addEnchantment(Enchantment.DAMAGE_ARTHROPODS, 5);
            itemStack.addEnchantment(Enchantment.DAMAGE_UNDEAD, 5);
            itemStack.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            itemStack.addEnchantment(Enchantment.KNOCKBACK, 2);
            itemStack.addEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);

            player.getInventory().addItem(itemStack);

            // Tell Admin
            ChatUtil.sendNotice(sender, "The player: " + player.getDisplayName()
                    + " has been given a new god sword.");
        }
    }
}