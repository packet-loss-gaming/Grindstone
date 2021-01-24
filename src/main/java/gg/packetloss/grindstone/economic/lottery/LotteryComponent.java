/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.lottery;

import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.ComponentCommandRegistrar;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.economic.lottery.mysql.MySQLLotteryTicketDatabase;
import gg.packetloss.grindstone.economic.lottery.mysql.MySQLLotteryWinnerDatabase;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.events.economy.MarketPurchaseEvent;
import gg.packetloss.grindstone.exceptions.NotFoundException;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import gg.packetloss.grindstone.util.probability.WeightedPicker;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.ChatUtil.WHOLE_NUMBER_FORMATTER;


@ComponentInformation(friendlyName = "Lottery", desc = "Can you win it big?")
@Depend(plugins = {"Vault"}, components = {ChatBridgeComponent.class, DataBaseComponent.class, WalletComponent.class})
public class LotteryComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private ChatBridgeComponent chatBridge;
    @InjectComponent
    private WalletComponent wallet;

    private LocalConfiguration config;

    private LotteryState lotteryState = new LotteryState();
    private SingleFileFilesystemStateHelper<LotteryState> stateHelper;

    private List<Player> recentList = new ArrayList<>();
    private LotteryTicketDatabase lotteryTicketDatabase;
    private LotteryWinnerDatabase lotteryWinnerDatabase;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());

        try {
            stateHelper = new SingleFileFilesystemStateHelper<>("lottery.json", new TypeToken<>() { });
            stateHelper.load().ifPresent(loadedState -> lotteryState = loadedState);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ComponentCommandRegistrar registrar = CommandBook.getComponentRegistrar();
        registrar.registerTopLevelCommands((commandManager, registration) -> {
            registrar.registerAsSubCommand("lottery", "Lottery", commandManager, (innerCommandManager, innerRegistration) -> {
                innerRegistration.register(innerCommandManager, LotteryCommandsRegistration.builder(), new LotteryCommands(this, wallet));
            });
        });

        // FIXME: Work around for database load order issue.
        server.getScheduler().runTaskLater(inst, () -> {
            lotteryTicketDatabase = new MySQLLotteryTicketDatabase();
            lotteryTicketDatabase.load();
            lotteryWinnerDatabase = new MySQLLotteryWinnerDatabase();
            lotteryWinnerDatabase.load();

            //noinspection AccessStaticViaInstance
            inst.registerEvents(this);

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

    @Override
    public void disable() {
        try {
            stateHelper.save(lotteryState);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMarketPurchase(MarketPurchaseEvent event) {
        // Deposit into the lottery account
        double lotteryContribution = event.getTotalCost().doubleValue() * .03;
        lotteryState.profitFromMarketSales += lotteryContribution;
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

    public LotteryTicketDatabase getLotteryTicketDatabase() {

        return lotteryTicketDatabase;
    }

    private Text formatTickets(int amount) {
        return Text.of(
            Text.of(ChatColor.WHITE, WHOLE_NUMBER_FORMATTER.format(amount)),
            " ",
            amount == 1 ? "ticket" : "tickets"
        );
    }

    public void buyTickets(final Player player, int count) throws CommandException {
        if (recentList.contains(player)) return;

        int[] ticketsBought = {0};

        recentList.add(player);

        wallet.getBalance(player).thenApplyAsynchronously(
            (balance) -> {
                return balance.divide(new BigDecimal(config.ticketPrice)).intValue();
            },
            (ignored) -> {
                ErrorUtil.reportUnexpectedError(player);
            }
        ).thenComposeFailableAsynchronously(
            (maxTicketPurchase) -> {
                int oldTicketCount = lotteryTicketDatabase.getTickets(player.getUniqueId());

                int newTicketCount = Math.min(config.maxPerLotto, oldTicketCount + Math.min(count, maxTicketPurchase));
                ticketsBought[0] = newTicketCount - oldTicketCount;

                return wallet.removeFromBalance(player, config.ticketPrice * ticketsBought[0]);
            }
        ).thenApplyFailableAsynchronously(
            (Function<Boolean, TaskResult<Void, Void>>) TaskResult::fromCondition,
            (ignored) -> {
                ErrorUtil.reportUnexpectedError(player);
            }
        ).thenAcceptAsynchronously(
            (ignored) -> {
                lotteryTicketDatabase.addTickets(player.getUniqueId(), ticketsBought[0]);
                lotteryTicketDatabase.save();

                ChatUtil.sendNotice(
                    player,
                    "You purchased: ", formatTickets(ticketsBought[0]),
                    " for: ", wallet.format(ticketsBought[0] * config.ticketPrice), "."
                );
            },
            (ignored) -> {
                ChatUtil.sendError(player, "You do not have enough ", wallet.currencyNamePlural(), ".");
            }
        ).thenFinally(() -> recentList.remove(player));
    }

    private int calculateCPUTicketCount() {
        int guaranteedPerCPU = (int) (config.maxPerLotto * config.cpuPercentageGuaranteed);
        int chanceTicketsPerCPU = config.maxPerLotto - guaranteedPerCPU;

        int guaranteedTickets = guaranteedPerCPU * config.cpuPlayers;
        int chanceTickets = ChanceUtil.getRandom(chanceTicketsPerCPU * config.cpuPlayers);

        return guaranteedTickets + chanceTickets;
    }

    private void handleWinner(LotteryWinner winner) {
        Text winMessage = Text.of(ChatColor.YELLOW, winner.getName(), " has won: ",
                wallet.format(winner.getAmt()), " via the lottery!");
        Bukkit.broadcast(winMessage.build());
        chatBridge.broadcast(ChatColor.stripColor(winMessage.toString()));

        if (winner.isBot()) {
            lotteryWinnerDatabase.addCPUWin(winner.getAmt());
        } else {
            wallet.addToBalance(winner.getAsOfflinePlayer(), winner.getAmt());
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

        BigDecimal cash = getWinnerCash();

        handleWinner(new LotteryWinner(name, cash));
    }

    public void broadcastLottery(Iterable<? extends CommandSender> senders) {
        for (CommandSender receiver : senders) {
            ChatUtil.sendNotice(receiver, "The lottery currently has: ",
                formatTickets(lotteryTicketDatabase.getTicketCount()), " and is worth: ",
                wallet.format(getWinnerCash()), ".");
        }
    }

    public List<LotteryWinner> getRecentWinner() {
        return lotteryWinnerDatabase.getRecentWinner(config.recentLength);
    }

    public BigDecimal getWinnerCash() {
        double amt = lotteryTicketDatabase.getTicketCount() * config.ticketPrice * .75;
        amt += lotteryState.profitFromMarketSales;
        return new BigDecimal(amt);
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