/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.economic.lottery;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.ItemID;
import com.skelril.aurora.economic.ImpersonalComponent;
import com.skelril.aurora.economic.lottery.mysql.MySQLLotteryTicketDatabase;
import com.skelril.aurora.economic.lottery.mysql.MySQLLotteryWinnerDatabase;
import com.skelril.aurora.exceptions.NotFoundException;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.CollectionUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.TimeUtil;
import com.skelril.aurora.util.player.GenericWealthStore;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Lottery", desc = "Can you win it big?")
@Depend(plugins = {"Vault"}, components = {ImpersonalComponent.class})
public class LotteryComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    ImpersonalComponent impersonalComponent;

    private LocalConfiguration config;

    private static String LOTTERY_BANK_ACCOUNT = "Lottery";
    private static double MIN_WINNING;
    private List<Player> recentList = new ArrayList<>();
    private LotteryTicketDatabase lotteryTicketDatabase;
    private LotteryWinnerDatabase lotteryWinnerDatabase;
    private static Economy economy = null;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        MIN_WINNING = config.maxPerLotto * config.ticketPrice * 1.25;

        lotteryTicketDatabase = new MySQLLotteryTicketDatabase();
        lotteryTicketDatabase.load();
        lotteryWinnerDatabase = new MySQLLotteryWinnerDatabase();
        lotteryWinnerDatabase.load();

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);

        setupEconomy();

        long ticks = TimeUtil.getTicksTill(17), nextHour = TimeUtil.getTicksTillHour();
        server.getScheduler().scheduleSyncRepeatingTask(inst, runLottery, ticks, 20 * 60 * 60 * 24);
        server.getScheduler().scheduleSyncRepeatingTask(inst, broadcastLottery, nextHour, 20 * 60 * 60);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private Runnable runLottery = this::completeLottery;

    private Runnable broadcastLottery = () -> broadcastLottery(server.getOnlinePlayers());


    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("ticket-price")
        public int ticketPrice = 20;
        @Setting("max.sell-count")
        public int maxSellCount = 50;
        @Setting("max.per-lotto")
        public int maxPerLotto = 150;
        @Setting("recent-length")
        public int recentLength = 5;
    }

    public class Commands {

        @Command(aliases = {"lottery", "lotto"}, desc = "Lottery Commands")
        @NestedCommand({NestedCommands.class})
        public void lotteryCmd(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {

        @Command(aliases = {"buy"},
                usage = "[amount]", desc = "Buy Lottery Tickets.",
                flags = "", min = 0, max = 1)
        @CommandPermissions({"aurora.lottery.ticket.buy.command"})
        public void lotteryBuyCmd(CommandContext args, CommandSender sender) throws CommandException {

            buyTickets(PlayerUtil.checkPlayer(sender), args.argsLength() > 0 ? args.getInteger(0) : 1);
        }

        @Command(aliases = {"draw"},
                usage = "", desc = "Trigger the lottery draw.",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.lottery.draw"})
        public void lotteryDrawCmd(CommandContext args, CommandSender sender) throws CommandException {

            ChatUtil.sendNotice(sender, "Lottery draw in progress.");
            completeLottery();
        }

        @Command(aliases = {"pot", "value"},
                usage = "", desc = "View the lottery pot size.",
                flags = "b", min = 0, max = 0)
        @CommandPermissions({"aurora.lottery.pot"})
        public void lotteryPotCmd(CommandContext args, CommandSender sender) throws CommandException {

            List<CommandSender> que = new ArrayList<>();

            que.add(sender);
            if (args.hasFlag('b') && sender.hasPermission("aurora.lottery.pot.broadcast")) {
                que.addAll(server.getOnlinePlayers());
            }

            broadcastLottery(que);
        }

        @Command(aliases = {"recent", "last", "previous"},
                usage = "", desc = "View the last lottery winner.",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.lottery.last"})
        public void lotteryLastCmd(CommandContext args, CommandSender sender) throws CommandException {

            ChatUtil.sendNotice(sender, ChatColor.GRAY, "Lottery - Recent winners:");
            List<LotteryWinner> winners = lotteryWinnerDatabase.getRecentWinner(config.recentLength);
            short number = 0;
            for (LotteryWinner winner : winners) {
                number++;
                ChatUtil.sendNotice(sender, "  " + ChatColor.GOLD + number + ". " + ChatColor.YELLOW
                        + winner.getName() + ChatColor.GOLD + " - " + ChatColor.WHITE + economy.format(winner.getAmt()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || block == null
                || !EnvironmentUtil.isSign(block)) return;

        Sign sign = (Sign) block.getState();

        if (sign.getLine(0).equals("[Lottery]") && inst.hasPermission(player, "aurora.lottery.ticket.buy.sign")) {
            if (!impersonalComponent.check(block, true)) return;
            try {
                int count = Integer.parseInt(sign.getLine(1).trim());
                buyTickets(player, count);
            } catch (NumberFormatException e) {
                block.breakNaturally();
            } catch (CommandException e) {
                ChatUtil.sendError(player, e.getMessage());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBlockPlace(SignChangeEvent event) {

        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (event.getLine(0).equalsIgnoreCase("[lottery]")) {
            if (!inst.hasPermission(player, "aurora.lottery.ticket.sell.sign")) {
                event.setCancelled(true);
                block.breakNaturally(new ItemStack(ItemID.SIGN, 1));
            }
            event.setLine(0, "[Lottery]");
            try {
                int ticketCount = Integer.parseInt(event.getLine(1).trim());
                if (ticketCount > config.maxSellCount || ticketCount < 0) {
                    ChatUtil.sendError(player, "The third line must be a number below: "
                            + ChatUtil.makeCountString(config.maxSellCount, "."));
                    event.setCancelled(true);
                    block.breakNaturally(new ItemStack(ItemID.SIGN, 1));
                }
                event.setLine(2, "for");
                event.setLine(3, String.valueOf(config.ticketPrice * ticketCount));
            } catch (NumberFormatException e) {
                ChatUtil.sendError(player, "The third line must be a number below: "
                        + ChatUtil.makeCountString(config.maxSellCount, "."));
                event.setCancelled(true);
                block.breakNaturally(new ItemStack(ItemID.SIGN, 1));
            }
        }
    }

    private boolean setupEconomy() {

        RegisteredServiceProvider<Economy> economyProvider = server.getServicesManager().getRegistration(net.milkbowl
                .vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    public LotteryTicketDatabase getLotteryTicketDatabase() {

        return lotteryTicketDatabase;
    }

    public void buyTickets(final Player player, int count) throws CommandException {

        if (recentList.contains(player)) return;

        String playerName = player.getName();

        int b = 0;
        int m = 0;
        int sold = 0;

        b = (int) (economy.getBalance(playerName) / config.ticketPrice);

        m = config.maxPerLotto - lotteryTicketDatabase.getTickets(playerName);

        if (b > m) {
            if (m > count) {
                sold = count;
            } else {
                sold = m;
            }
        } else if (m > b) {
            if (b > count) {
                sold = count;
            } else {
                sold = b;
            }
        }

        if (sold < 0) {
            sold = 0;
        }

        if (!economy.has(playerName, config.ticketPrice * sold)) {
            throw new CommandException("You do not have enough " + economy.currencyNamePlural() + ".");
        }

        if (sold > 0) {
            economy.withdrawPlayer(playerName, config.ticketPrice * sold);

            lotteryTicketDatabase.addTickets(playerName, sold);
            lotteryTicketDatabase.save();
        }

        if (sold * config.ticketPrice != 1) {
            ChatUtil.sendNotice(player, "You purchased: "
                    + ChatUtil.makeCountString(sold, " tickets for: ")
                    + ChatUtil.makeCountString(economy.format(sold * config.ticketPrice), "."));
        } else {
            ChatUtil.sendNotice(player, "You purchased: "
                    + ChatUtil.makeCountString(sold, " tickets for: ")
                    + ChatUtil.makeCountString(economy.format(sold * config.ticketPrice), "."));
        }
        recentList.add(player);
        server.getScheduler().scheduleSyncDelayedTask(inst, () -> recentList.remove(player), 10);
    }

    public void completeLottery() {

        String name;
        try {
            name = findNewMillionaire();
        } catch (NotFoundException ex) {
            return;
        }

        double cash = getWinnerCash();

        try {
            economy.bankWithdraw(LOTTERY_BANK_ACCOUNT, economy.bankBalance(LOTTERY_BANK_ACCOUNT).balance);
        } catch (Throwable t) {
        }
        economy.depositPlayer(name, cash);
        Bukkit.broadcastMessage(ChatColor.YELLOW + name + " has won: " +
                ChatUtil.makeCountString(economy.format(cash), " via the lottery!"));

        lotteryWinnerDatabase.addWinner(name, cash);
        lotteryWinnerDatabase.save();
        lotteryTicketDatabase.clearTickets();
        lotteryTicketDatabase.save();
    }

    public void broadcastLottery(Iterable<? extends CommandSender> senders) {

        for (CommandSender receiver : senders) {
            ChatUtil.sendNotice(receiver, "The lottery currently has: "
                    + ChatUtil.makeCountString(lotteryTicketDatabase.getTicketCount(), " tickets and is worth: ")
                    + ChatUtil.makeCountString(economy.format(getWinnerCash()),
                    "."));
        }
    }

    public double getWinnerCash() {

        double amt = lotteryTicketDatabase.getTicketCount() * config.ticketPrice * .75;

        EconomyResponse response = economy.bankBalance(LOTTERY_BANK_ACCOUNT);
        if (response.transactionSuccess()) {
            amt += response.balance;
        }
        return amt;
    }

    private String findNewMillionaire() throws NotFoundException {

        List<GenericWealthStore> ticketDB = lotteryTicketDatabase.getTickets();
        if (ticketDB.size() < 2) throw new NotFoundException();

        List<String> tickets = new ArrayList<>();
        for (GenericWealthStore lotteryTicket : ticketDB) {
            for (int i = 0; i < lotteryTicket.getValue(); i++) {
                tickets.add(lotteryTicket.getOwnerName());
            }
        }
        return CollectionUtil.getElement(tickets);
    }
}