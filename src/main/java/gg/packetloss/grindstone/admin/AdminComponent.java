/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.god.GodComponent;
import com.sk89q.commandbook.component.info.InfoComponent;
import com.sk89q.commandbook.util.InputUtil;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.events.DumpPlayerInventoryEvent;
import gg.packetloss.grindstone.events.PlayerAdminModeChangeEvent;
import gg.packetloss.grindstone.events.apocalypse.ApocalypsePersonalSpawnEvent;
import gg.packetloss.grindstone.exceptions.ConflictingPlayerStateException;
import gg.packetloss.grindstone.state.player.PlayerStateComponent;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.item.InventoryUtil;
import gg.packetloss.grindstone.util.player.GeneralPlayerUtil;
import net.milkbowl.vault.permission.Permission;
import org.apache.commons.lang.Validate;
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
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ComponentInformation(friendlyName = "Admin", desc = "Player Administration commands.")
@Depend(plugins = {"WorldEdit, Vault"}, components = {GodComponent.class, PlayerStateComponent.class})
public class AdminComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private GodComponent godComponent;
    @InjectComponent
    private PlayerStateComponent stateComponent;

    private Permission permission = null;

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
        if (!(plugin instanceof WorldEditPlugin)) return null;

        return (WorldEditPlugin) plugin;
    }

    private boolean setupPermissions() {

        RegisteredServiceProvider<Permission> permissionProvider = server.getServicesManager().getRegistration(net
                .milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) permission = permissionProvider.getProvider();

        return (permission != null);
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
                try {
                    stateComponent.pushState(PlayerStateKind.ADMIN, player);
                    permission.playerAddGroup((World) null, player.getName(), "Admin");
                } catch (IOException | ConflictingPlayerStateException ex) {
                    ex.printStackTrace();
                }
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
                try {
                    // Restore the previous state
                    stateComponent.popState(PlayerStateKind.ADMIN, player);

                    // Change Permissions
                    permission.playerRemoveGroup((World) null, player.getName(), "Admin");
                    Validate.isTrue(!isAdmin(player));
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

        boolean wasFlying = player.isFlying();

        if (!player.getGameMode().equals(GameMode.SURVIVAL)) player.setGameMode(GameMode.SURVIVAL);
        GeneralPlayerUtil.takeFlightSafely(player);

        if (wasFlying) {
            player.setFallDistance(0);
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (isAdmin(player)) {
            event.getDrops().clear();
            event.setDroppedExp(0);

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

        @Command(aliases = {"deadmin"},
                usage = "", desc = "Leave Admin Mode",
                flags = "k", min = 0, max = 0)
        public void deadminModeCmd(CommandContext args, CommandSender sender) throws CommandException {

            Player player = PlayerUtil.checkPlayer(sender);

            if (!isAdmin(player)) {
                throw new CommandException("You were not in admin mode!");
            }

            if (!args.hasFlag('k') && !stateComponent.hasValidStoredState(PlayerStateKind.ADMIN, player)) {
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

        @Command(aliases = {"openinventory", "openinv"},
                 usage = "<player>", desc = "View a player's inventory",
                 flags = "e", min = 1, max = 1)
        @CommandPermissions({"aurora.admin.user.openinv"})
        public void openInvCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player admin = PlayerUtil.checkPlayer(sender);
            Player target = InputUtil.PlayerParser.matchSinglePlayer(sender, args.getString(0));

            if (args.hasFlag('e')) {
                admin.openInventory(target.getEnderChest());
                ChatUtil.sendNotice(sender, "Viewing ender chest inventory of: " + target.getName());
            } else {
                admin.openInventory(target.getInventory());
                ChatUtil.sendNotice(sender, "Viewing inventory of: " + target.getName());
            }
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