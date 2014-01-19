package com.skelril.aurora.city.engine.minigame;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.player.GeneralPlayerUtil;
import com.skelril.aurora.util.player.PlayerState;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public abstract class MinigameComponent extends BukkitComponent implements Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    protected final String casualName;
    protected final String name;

    protected GameProgress progress = GameProgress.DONE;
    protected final ConcurrentHashMap<String, PlayerGameState> playerState = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, PlayerGameState> goneState = new ConcurrentHashMap<>();
    protected Set<Character> gameFlags = new HashSet<>();

    public MinigameComponent(String casualName, String name) {

        this.casualName = casualName;
        this.name = name;
    }

    @Override
    public void enable() {

    }

    // Region Methods
    public abstract boolean probe();

    // Game Methods
    public void begin(Set<Character> flags, int tickDelay) {

        initialize(flags);
        server.getScheduler().runTaskLater(inst, new Runnable() {
            @Override
            public void run() {
                start();
            }
        }, tickDelay);
    }

    public void initialize(Set<Character> flags) {

        gameFlags.addAll(flags);

        progress = GameProgress.INITIALIZED;
    }

    public void start() {

        progress = GameProgress.ACTIVE;
    }

    public abstract void printFlags();

    public void end() {

        progress = GameProgress.ENDING;

        Iterator<Map.Entry<String, PlayerGameState>> it = playerState.entrySet().iterator();

        while (it.hasNext()) {
            PlayerGameState state = it.next().getValue();

            try {
                removeFromTeam(PlayerUtil.matchPlayerExactly(null, state.getOwnerName()), true);
            } catch (CommandException e) {
                goneState.put(state.getOwnerName(), state);
            }
            it.remove();
        }

        gameFlags.clear();

        progress = GameProgress.DONE;
    }

    public boolean isGameInitialised() {

        return progress.level > 0;
    }

    public boolean isGameActive() {

        return progress == GameProgress.ACTIVE;
    }

    public abstract Player[] getContainedPlayers();

    // Team Methods
    public boolean addToTeam(Player player, int team, Set<Character> flags) {

        PlayerState state = GeneralPlayerUtil.makeComplexState(player);
        state.setLocation(player.getLocation());
        playerState.put(player.getName(), new PlayerGameState(state, team));

        return true;
    }

    public int getTeam(Player player) {

        PlayerGameState state = playerState.get(player.getName());

        if (state == null) {
            return -1;
        }

        return state.getTeamNumber();
    }

    public void left(Player player) {

        PlayerGameState state = playerState.get(player.getName());
        goneState.put(state.getOwnerName(), state);
        playerState.remove(state.getOwnerName());
    }

    protected void restore(Player player, PlayerGameState state) {

        // Clear Player
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Teleport Player
        player.teleport(state.getLocation());

        // Restore the contents
        player.getInventory().setArmorContents(state.getArmourContents());
        player.getInventory().setContents(state.getInventoryContents());
        player.setHealth(Math.min(player.getMaxHealth(), state.getHealth()));
        player.setFoodLevel(state.getHunger());
        player.setSaturation(state.getSaturation());
        player.setExhaustion(state.getExhaustion());
        player.setLevel(state.getLevel());
        player.setExp(state.getExperience());

        if (player.getVehicle() != null) {
            player.getVehicle().eject();
        }
    }

    public void removeFromTeam(Player player, boolean forced) {

        if (!playerState.containsKey(player.getName())) return;
        restore(player, playerState.get(player.getName()));
        playerState.remove(player.getName());
    }

    public void removeGoneFromTeam(Player player, boolean forced) {

        if (!goneState.containsKey(player.getName())) return;
        restore(player, goneState.get(player.getName()));
        goneState.remove(player.getName());
    }

    public void joinCmd(CommandContext args, CommandSender sender) throws CommandException {

        if (!(sender instanceof Player)) sender.sendMessage("You must be a player to use this command.");
        Player targetPlayer = (Player) sender;

        int teamNumber;

        if (args.argsLength() <= 1) {
            inst.checkPermission(sender, "aurora." + name + ".self.join");
            if (isGameInitialised()) {
                throw new CommandException("You cannot add players while a " + casualName + " is active!");
            } else if (getTeam(targetPlayer) != -1) {
                throw new CommandException("You are already in a " + casualName + "!");
            } else if (targetPlayer.isSleeping()) {
                throw new CommandException("You cannot join while sleeping.");
            }
            teamNumber = args.getInteger(0, 0);
        } else {
            targetPlayer = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
            inst.checkPermission(sender, targetPlayer.getWorld(), "aurora." + name + ".other.join");
            if (isGameInitialised()) {
                throw new CommandException("You cannot add players while a " + casualName + " is active!");
            } else if (getTeam(targetPlayer) != -1) {
                throw new CommandException("That player is already in a " + casualName + "!");
            } else if (targetPlayer.isSleeping()) {
                throw new CommandException("That player is sleeping.");
            }
            teamNumber = args.getInteger(1, 0);
        }

        if (teamNumber > 2 || teamNumber < 0) {
            throw new CommandException("Valid teams: 0, 1, 2.");
        }

        if (!addToTeam(targetPlayer, teamNumber, args.getFlags())) {
            removeFromTeam(targetPlayer, true);
            throw new CommandException("That player couldn't be added to the " + casualName + "!");
        }

        Player[] containedPlayers = getContainedPlayers();
        if (containedPlayers.length > 1) {
            ChatUtil.sendNotice(targetPlayer, ChatColor.DARK_GREEN, "Currently present players:");
            for (Player player : containedPlayers) {
                if (!player.isValid() || targetPlayer.equals(player)) continue;
                ChatUtil.sendNotice(targetPlayer, ChatColor.GREEN, player.getName());
                ChatUtil.sendNotice(player, ChatColor.DARK_GREEN,
                        targetPlayer.getName() + " has joined the " + casualName + ".");
            }
        }
    }

    public void leaveCmd(CommandContext args, CommandSender sender) throws CommandException {

        Player targetPlayer;

        if (args.argsLength() > 0) {
            targetPlayer = PlayerUtil.matchSinglePlayer(sender, args.getString(0));
            inst.checkPermission(sender, targetPlayer.getWorld(), "aurora." + name + ".other.leave");
        } else {
            targetPlayer = PlayerUtil.checkPlayer(sender);
            inst.checkPermission(sender, "aurora." + name + ".self.leave");
        }

        if (getTeam(targetPlayer) == -1) {
            throw new CommandException("That player is not currently in a " + casualName + ".");
        }

        removeFromTeam(targetPlayer, false);

        for (Player player : getContainedPlayers()) {
            if (!player.isValid() || targetPlayer.equals(player)) continue;
            ChatUtil.sendNotice(player, ChatColor.DARK_GREEN,
                    targetPlayer.getName() + " has left the " + casualName + ".");
        }
    }

    public void resetCmd(CommandContext args, CommandSender sender) throws CommandException {

        inst.checkPermission(sender, "aurora." + name + ".reset");

        if (args.hasFlag('p')) {
            probe();
        }
        end();
    }

    public void startCmd(CommandContext args, CommandSender sender) throws CommandException {

        inst.checkPermission(sender, "aurora." + name + ".start");

        if (playerState.size() < 2) {
            throw new CommandException("You need more players to start a " + casualName + ".");
        } else if (isGameInitialised()) {
            throw new CommandException("This " + casualName + " has already been initialised.");
        }

        begin(args.getFlags(), 20 * 9 * (args.hasFlag('q') ? 1 : 5));

        if (gameFlags.size() > 0) {
            printFlags();
        }
    }
}
