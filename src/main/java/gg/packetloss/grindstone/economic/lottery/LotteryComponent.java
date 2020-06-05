/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.*;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.economic.lottery.mysql.MySQLLotteryTicketDatabase;
import gg.packetloss.grindstone.economic.lottery.mysql.MySQLLotteryWinnerDatabase;
import gg.packetloss.grindstone.events.economy.MarketPurchaseEvent;
import gg.packetloss.grindstone.exceptions.NotFoundException;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.probability.WeightedPicker;
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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;


@ComponentInformation(friendlyName = "Lottery", desc = "Can you win it big?")
@Depend(plugins = {"Vault"}, components = {DataBaseComponent.class})
public class LotteryComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;

    private static final String LOTTERY_BANK_ACCOUNT = "Lottery";
    private static double MIN_WINNING;
    private List<Player> recentList = new ArrayList<>();
    private LotteryTicketDatabase lotteryTicketDatabase;
    private LotteryWinnerDatabase lotteryWinnerDatabase;
    private static Economy economy = null;

    @Override
    public void enable() {
        // FIXME: Work around for database load order issue.
        server.getScheduler().runTaskLater(inst, () -> {
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

            long ticks = TimeUtil.getTicksTill(17, 6);
            server.getScheduler().scheduleSyncRepeatingTask(inst, runLottery, ticks, 20 * 60 * 60 * 24 * 7);

            long nextHour = TimeUtil.getTicksTillHour();
            server.getScheduler().scheduleSyncRepeatingTask(inst, broadcastLottery, nextHour, 20 * 60 * 60);
        }, 1);
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
        @Setting("cpu.percentage-guaranteed")
        public double cpuPercentageGuaranteed = .5;
        @Setting("cpu.players")
        public int cpuPlayers = 20;
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMarketPurchase(MarketPurchaseEvent event) {
        // Deposit into the lottery account
        double lottery = event.getTotalCost() * .03;
        economy.bankDeposit(LOTTERY_BANK_ACCOUNT, lottery);
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
                block.breakNaturally();
            }
            event.setLine(0, "[Lottery]");
            try {
                int ticketCount = Integer.parseInt(event.getLine(1).trim());
                if (ticketCount > config.maxSellCount || ticketCount < 0) {
                    ChatUtil.sendError(player, "The third line must be a number below: "
                            + ChatUtil.makeCountString(config.maxSellCount, "."));
                    event.setCancelled(true);
                    block.breakNaturally();
                }
                event.setLine(2, "for");
                event.setLine(3, String.valueOf(config.ticketPrice * ticketCount));
            } catch (NumberFormatException e) {
                ChatUtil.sendError(player, "The third line must be a number below: "
                        + ChatUtil.makeCountString(config.maxSellCount, "."));
                event.setCancelled(true);
                block.breakNaturally();
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

        int maxTicketPurchase = (int) (economy.getBalance(player) / config.ticketPrice);
        int oldTicketCount = lotteryTicketDatabase.getTickets(player.getUniqueId());

        int newTicketCount = Math.min(config.maxPerLotto, oldTicketCount + Math.min(count, maxTicketPurchase));
        int ticketsBought = newTicketCount - oldTicketCount;

        if (!economy.has(player, config.ticketPrice * ticketsBought)) {
            throw new CommandException("You do not have enough " + economy.currencyNamePlural() + ".");
        }

        if (ticketsBought > 0) {
            economy.withdrawPlayer(player, config.ticketPrice * ticketsBought);

            lotteryTicketDatabase.addTickets(player.getUniqueId(), ticketsBought);
            lotteryTicketDatabase.save();
        }

        if (ticketsBought * config.ticketPrice != 1) {
            ChatUtil.sendNotice(player, "You purchased: "
                    + ChatUtil.makeCountString(ticketsBought, " tickets for: ")
                    + ChatUtil.makeCountString(economy.format(ticketsBought * config.ticketPrice), "."));
        } else {
            ChatUtil.sendNotice(player, "You purchased: "
                    + ChatUtil.makeCountString(ticketsBought, " tickets for: ")
                    + ChatUtil.makeCountString(economy.format(ticketsBought * config.ticketPrice), "."));
        }
        recentList.add(player);
        server.getScheduler().scheduleSyncDelayedTask(inst, () -> recentList.remove(player), 1);
    }

    private int calculateCPUTicketCount() {
        int guaranteedPerCPU = (int) (config.maxPerLotto * config.cpuPercentageGuaranteed);
        int chanceTicketsPerCPU = config.maxPerLotto - guaranteedPerCPU;

        int guaranteedTickets = guaranteedPerCPU * config.cpuPlayers;
        int chanceTickets = ChanceUtil.getRandom(chanceTicketsPerCPU * config.cpuPlayers);

        return guaranteedTickets + chanceTickets;
    }

    private void handleWinner(LotteryWinner winner) {
        economy.bankWithdraw(LOTTERY_BANK_ACCOUNT, economy.bankBalance(LOTTERY_BANK_ACCOUNT).balance);

        Bukkit.broadcastMessage(ChatColor.YELLOW + winner.getName() + " has won: " +
                ChatUtil.makeCountString(economy.format(winner.getAmt()), " via the lottery!"));

        if (winner.isBot()) {
            lotteryWinnerDatabase.addCPUWin(winner.getAmt());
        } else {
            economy.depositPlayer(winner.getAsOfflinePlayer(), winner.getAmt());
            lotteryWinnerDatabase.addWinner(winner.getPlayerID(), winner.getAmt());
        }

        lotteryWinnerDatabase.save();

        lotteryTicketDatabase.clearTickets();
        lotteryTicketDatabase.addCPUTickets(calculateCPUTicketCount());
        lotteryTicketDatabase.save();
    }

    public void completeLottery() {
        UUID name;
        try {
            name = findNewMillionaire();
        } catch (NotFoundException ex) {
            return;
        }

        double cash = getWinnerCash();

        handleWinner(new LotteryWinner(name, cash));
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

    private UUID findNewMillionaire() throws NotFoundException {
        List<LotteryTicketEntry> ticketDB = lotteryTicketDatabase.getTickets();
        if (ticketDB.size() < 2 && config.cpuPlayers < 1) throw new NotFoundException();

        WeightedPicker<UUID> tickets = new WeightedPicker<>();
        for (LotteryTicketEntry lotteryTicket : ticketDB) {
            tickets.add(lotteryTicket.getPlayerID(), lotteryTicket.getTicketCount());
        }

        return tickets.pick();
    }
}