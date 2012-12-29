package us.arrowcraft.aurora.economic.casino;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import us.arrowcraft.aurora.economic.ImpersonalComponent;
import us.arrowcraft.aurora.util.ChanceUtil;
import us.arrowcraft.aurora.util.ChatUtil;
import us.arrowcraft.aurora.util.EnvironmentUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Casino", desc = "Risk it!")
@Depend(plugins = {"Vault"}, components = {ImpersonalComponent.class})
public class CasinoComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    @InjectComponent
    ImpersonalComponent impersonalComponent;

    private double profit = 20000;
    private static Economy economy = null;
    private List<Player> recentList = new ArrayList<>();

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        setupEconomy();
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 60 * 60, 20 * 60 * 60);
    }

    @Override
    public void run() {

        profit = Math.max(profit, 20000);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {

        final Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || block == null
                || !EnvironmentUtil.isSign(block) || recentList.contains(player)
                || player.isSneaking()) return;

        Sign sign = (Sign) block.getState();
        String operator = sign.getLine(0).trim();
        double bet = setBet(sign);

        if (operator.equalsIgnoreCase(player.getName())) operator = "";

        switch (sign.getLine(1)) {
            case "[Slots]":
                if (!impersonalComponent.check(block, true)) return;
                if (checkBalance(player, bet)) operateSlots(operator, player, bet);
                break;
            case "[Roulette]":
                if (!impersonalComponent.check(block, true)) return;
                if (checkBalance(player, bet)) operateRoulette(operator, player, bet);
                break;
            case "[RussianR]":
                if (checkBalance(player, bet)) operateRussianRoulette(player);
                break;
            case "[RS Dice]":
                if (!impersonalComponent.check(block, true)) return;
                if (checkBalance(player, bet)) operateRSDice(operator, player, bet);
                break;
            default:
                break;
        }

        recentList.add(player);
        server.getScheduler().scheduleSyncDelayedTask(inst, new Runnable() {

            @Override
            public void run() {

                recentList.remove(player);
            }
        }, 10);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerBlockPlace(SignChangeEvent event) {

        Block block = event.getBlock();
        Player player = event.getPlayer();

        String header = event.getLine(1);
        boolean validHeader = false;

        if (header.equalsIgnoreCase("[Slots]")) {
            if (inst.hasPermission(player, "aurora.casino.slots")) {
                event.setLine(1, "[Slots]");
                validHeader = true;
            }
        } else if (header.equalsIgnoreCase("[Roulette]")) {
            if (inst.hasPermission(player, "aurora.casino.roulette")) {
                event.setLine(1, "[Roulette]");
                validHeader = true;
            }
        } else if (header.equalsIgnoreCase("[RussianR]")) {
            if (inst.hasPermission(player, "aurora.casino.russianr")) {
                event.setLine(1, "[RussianR]");
                validHeader = true;
            }
        } else if (header.equalsIgnoreCase("[RS Dice]")) {
            if (inst.hasPermission(player, "aurora.casino.rsdice")) {
                event.setLine(1, "[RS Dice]");
                validHeader = true;
            }
        } else {
            return;
        }

        if (!validHeader) {
            event.setCancelled(true);
            block.breakNaturally(new ItemStack(ItemID.SIGN));
        }

        try {
            Integer.parseInt(event.getLine(2).trim());
        } catch (NumberFormatException e) {
            ChatUtil.sendError(player, "The third line must be the amount of "
                    + economy.currencyNamePlural() + " to bet.");
            event.setCancelled(true);
            block.breakNaturally(new ItemStack(ItemID.SIGN));
        }
    }

    private void operateSlots(String operator, Player player, double bet) {

        double loot = bet * 20;

        if (!operatorHasMoney(operator, player, loot)) return;

        String one = ChatUtil.loonyCharacter();
        String two = ChatUtil.loonyCharacter();
        String three = ChatUtil.loonyCharacter();

        if (profit < 0) {
            while (two.equals(one)) {
                one = ChatUtil.loonyCharacter();
            }
        }

        if (bet != 1) {
            ChatUtil.sendNotice(player, "You deposit: "
                    + ChatUtil.makeCountString(economy.format(bet), " " + economy.currencyNamePlural() + "."));
        } else {
            ChatUtil.sendNotice(player, "You deposit: "
                    + ChatUtil.makeCountString(economy.format(bet), " " + economy.currencyNameSingular() + "."));
        }

        ChatUtil.sendNotice(player, one + " - " + two + " - " + three);

        if (one.equals(two) && two.equals(three)) {
            profit -= loot;
            economy.depositPlayer(player.getName(), loot);
            if (!operatorIsInf(operator)) economy.withdrawPlayer(operator, loot / 10);
            ChatUtil.sendNotice(player, ChatColor.GOLD, "Jackpot!");
            ChatUtil.sendNotice(player, "You won: " + ChatUtil.makeCountString(economy.format(loot),
                    " " + economy.currencyNamePlural() + "."));
        } else {
            profit += bet;
            economy.withdrawPlayer(player.getName(), bet);
            if (!operatorIsInf(operator)) economy.depositPlayer(operator, bet / 10);
            ChatUtil.sendNotice(player, "Better luck next time.");
        }
    }

    private void operateRoulette(String operator, Player player, double bet) {

        double loot = bet * 35;

        if (!operatorHasMoney(operator, player, loot)) return;

        if (bet != 1) {
            ChatUtil.sendNotice(player, "You deposit: "
                    + ChatUtil.makeCountString(economy.format(bet), " " + economy.currencyNamePlural() + "."));
        } else {
            ChatUtil.sendNotice(player, "You deposit: "
                    + ChatUtil.makeCountString(economy.format(bet), " " + economy.currencyNameSingular() + "."));
        }

        if (ChanceUtil.getChance(38) && profit > 0) {
            profit -= loot;
            economy.depositPlayer(player.getName(), loot);
            if (!operatorIsInf(operator)) economy.withdrawPlayer(operator, loot / 10);
            ChatUtil.sendNotice(player, "You won: " + ChatUtil.makeCountString(economy.format(loot),
                    " " + economy.currencyNamePlural() + "."));
        } else {
            profit += bet;
            economy.withdrawPlayer(player.getName(), bet);
            if (!operatorIsInf(operator)) economy.depositPlayer(operator, bet / 10);
            ChatUtil.sendNotice(player, "Better luck next time.");
        }
    }

    private void operateRussianRoulette(Player player) {

        ChatUtil.sendNotice(player, "Moron, no one wins this game it's not worth it, your lucky to be alive.");
        player.setHealth(1);
        player.playEffect(EntityEffect.HURT);
    }

    private void operateRSDice(String operator, Player player, double bet) {

        double loot = bet * 2;

        if (!operatorHasMoney(operator, player, loot)) return;

        int roll = ChanceUtil.getRandom(100);
        if (profit < 0) roll = ChanceUtil.getRandom(54);

        if (bet != 1) {
            ChatUtil.sendNotice(player, "You deposit: "
                    + ChatUtil.makeCountString(economy.format(bet), " " + economy.currencyNamePlural() + "."));
        } else {
            ChatUtil.sendNotice(player, "You deposit: "
                    + ChatUtil.makeCountString(economy.format(bet), " " + economy.currencyNameSingular() + "."));
        }

        ChatUtil.sendNotice(player, "The dice roll: " + ChatUtil.makeCountString(roll, "."));

        if (roll >= 55) {
            economy.withdrawPlayer(player.getName(), bet);
            economy.depositPlayer(operator, bet / 10);

            profit -= loot;
            economy.depositPlayer(player.getName(), loot);
            if (!operatorIsInf(operator)) economy.withdrawPlayer(operator, loot / 10);
            ChatUtil.sendNotice(player, "You won: " + ChatUtil.makeCountString(economy.format(loot),
                    " " + economy.currencyNamePlural() + "."));
        } else {
            profit += bet;
            economy.withdrawPlayer(player.getName(), bet);
            if (!operatorIsInf(operator)) economy.depositPlayer(operator, bet / 10);
            ChatUtil.sendNotice(player, "Better luck next time.");
        }
    }

    private int setBet(Sign sign) {

        try {
            return Integer.parseInt(sign.getLine(2).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean operatorIsInf(String operator) {

        return operator.equals("");
    }

    private boolean operatorHasMoney(String operator, Player better, double loot) {

        if (operatorIsInf(operator)) {
            return true;
        } else if (!economy.hasAccount(operator)) {
            ChatUtil.sendError(better, "The Operator: " + operator + " does not exist.");
            return false;
        } else if (!economy.has(operator, loot / 10)) {
            ChatUtil.sendError(better, "The Operator: " + operator + " does not have sufficient funds.");
            return false;
        } else {
            return true;
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

    private boolean checkBalance(Player player, double bet) {

        if (economy.getBalance(player.getName()) - bet <= 0) {
            ChatUtil.sendError(player, "You do not have enough " + economy.currencyNamePlural() + " to bet.");
            return false;
        }
        return true;
    }
}
