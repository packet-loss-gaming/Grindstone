/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.economic.casino;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.economic.wallet.WalletComponent;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.ErrorUtil;
import gg.packetloss.grindstone.util.task.promise.TaskResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;


@ComponentInformation(friendlyName = "Casino", desc = "Risk it!")
@Depend(plugins = {"Vault"}, components = {WalletComponent.class})
public class CasinoComponent extends BukkitComponent implements Listener {
    @InjectComponent
    private WalletComponent wallet;

    private final List<Player> recentList = new ArrayList<>();
    private LocalConfiguration config;

    @Override
    public void enable() {
        config = configure(new LocalConfiguration());
        CommandBook.registerEvents(this);
    }

    @Override
    public void reload() {
        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("slot.match-modifier")
        public double slotMatchModifier = 15;
        @Setting("slot.jackpot-modifier.min")
        public int slotJackpotModifierMin = 100;
        @Setting("slot.jackpot-modifier.max")
        public int slotJackpotModifierMax = 5000;
        @Setting("roulette.multiplier")
        public double rouletteMultiplier = 2;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || block == null
                || !EnvironmentUtil.isSign(block) || recentList.contains(player)
                || player.isSneaking()) return;

        Sign sign = (Sign) block.getState();
        double bet = setBet(sign);

        switch (sign.getLine(1)) {
            case "[Slots]":
                operateSlots(player, bet);
                break;
            case "[Roulette]":
                operateRoulette(player, bet);
                break;
            case "[RussianR]":
                operateRussianRoulette(player);
                break;
            case "[RS Dice]":
                operateRSDice(player, bet);
                break;
            default:
                break;
        }

        recentList.add(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(CommandBook.inst(), () -> recentList.remove(player), 10);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBlockPlace(SignChangeEvent event) {

        Block block = event.getBlock();
        Player player = event.getPlayer();

        String header = event.getLine(1);
        boolean validHeader = false;

        if (header.equalsIgnoreCase("[Slots]")) {
            if (player.hasPermission("aurora.casino.slots")) {
                event.setLine(1, "[Slots]");
                validHeader = true;
            }
        } else if (header.equalsIgnoreCase("[Roulette]")) {
            if (player.hasPermission("aurora.casino.roulette")) {
                event.setLine(1, "[Roulette]");
                validHeader = true;
            }
        } else if (header.equalsIgnoreCase("[RussianR]")) {
            if (player.hasPermission("aurora.casino.russianr")) {
                event.setLine(1, "[RussianR]");
                validHeader = true;
            }
        } else if (header.equalsIgnoreCase("[RS Dice]")) {
            if (player.hasPermission("aurora.casino.rsdice")) {
                event.setLine(1, "[RS Dice]");
                validHeader = true;
            }
        } else {
            return;
        }

        if (!validHeader) {
            event.setCancelled(true);
            block.breakNaturally();
        }

        try {
            Integer.parseInt(event.getLine(2).trim());
        } catch (NumberFormatException e) {
            ChatUtil.sendError(player, "The third line must be the amount of ", wallet.currencyNamePlural(), " to bet.");
            event.setCancelled(true);
            block.breakNaturally();
        }
    }

    private void handleCasinoTransaction(Player player, double bet, Supplier<Double> won) {
        double[] loot = {0};

        wallet.removeFromBalance(player, bet).thenApplyFailableAsynchronously(
            (Function<Boolean, TaskResult<Void, Void>>) TaskResult::fromCondition,
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        ).thenComposeFailableAsynchronously(
            (ignored) -> {
                // Bet successfully removed from the balance, run the mechanic logic to see if we have a win
                ChatUtil.sendNotice(player, "You deposit: ", wallet.format(bet), ".");
                loot[0] = won.get();
                if (loot[0] > 0) {
                    // We have a win, award loot
                    return wallet.addToBalance(player, loot[0]);
                } else {
                    // A loss, no balance adjustment needed
                    return TaskResult.<BigDecimal>success().asTaskFuture();
                }
            },
            (ignored) -> {
                ChatUtil.sendError(player, "You do not have enough ", wallet.currencyNamePlural(), " to make this bet.");
            }
        ).thenAcceptAsynchronously(
            (newBalance) -> {
                if (loot[0] > 0) {
                    ChatUtil.sendNotice(player, "You won: ", wallet.format(loot[0]), ".");
                } else {
                    ChatUtil.sendNotice(player, "Better luck next time.");
                }
            },
            (ignored) -> { ErrorUtil.reportUnexpectedError(player); }
        );
    }

    private void operateSlots(Player player, double bet) {
        handleCasinoTransaction(player, bet, () -> {
            char jackPotChar = ChatUtil.loonyCharacter();

            boolean boolOne = ChanceUtil.getChance(64);
            boolean boolTwo = ChanceUtil.getChance(64);
            boolean boolThree = ChanceUtil.getChance(64);

            char one = boolOne ? jackPotChar : ChatUtil.loonyCharacter();
            char two = boolTwo ? jackPotChar : ChatUtil.loonyCharacter();
            char three = boolThree ? jackPotChar : ChatUtil.loonyCharacter();

            boolean solved = (boolOne && boolTwo && boolThree) || (one != jackPotChar || two != jackPotChar || three != jackPotChar);
            while (!solved) {
                if (!boolOne) {
                    one = ChatUtil.loonyCharacter();
                }
                if (!boolTwo) {
                    two = ChatUtil.loonyCharacter();
                }
                if (!boolThree) {
                    three = ChatUtil.loonyCharacter();
                }

                solved = (one != jackPotChar || two != jackPotChar || three != jackPotChar);
            }

            int jackpotModifier = ChanceUtil.getRangedRandom(
                config.slotJackpotModifierMin, config.slotJackpotModifierMax
            );

            String jackpotModifierString = "x" + ChatUtil.WHOLE_NUMBER_FORMATTER.format(jackpotModifier);

            ChatUtil.sendNotice(player, "Jackpot " + jackpotModifierString + " on: " + jackPotChar);
            ChatUtil.sendNotice(player, one + " - " + two + " - " + three);

            if (one == two && two == three) {
                double loot = bet * config.slotMatchModifier;
                if (one == jackPotChar) {
                    loot *= jackpotModifier;
                    ChatUtil.sendNotice(player, ChatColor.GOLD, "Jackpot! " + jackpotModifierString + "!");
                }

                return loot;
            }

            return 0d;
        });
    }

    private void operateRoulette(Player player, double bet) {
        handleCasinoTransaction(player, bet, () -> {
            if (ChanceUtil.getChance(18, 37)) {
                return bet * config.rouletteMultiplier;
            }

            return 0d;
        });
    }

    private void operateRussianRoulette(Player player) {

        ChatUtil.sendNotice(player, "Moron, no one wins this game it's not worth it, your lucky to be alive.");
        player.setHealth(1);
        player.playEffect(EntityEffect.HURT);
    }

    private void operateRSDice(Player player, double bet) {
        handleCasinoTransaction(player, bet, () -> {
            int roll = ChanceUtil.getRandom(100);

            ChatUtil.sendNotice(player, "The dice roll: " + ChatUtil.makeCountString(roll, "."));
            if (roll >= 55) {
                return bet * 2;
            }

            return 0d;
        });
    }

    private int setBet(Sign sign) {
        try {
            return Integer.parseInt(sign.getLine(2).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
