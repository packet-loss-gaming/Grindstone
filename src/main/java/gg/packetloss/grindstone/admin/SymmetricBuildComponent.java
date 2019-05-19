/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
        return session.isEnabled();
    }

    private void mirrorX(Vector newPos, Vector mirrorPos, Vector position) {
        if (position.getX() < mirrorPos.getX()) {
            double diff = mirrorPos.getX() - position.getX();
            newPos.setX(mirrorPos.getX() + diff);
        } else {
            double diff = position.getX() - mirrorPos.getX();
            newPos.setX(mirrorPos.getX() - diff);
        }
    }

    private void mirrorY(Vector newPos, Vector mirrorPos, Vector position) {
        if (position.getY() < mirrorPos.getY()) {
            double diff = mirrorPos.getY() - position.getY();
            newPos.setY(mirrorPos.getY() + diff);
        } else {
            double diff = position.getY() - mirrorPos.getY();
            newPos.setY(mirrorPos.getY() - diff);
        }
    }

    private void mirrorZ(Vector newPos, Vector mirrorPos, Vector position) {
        if (position.getZ() < mirrorPos.getZ()) {
            double diff = mirrorPos.getZ() - position.getZ();
            newPos.setZ(mirrorPos.getZ() + diff);
        } else {
            double diff = position.getZ() - mirrorPos.getZ();
            newPos.setZ(mirrorPos.getZ() - diff);
        }
    }

    private List<Vector> getMirrorPositions(Player player, Vector position) {
        SymmetricSession session = sessions.getSession(SymmetricSession.class, player);

        Vector mirrorPos = session.getPosition();

        List<Vector> mirrorPositions = new ArrayList<>();

        if (session.isEnabledMirror(MirrorDirection.X)) {
            Vector newPos = position.clone();
            mirrorX(newPos, mirrorPos, position);
            mirrorPositions.add(newPos);
        }

        if (session.isEnabledMirror(MirrorDirection.Y)) {
            Vector newPos = position.clone();
            mirrorY(newPos, mirrorPos, position);
            mirrorPositions.add(newPos);
        }

        if (session.isEnabledMirror(MirrorDirection.Z)) {
            Vector newPos = position.clone();
            mirrorZ(newPos, mirrorPos, position);
            mirrorPositions.add(newPos);
        }

        if (session.isEnabledMirror(MirrorDirection.X) && session.isEnabledMirror(MirrorDirection.Z)) {
            Vector newPos = position.clone();
            mirrorX(newPos, mirrorPos, position);
            mirrorZ(newPos, mirrorPos, position);
            mirrorPositions.add(newPos);
        }

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

    public class Commands {
        @Command(aliases = {"symmetry"}, desc = "Symmetry Commands")
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
        }

        @Command(aliases = {"setpoint", "sp"}, usage = "<x> <y> <z>",
                 desc = "Set the point of symmetry", min = 3, max = 3)
        public void setPoint(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);

            SymmetricSession session = sessions.getSession(SymmetricSession.class, player);
            session.setPosition(new Vector(args.getDouble(0), args.getDouble(1), args.getDouble(2)));

            ChatUtil.sendNotice(player, "Point of symmetry set to: " + session.getPosition().toString() + "!");
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
            } catch (IllegalArgumentException ex) {
                throw new CommandException("No such direction! Valid directions: 'X', 'Y', 'Z'.");
            }
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
            return position.clone();
        }

        public void setMirror(MirrorDirection direction, boolean active) {
            mirrors[direction.ordinal()] = active;
        }

        public boolean isEnabledMirror(MirrorDirection direction) {
           return mirrors[direction.ordinal()];
        }
    }
}
