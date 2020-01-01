/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.component.session.PersistentSession;
import com.sk89q.commandbook.component.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Symmetric Build", desc = "Build symmetric structures faster.")
@Depend(components = {SessionComponent.class})
public class SymmetricBuildComponent extends BukkitComponent implements Listener {
    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private final double MAX_TELEPORT_DISTANCE = 200;
    private final double MAX_TELEPORT_DISTANCE_SQ = Math.pow(MAX_TELEPORT_DISTANCE, 2);

    @InjectComponent
    private SessionComponent sessions;

    @Override
    public void enable() {
        registerCommands(Commands.class);
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    private boolean isBuildingWithSymmetricBuilding(Player player) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            return false;
        }

        SymmetricSession session = sessions.getSession(SymmetricSession.class, player);
        return session.isEnabled() && session.getPosition() != null;
    }

    private void mirrorX(Vector mirrorPosition, List<Vector> mirrorPositions) {
        List<Vector> producedPositions = new ArrayList<>();

        for (Vector position : mirrorPositions) {
            Vector newPosition = position.clone();
            if (position.getX() < mirrorPosition.getX()) {
                double diff = mirrorPosition.getX() - position.getX();
                newPosition.setX(mirrorPosition.getX() + diff);
            } else {
                double diff = position.getX() - mirrorPosition.getX();
                newPosition.setX(mirrorPosition.getX() - diff);
            }
            producedPositions.add(newPosition);
        }

        mirrorPositions.addAll(producedPositions);
    }

    private void mirrorY(Vector mirrorPosition, List<Vector> mirrorPositions) {
        List<Vector> producedPositions = new ArrayList<>();

        for (Vector position : mirrorPositions) {
            Vector newPosition = position.clone();
            if (position.getY() < mirrorPosition.getY()) {
                double diff = mirrorPosition.getY() - position.getY();
                newPosition.setY(mirrorPosition.getY() + diff);
            } else {
                double diff = position.getY() - mirrorPosition.getY();
                newPosition.setY(mirrorPosition.getY() - diff);
            }
            producedPositions.add(newPosition);
        }

        mirrorPositions.addAll(producedPositions);
    }

    private void mirrorZ(Vector mirrorPosition, List<Vector> mirrorPositions) {
        List<Vector> producedPositions = new ArrayList<>();

        for (Vector position : mirrorPositions) {
            Vector newPosition = position.clone();
            if (position.getZ() < mirrorPosition.getZ()) {
                double diff = mirrorPosition.getZ() - position.getZ();
                newPosition.setZ(mirrorPosition.getZ() + diff);
            } else {
                double diff = position.getZ() - mirrorPosition.getZ();
                newPosition.setZ(mirrorPosition.getZ() - diff);
            }
            producedPositions.add(newPosition);
        }

        mirrorPositions.addAll(producedPositions);
    }

    private List<Vector> getMirrorPositions(Player player, Vector position) {
        SymmetricSession session = sessions.getSession(SymmetricSession.class, player);

        Vector mirrorPosition = session.getPosition();

        List<Vector> mirrorPositions = new ArrayList<>();

        // Add the current block to "bootstrap" the mirroring.
        mirrorPositions.add(position);

        if (session.isEnabledMirror(MirrorDirection.X)) {
            mirrorX(mirrorPosition, mirrorPositions);
        }

        if (session.isEnabledMirror(MirrorDirection.Y)) {
            mirrorY(mirrorPosition, mirrorPositions);
        }

        if (session.isEnabledMirror(MirrorDirection.Z)) {
            mirrorZ(mirrorPosition, mirrorPositions);
        }

        // Remove the current block as whatever change needs to be mirrored already
        // occurred there, and we no longer need it to calculate any mirrors.
        mirrorPositions.remove(0);

        return mirrorPositions;
    }

    private void mirrorBlockPlace(Player player, Block sourceBlock) {
        BlockState sourceState = sourceBlock.getState();

        List<Vector> positions = getMirrorPositions(player, sourceBlock.getLocation().toVector());
        for (Vector pos : positions) {
            Block block = sourceBlock.getWorld().getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
            BlockState state = block.getState();
            state.setType(sourceState.getType());
            state.setData(sourceState.getData());
            state.update(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isBuildingWithSymmetricBuilding(player)) {
            return;
        }

        mirrorBlockPlace(player, event.getBlock());
    }

    private void mirrorBlockBreak(Player player, Block block) {
        List<Vector> positions = getMirrorPositions(player, block.getLocation().toVector());
        for (Vector pos : positions) {
            block.getWorld().getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()).setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isBuildingWithSymmetricBuilding(player)) {
            return;
        }

        mirrorBlockBreak(player, event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // We only need to disable symmetric building when the new game mode is not creative mode
        if (event.getNewGameMode() == GameMode.CREATIVE) {
            return;
        }

        Player player = event.getPlayer();

        // Force disable symmetric building so that if the player reenters creative mode with a point of
        // symmetry a long distance away, they don't accidentally destroy things.
        SymmetricSession session = sessions.getSession(SymmetricSession.class, player);
        session.setEnabled(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getWorld().equals(to.getWorld())) {
            if (LocationUtil.distanceSquared2D(from, to) < MAX_TELEPORT_DISTANCE_SQ) {
                return;
            }
        }

        Player player = event.getPlayer();
        SymmetricSession session = sessions.getSession(SymmetricSession.class, player);
        if (session.isEnabled()) {
            session.setEnabled(false);
            ChatUtil.sendNotice(player, "Symmetry disable due to teleport distance.");
        }
    }

    private String format(boolean b) {
        if (b) {
            return ChatColor.DARK_GREEN + "TRUE";
        } else {
            return ChatColor.RED + "FALSE";
        }
    }

    private void printStatus(Player player) {
        SymmetricSession session = sessions.getSession(SymmetricSession.class, player);

        ChatUtil.sendNotice(player, ChatColor.GOLD + "Symmetry Status:");
        ChatUtil.sendNotice(player, "  Enabled: " + format(session.isEnabled()));
        ChatUtil.sendNotice(player, "  Point of Symmetry: " + ChatColor.BLUE + ChatUtil.toString(session.getPosition()));
        ChatUtil.sendNotice(player, "  Mirroring X: " + format(session.isEnabledMirror(MirrorDirection.X)));
        ChatUtil.sendNotice(player, "  Mirroring Y: " + format(session.isEnabledMirror(MirrorDirection.Y)));
        ChatUtil.sendNotice(player, "  Mirroring Z: " + format(session.isEnabledMirror(MirrorDirection.Z)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        SymmetricSession session = sessions.getSession(SymmetricSession.class, event.getPlayer());
        session.setEnabled(false);
    }

    public class Commands {
        @Command(aliases = {"symmetry", "/sym"}, desc = "Symmetry Commands")
        @NestedCommand({SymmetryCommands.class})
        @CommandPermissions({"aurora.symmetry"})
        public void symmetryCommands(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class SymmetryCommands {
        @Command(aliases = {"toggle", "t"}, desc = "Enable/disable symmetry building",
                 min = 0, max = 0)
        public void toggleSymmetry(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            SymmetricSession session = sessions.getSession(SymmetricSession.class, player);
            session.setEnabled(!session.isEnabled());

            ChatUtil.sendNotice(
              player,
              "Symmetry building " + (session.isEnabled() ? "enabled" : "disabled") + "!"
            );

            printStatus(player);
        }

        @Command(aliases = {"setpoint", "sp"}, usage = "<x> <y> <z>",
                 desc = "Set the point of symmetry", min = 3, max = 3)
        public void setPoint(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            SymmetricSession session = sessions.getSession(SymmetricSession.class, player);
            session.setPosition(new Vector(args.getDouble(0), args.getDouble(1), args.getDouble(2)));

            ChatUtil.sendNotice(player, "Point of symmetry set to: " + ChatUtil.toString(session.getPosition()) + "!");

            printStatus(player);
        }

        @Command(aliases = {"togglemirror", "tm"}, usage = "<direction>",
                 desc = "Set the symmetric mirroring direction", min = 1, max = 1)
        public void toggleMirror(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            SymmetricSession session = sessions.getSession(SymmetricSession.class, player);

            try {
                MirrorDirection direction = MirrorDirection.valueOf(args.getString(0).toUpperCase());
                session.setMirror(direction, !session.isEnabledMirror(direction));

                ChatUtil.sendNotice(
                  player,
                  "Mirror direction \"" + direction + "\" " +
                    (session.isEnabledMirror(direction) ? "enabled" : "disabled") +"!"
                );

                printStatus(player);
            } catch (IllegalArgumentException ex) {
                throw new CommandException("No such direction! Valid directions: 'X', 'Y', 'Z'.");
            }
        }

        @Command(aliases = {"status"}, desc = "View symmetry mode status", max = 0)
        public void viewStatus(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            printStatus(player);
        }
    }

    public enum MirrorDirection {
        X,
        Y,
        Z
    }

    private static class SymmetricSession extends PersistentSession {
        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private boolean enabled = false;
        private Vector position = null;
        private boolean[] mirrors = {false, false, false};

        protected SymmetricSession() {
            super(MAX_AGE);
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setPosition(Vector position) {
            this.position = position;
        }

        public Vector getPosition() {
            return position == null ? null : position.clone();
        }

        public void setMirror(MirrorDirection direction, boolean active) {
            mirrors[direction.ordinal()] = active;
        }

        public boolean isEnabledMirror(MirrorDirection direction) {
           return mirrors[direction.ordinal()];
        }
    }
}
