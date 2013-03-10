package com.skelril.aurora.duel;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.player.GenericWealthStore;
import com.skelril.aurora.util.player.WealthStore;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Duel Component", desc = "Fighting at it's best!")
public class DuelComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private Set<Duel> duels = new HashSet<>();

    protected boolean isInDuel(Player player) {

        for (Duel duel : duels) {
            for (DuelTeam duelTeam : duel.getTeams()) {
                if (duelTeam == null)
                    continue;
                for (Player teamPlayer : duelTeam.getPlayers()) {
                    if (teamPlayer.equals(player))
                        return true;
                }
            }
        }
        return false;
    }

    protected Duel getDuel(Player player) {

        for (Duel duel : duels) {
            for (DuelTeam duelTeam : duel.getTeams()) {
                if (duelTeam == null)
                    continue;
                for (Player teamPlayer : duelTeam.getPlayers()) {
                    if (teamPlayer.equals(player))
                        return duel;
                }
            }
        }
        return null;
    }

    protected DuelTeam getDuelTeam(Player player, Duel duel) {

        for (DuelTeam duelTeam : duel.getTeams()) {
            if (duelTeam == null)
                continue;
            for (Player teamPlayer : duelTeam.getPlayers()) {
                if (teamPlayer.equals(player))
                    return duelTeam;
            }
        }
        return null;
    }

    protected List<ItemStack> getStake(GenericWealthStore[] duelStakes) {

        try {
            List<ItemStack> playerStake = new ArrayList<>();
            for (GenericWealthStore duelStake : duelStakes) {
                for (ItemStack itemStack : duelStake.getItemStacks()) {
                    playerStake.add(itemStack);
                }
            }

            if (playerStake.size() > 1) {
                return playerStake;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    protected GenericWealthStore[] getPlayerStakes(Player player) {

        try {
            DuelTeam duelTeam = getDuelTeam(player, getDuel(player));
            List<GenericWealthStore> playerDuelStakes = new ArrayList<>();
            for (GenericWealthStore duelStake : duelTeam.getStakes()) {
                if (duelStake.getOwnerName().equals(player.getName())) {
                    playerDuelStakes.add(duelStake);
                }
            }

            if (playerDuelStakes.size() > 1) {
                return (GenericWealthStore[]) playerDuelStakes.toArray();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    protected void transferStake(Duel duel, Player fromPlayer, Player toPlayer) {

        if (duel != null && duel.isStakedFight()) {
            // Get the Stake of fallen player
            GenericWealthStore[] playerStake = getPlayerStakes(fromPlayer);
            if (playerStake != null) {
                // Remove the stake from the fallen player's team
                DuelTeam fallenPlayersTeam = getDuelTeam(fromPlayer, duel);
                for (GenericWealthStore duelStake : playerStake) {
                    fallenPlayersTeam.getStakes().remove(duelStake);
                }
                // Get the objects from the old duel stake
                List<ItemStack> loot = getStake(playerStake);

                getDuelTeam(toPlayer, duel).getStakes().add(new WealthStore(toPlayer.getName(), loot));
            }
        }
    }

    protected void startDuel(Location startLoc, DuelTeam duelTeamOne, DuelTeam duelTeamTwo,
                             DuelTeam neutralPlayers, boolean stakedFight) {

        Duel duel = new Duel(duelTeamOne, duelTeamTwo, neutralPlayers, stakedFight);
        for (Player player : duel.getDuelPlayers()) {
            player.teleport(startLoc);
        }
    }

    protected boolean hasDuelStarted(Duel duel) {

        return duel.isActive();
    }

    protected void endGame(Duel duel, DuelWinner duelWinner) {

        duels.remove(duel);
        if (duelWinner == null) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "The duel was a draw!");
        } else if (duelWinner.isTeam()) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "The team: "
                    + ((DuelTeam) duelWinner.getWinner()).getTeamName() + " is victorious!");
        } else {
            Bukkit.broadcastMessage(ChatColor.GOLD + "The player: "
                    + ((Player) duelWinner.getWinner()).getDisplayName() + " is victorious!");
        }
    }

    private DuelWinner findWinner(Duel duel) {

        List<DuelTeam> activeTeams = new ArrayList<>();
        for (DuelTeam duelTeam : duel.getTeams()) {
            if (duelTeam == null)
                continue;
            if (duelTeam.getPlayers().size() > 0)
                activeTeams.add(duelTeam);
        }

        if (activeTeams.size() == 1) {
            if (!activeTeams.get(0).isNeutralTeam()) {
                return new DuelWinner(true, activeTeams.get(0));
            } else {
                if (activeTeams.get(0).getPlayers().size() == 1) {
                    Player[] players = (Player[]) activeTeams.get(0).getPlayers().toArray();
                    return new DuelWinner(false, players[0]);
                }
            }
        } else if (activeTeams.size() == 0) {
            endGame(duel, null);
        }
        return null;
    }

    @Override
    public void enable() {

        //CommandBook.registerEvents(this);
        //registerCommands(Commands.class);
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (isInDuel(player)) {
            Duel duel = getDuel(player);

            Player killer = player.getKiller();
            if (killer != null)
                transferStake(duel, player, killer);
            DuelWinner duelWinner = findWinner(duel);

            List<ItemStack> drops = event.getDrops();
            drops.clear();

            if (duelWinner != null) {
                endGame(duel, duelWinner);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        if (isInDuel(player)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot drop items while dueling!");
        }
    }

    @EventHandler
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {

        Player player = event.getPlayer();
        String[] args = event.getMessage().split(" ");

        if (!isInDuel(player))
            return;

        Duel duel = getDuel(player);

        if (duel.isStakedFight() && duel.isActive()) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You cannot use commands in this type of duel.");
        } else if (duel.isActive() && !args[0].equalsIgnoreCase("duel")) {
            event.setCancelled(true);
            ChatUtil.sendWarning(player, "You can only use duel commands in this type of duel.");
        }
    }

    @EventHandler
    public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

        Entity attackingEntity = event.getDamager();
        Entity defendingEntity = event.getEntity();

        // Figure out if the entities are players
        if (!(defendingEntity instanceof Player))
            return;
        Player defendingPlayer = (Player) defendingEntity;
        Player attackingPlayer;
        if (attackingEntity instanceof Player) {
            attackingPlayer = (Player) attackingEntity;
        } else if (attackingEntity instanceof Arrow) {
            if (!(((Arrow) attackingEntity).getShooter() instanceof Player))
                return;
            attackingPlayer = (Player) ((Arrow) attackingEntity).getShooter();
        } else {
            return;
        }

        // Figure out if this fight is related to a duel.
        if (!isInDuel(attackingPlayer) && !isInDuel(defendingPlayer))
            return;
        if (!isInDuel(attackingPlayer) && isInDuel(defendingPlayer)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(attackingPlayer, "Don't attack players that are dueling.");
            return;
        }
        if (!isInDuel(attackingPlayer))
            return;
        if (!isInDuel(defendingPlayer)) {
            event.setCancelled(true);
            ChatUtil.sendWarning(attackingPlayer, "Don't attack bystanders.");
            return;
        }
        if (!hasDuelStarted(getDuel(attackingPlayer))) {
            event.setCancelled(true);
            ChatUtil.sendWarning(attackingPlayer, "The game has not yet started!");
            return;
        }
        if ((getDuelTeam(attackingPlayer, getDuel(attackingPlayer)) == getDuelTeam(defendingPlayer,
                getDuel(defendingPlayer))) || getDuelTeam(attackingPlayer, getDuel(attackingPlayer)).isNeutralTeam()) {
            event.setCancelled(true);
            ChatUtil.sendWarning(attackingPlayer, "Don't hit your team mates!");
        } else {
            ChatUtil.sendNotice(attackingPlayer, "You've hit " + defendingPlayer.getDisplayName() + "!");
        }
    }

    public class Commands {

        @Command(aliases = {"duel", "challenge", "fight"}, desc = "Fight players")
        @NestedCommand({DuelCommands.class})
        public void duelCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class DuelCommands {


    }
}
